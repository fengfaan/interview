# Prompt Style Profiles Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add per-direction×per-level prompt style profiles (structured JSON fields) that inject style instructions into existing interview prompt templates.

**Architecture:** JSON files in `prompts/styles/{direction}/{level}.json` store structured fields. A new `StyleService` reads them, builds a style instruction string, and injects it via `{{styleInstruction}}` into existing templates. New REST endpoints under `/api/settings/styles/` and a new Settings UI card manage the profiles.

**Tech Stack:** Spring Boot 3 / Jackson / Vue 3 + Pinia / Tailwind CSS

---

## File Structure

| File | Action | Responsibility |
|------|--------|----------------|
| `backend/src/main/java/com/interviewassistant/dto/settings/StyleProfileRequest.java` | Create | DTO for saving a style profile |
| `backend/src/main/java/com/interviewassistant/dto/settings/StyleProfileResponse.java` | Create | DTO for returning a style profile |
| `backend/src/main/java/com/interviewassistant/dto/settings/StyleProfileSummary.java` | Create | DTO for list endpoint (direction + level + hasCustomization) |
| `backend/src/main/java/com/interviewassistant/ai/style/StyleService.java` | Create | Read/write JSON style files, build instruction string |
| `backend/src/main/java/com/interviewassistant/ai/style/StyleProfile.java` | Create | Model for style profile fields |
| `backend/src/main/java/com/interviewassistant/controller/SettingsController.java` | Modify | Add 3 style endpoints |
| `backend/src/main/java/com/interviewassistant/service/InterviewAiService.java` | Modify | Add `styleInstruction` to all render() calls |
| `backend/prompts/styles/GO_BACKEND/BASIC.json` | Create | Default style for Go后端-基础八股 |
| `backend/prompts/styles/GO_BACKEND/DEEP_PRINCIPLE.json` | Create | Default style for Go后端-深度原理 |
| `backend/prompts/styles/GO_BACKEND/PROJECT_PRACTICE.json` | Create | Default style for Go后端-项目实战 |
| `backend/prompts/styles/REACT_FRONTEND/BASIC.json` | Create | Default style for React前端-基础八股 |
| `backend/prompts/styles/REACT_FRONTEND/DEEP_PRINCIPLE.json` | Create | Default style for React前端-深度原理 |
| `backend/prompts/styles/REACT_FRONTEND/PROJECT_PRACTICE.json` | Create | Default style for React前端-项目实战 |
| `backend/prompts/styles/SYSTEM_DESIGN/BASIC.json` | Create | Default style for 系统设计-基础八股 |
| `backend/prompts/styles/SYSTEM_DESIGN/DEEP_PRINCIPLE.json` | Create | Default style for 系统设计-深度原理 |
| `backend/prompts/styles/SYSTEM_DESIGN/PROJECT_PRACTICE.json` | Create | Default style for 系统设计-项目实战 |
| `backend/prompts/styles/DATABASE_RELATED/BASIC.json` | Create | Default style for 数据库相关-基础八股 |
| `backend/prompts/styles/DATABASE_RELATED/DEEP_PRINCIPLE.json` | Create | Default style for 数据库相关-深度原理 |
| `backend/prompts/styles/DATABASE_RELATED/PROJECT_PRACTICE.json` | Create | Default style for 数据库相关-项目实战 |
| `backend/prompts/styles/AI_CODING/BASIC.json` | Create | Default style for AI Agent-基础八股 |
| `backend/prompts/styles/AI_CODING/DEEP_PRINCIPLE.json` | Create | Default style for AI Agent-深度原理 |
| `backend/prompts/styles/AI_CODING/PROJECT_PRACTICE.json` | Create | Default style for AI Agent-项目实战 |
| `backend/prompts/interview/question.md` | Modify | Add `{{styleInstruction}}` |
| `backend/prompts/interview/batch-question.md` | Modify | Add `{{styleInstruction}}` |
| `backend/prompts/interview/feedback-stream.md` | Modify | Add `{{styleInstruction}}` |
| `backend/prompts/interview/feedback-json.md` | Modify | Add `{{styleInstruction}}` |
| `backend/prompts/interview/recommended-answer.md` | Modify | Add `{{styleInstruction}}` |
| `backend/prompts/interview/deep-dive.md` | Modify | Add `{{styleInstruction}}` |
| `frontend/src/types/settings.ts` | Modify | Add StyleProfile types |
| `frontend/src/api/settingsApi.ts` | Modify | Add 3 style API functions |
| `frontend/src/stores/settingsStore.ts` | Modify | Add style profile state and actions |
| `frontend/src/views/SettingsView.vue` | Modify | Add style profile card |

---

### Task 1: Create StyleProfile model and StyleService

**Files:**
- Create: `backend/src/main/java/com/interviewassistant/ai/style/StyleProfile.java`
- Create: `backend/src/main/java/com/interviewassistant/ai/style/StyleService.java`

- [ ] **Step 1: Create StyleProfile model**

```java
package com.interviewassistant.ai.style;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StyleProfile {
    private String focusAreas;
    private String scenarioPreference;
    private String keywordStyle;
}
```

- [ ] **Step 2: Create StyleService**

This service reads/writes JSON style files and builds the style instruction string.

```java
package com.interviewassistant.ai.style;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewassistant.dto.interview.InterviewDirection;
import com.interviewassistant.dto.interview.InterviewLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class StyleService {

    private final Path stylesDirectory;
    private final ObjectMapper objectMapper;

    public StyleService(@Value("${app.prompts.directory:prompts}") String promptDirectory,
                        ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        Path configuredDirectory = Path.of(promptDirectory).toAbsolutePath().normalize();
        Path repoRootDirectory = Path.of("backend", "prompts").toAbsolutePath().normalize();
        Path baseDir;
        if ("prompts".equals(promptDirectory) && !Files.isDirectory(configuredDirectory)
                && Files.isDirectory(repoRootDirectory)) {
            baseDir = repoRootDirectory;
        } else {
            baseDir = configuredDirectory;
        }
        this.stylesDirectory = baseDir.resolve("styles");
    }

    public StyleProfile loadProfile(InterviewDirection direction, InterviewLevel level) {
        Path file = resolveProfilePath(direction, level);
        if (!Files.exists(file)) {
            return new StyleProfile("", "", "");
        }
        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            return objectMapper.readValue(json, StyleProfile.class);
        } catch (IOException e) {
            log.warn("Failed to read style profile {}/{}: {}", direction, level, e.getMessage());
            return new StyleProfile("", "", "");
        }
    }

    public void saveProfile(InterviewDirection direction, InterviewLevel level, StyleProfile profile) {
        Path file = resolveProfilePath(direction, level);
        try {
            Files.createDirectories(file.getParent());
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(profile);
            Files.writeString(file, json, StandardCharsets.UTF_8);
            log.info("Style profile saved: {}/{}", direction, level);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save style profile: " + e.getMessage(), e);
        }
    }

    public String buildStyleInstruction(InterviewDirection direction, InterviewLevel level) {
        StyleProfile profile = loadProfile(direction, level);
        StringBuilder sb = new StringBuilder();
        if (isNotBlank(profile.getFocusAreas())) {
            sb.append("出题侧重：").append(profile.getFocusAreas().trim()).append("。");
        }
        if (isNotBlank(profile.getScenarioPreference())) {
            sb.append("场景偏好：").append(profile.getScenarioPreference().trim()).append("。");
        }
        if (isNotBlank(profile.getKeywordStyle())) {
            sb.append("关键词风格：").append(profile.getKeywordStyle().trim()).append("。");
        }
        return sb.toString();
    }

    public List<String> listDirections() {
        List<String> directions = new ArrayList<>();
        if (!Files.isDirectory(stylesDirectory)) {
            return directions;
        }
        try (var stream = Files.list(stylesDirectory)) {
            stream.filter(Files::isDirectory)
                  .map(p -> p.getFileName().toString())
                  .forEach(directions::add);
        } catch (IOException e) {
            log.warn("Failed to list style directories: {}", e.getMessage());
        }
        return directions;
    }

    private Path resolveProfilePath(InterviewDirection direction, InterviewLevel level) {
        return stylesDirectory.resolve(direction.name()).resolve(level.name() + ".json");
    }

    private boolean isNotBlank(String s) {
        return s != null && !s.isBlank();
    }
}
```

- [ ] **Step 3: Verify compilation**

Run: `cd /Users/fengfan/interviewAssistant/backend && ./mvnw compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/interviewassistant/ai/style/StyleProfile.java \
        backend/src/main/java/com/interviewassistant/ai/style/StyleService.java
git commit -m "feat(style): add StyleProfile model and StyleService for prompt style management"
```

---

### Task 2: Create style DTOs

**Files:**
- Create: `backend/src/main/java/com/interviewassistant/dto/settings/StyleProfileRequest.java`
- Create: `backend/src/main/java/com/interviewassistant/dto/settings/StyleProfileResponse.java`
- Create: `backend/src/main/java/com/interviewassistant/dto/settings/StyleProfileSummary.java`

- [ ] **Step 1: Create StyleProfileRequest**

```java
package com.interviewassistant.dto.settings;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StyleProfileRequest {
    @Size(max = 2000, message = "出题侧重不能超过2000字")
    private String focusAreas;

    @Size(max = 2000, message = "场景偏好不能超过2000字")
    private String scenarioPreference;

    @Size(max = 1000, message = "关键词风格不能超过1000字")
    private String keywordStyle;
}
```

- [ ] **Step 2: Create StyleProfileResponse**

```java
package com.interviewassistant.dto.settings;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StyleProfileResponse {
    private String direction;
    private String level;
    private String focusAreas;
    private String scenarioPreference;
    private String keywordStyle;
}
```

- [ ] **Step 3: Create StyleProfileSummary**

```java
package com.interviewassistant.dto.settings;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StyleProfileSummary {
    private String direction;
    private String level;
    private boolean hasCustomization;
}
```

- [ ] **Step 4: Verify compilation**

Run: `cd /Users/fengfan/interviewAssistant/backend && ./mvnw compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/interviewassistant/dto/settings/StyleProfileRequest.java \
        backend/src/main/java/com/interviewassistant/dto/settings/StyleProfileResponse.java \
        backend/src/main/java/com/interviewassistant/dto/settings/StyleProfileSummary.java
git commit -m "feat(style): add DTOs for style profile API"
```

---

### Task 3: Add style endpoints to SettingsController

**Files:**
- Modify: `backend/src/main/java/com/interviewassistant/controller/SettingsController.java`

- [ ] **Step 1: Add imports and field**

Add these imports after existing imports:

```java
import com.interviewassistant.ai.style.StyleService;
import com.interviewassistant.ai.style.StyleProfile;
import com.interviewassistant.dto.interview.InterviewDirection;
import com.interviewassistant.dto.interview.InterviewLevel;
import com.interviewassistant.dto.settings.StyleProfileRequest;
import com.interviewassistant.dto.settings.StyleProfileResponse;
import com.interviewassistant.dto.settings.StyleProfileSummary;
```

Add field to the class (after `circuitBreaker` field):

```java
private final StyleService styleService;
```

- [ ] **Step 2: Add GET /api/settings/styles endpoint**

Add this method to the controller:

```java
@GetMapping("/styles")
public ApiResponse<List<StyleProfileSummary>> listStyles() {
    List<StyleProfileSummary> summaries = new java.util.ArrayList<>();
    for (InterviewDirection dir : InterviewDirection.values()) {
        for (InterviewLevel lvl : InterviewLevel.values()) {
            StyleProfile profile = styleService.loadProfile(dir, lvl);
            boolean hasCustomization = (profile.getFocusAreas() != null && !profile.getFocusAreas().isBlank())
                    || (profile.getScenarioPreference() != null && !profile.getScenarioPreference().isBlank())
                    || (profile.getKeywordStyle() != null && !profile.getKeywordStyle().isBlank());
            summaries.add(new StyleProfileSummary(dir.name(), lvl.name(), hasCustomization));
        }
    }
    return ApiResponse.ok(summaries);
}
```

- [ ] **Step 3: Add GET /api/settings/styles/{direction}/{level} endpoint**

```java
@GetMapping("/styles/{direction}/{level}")
public ApiResponse<StyleProfileResponse> getStyleProfile(
        @PathVariable String direction,
        @PathVariable String level) {
    InterviewDirection dir = InterviewDirection.valueOf(direction);
    InterviewLevel lvl = InterviewLevel.valueOf(level);
    StyleProfile profile = styleService.loadProfile(dir, lvl);
    return ApiResponse.ok(new StyleProfileResponse(
            dir.name(), lvl.name(),
            profile.getFocusAreas() != null ? profile.getFocusAreas() : "",
            profile.getScenarioPreference() != null ? profile.getScenarioPreference() : "",
            profile.getKeywordStyle() != null ? profile.getKeywordStyle() : ""
    ));
}
```

- [ ] **Step 4: Add PUT /api/settings/styles/{direction}/{level} endpoint**

```java
@PutMapping("/styles/{direction}/{level}")
public ApiResponse<StyleProfileResponse> saveStyleProfile(
        @PathVariable String direction,
        @PathVariable String level,
        @Valid @RequestBody StyleProfileRequest request) {
    InterviewDirection dir = InterviewDirection.valueOf(direction);
    InterviewLevel lvl = InterviewLevel.valueOf(level);
    StyleProfile profile = new StyleProfile(
            request.getFocusAreas() != null ? request.getFocusAreas() : "",
            request.getScenarioPreference() != null ? request.getScenarioPreference() : "",
            request.getKeywordStyle() != null ? request.getKeywordStyle() : ""
    );
    styleService.saveProfile(dir, lvl, profile);
    return ApiResponse.ok(new StyleProfileResponse(dir.name(), lvl.name(),
            profile.getFocusAreas(), profile.getScenarioPreference(), profile.getKeywordStyle()));
}
```

- [ ] **Step 5: Verify compilation**

Run: `cd /Users/fengfan/interviewAssistant/backend && ./mvnw compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/interviewassistant/controller/SettingsController.java
git commit -m "feat(style): add style profile endpoints to SettingsController"
```

---

### Task 4: Create default style JSON files

**Files:**
- Create: 15 JSON files under `backend/prompts/styles/`

- [ ] **Step 1: Create directory structure and all default files**

Create all 15 default JSON files. Each direction gets appropriate defaults for its domain.

**GO_BACKEND/BASIC.json:**
```json
{
  "focusAreas": "Go 语言基础语法、内置数据结构（slice/map/channel）、错误处理模式、接口与类型系统",
  "scenarioPreference": "日常开发中常见的代码实现场景，如数据转换、并发控制、错误链处理",
  "keywordStyle": "偏向 Go 语言特有术语，如 goroutine、channel、defer、interface 满足、零值"
}
```

**GO_BACKEND/DEEP_PRINCIPLE.json:**
```json
{
  "focusAreas": "Go runtime 原理（GMP 调度模型、三色标记 GC、内存分配器）、并发原语底层实现、编译器优化",
  "scenarioPreference": "高性能服务端场景，如百万连接网关、低延迟交易系统、大规模数据处理",
  "keywordStyle": "偏向底层原理术语，如 GMP 模型、写屏障、span/mspan、STW、抢占式调度"
}
```

**GO_BACKEND/PROJECT_PRACTICE.json:**
```json
{
  "focusAreas": "Go 微服务架构设计、gRPC/Protobuf 实践、中间件集成（Kafka/Redis/etcd）、可观测性（OpenTelemetry）",
  "scenarioPreference": "真实业务场景：订单系统、支付网关、实时推送服务、分布式任务调度",
  "keywordStyle": "偏向工程实践术语，如服务拆分策略、优雅关停、连接池、熔断降级"
}
```

**REACT_FRONTEND/BASIC.json:**
```json
{
  "focusAreas": "React 核心概念（JSX/组件/Props/State）、Hooks 使用规则、事件处理、条件渲染与列表渲染",
  "scenarioPreference": "常见 UI 开发场景：表单管理、列表筛选、弹窗交互、路由导航",
  "keywordStyle": "偏向 React 基础术语，如受控组件、单向数据流、re-render、key 稳定性"
}
```

**REACT_FRONTEND/DEEP_PRINCIPLE.json:**
```json
{
  "focusAreas": "React Fiber 架构、调度器与优先级队列、Lane 模型、Hooks 底层链表实现、并发模式（Suspense/Transition）",
  "scenarioPreference": "大型前端应用性能优化场景：虚拟滚动、状态管理选型、SSR/SSG 混合渲染",
  "keywordStyle": "偏向底层原理术语，如 Fiber 节点、Lane 优先级、时间切片、bailout 优化、hydrating"
}
```

**REACT_FRONTEND/PROJECT_PRACTICE.json:**
```json
{
  "focusAreas": "React 工程化实践（Monorepo/微前端）、状态管理方案（Zustand/Jotai/Redux Toolkit）、组件库设计、CI/CD 流水线",
  "scenarioPreference": "企业级前端场景：后台管理系统、数据可视化大屏、低代码平台、跨端应用",
  "keywordStyle": "偏向工程实践术语，如模块联邦、Design System、Tree Shaking、Bundle Analysis"
}
```

**SYSTEM_DESIGN/BASIC.json:**
```json
{
  "focusAreas": "分布式系统基础概念（CAP/BASE/一致性模型）、常见架构模式（缓存/消息队列/负载均衡）、基础容量估算",
  "scenarioPreference": "经典系统设计题：URL 短链、限流器、键值存储、消息队列",
  "keywordStyle": "偏向系统设计术语，如读写分离、分片策略、最终一致性、幂等性"
}
```

**SYSTEM_DESIGN/DEEP_PRINCIPLE.json:**
```json
{
  "focusAreas": "分布式共识算法（Raft/Paxos）、分布式事务（2PC/TCC/Saga）、数据分区与复制策略、多活架构",
  "scenarioPreference": "大规模系统设计：全球分布式数据库、实时推荐系统、搜索引擎、流式计算平台",
  "keywordStyle": "偏向深度架构术语，如向量时钟、Gossip 协议、LSM Tree、Bloom Filter、一致性哈希"
}
```

**SYSTEM_DESIGN/PROJECT_PRACTICE.json:**
```json
{
  "focusAreas": "系统设计面试实战：需求澄清、容量估算、高层设计、详细设计、瓶颈分析",
  "scenarioPreference": "真实业务系统：社交平台、电商系统、视频流媒体、在线协作工具",
  "keywordStyle": "偏向面试表达术语，如 QPS 估算、数据量级、单点故障、水平扩展、降级策略"
}
```

**DATABASE_RELATED/BASIC.json:**
```json
{
  "focusAreas": "SQL 基础（JOIN/子查询/窗口函数）、索引原理（B+Tree/Hash）、事务 ACID、隔离级别",
  "scenarioPreference": "常见数据库操作场景：多表关联查询、分页优化、数据去重、慢查询排查",
  "keywordStyle": "偏向数据库基础术语，如聚簇索引、覆盖索引、脏读、幻读、MVCC"
}
```

**DATABASE_RELATED/DEEP_PRINCIPLE.json:**
```json
{
  "focusAreas": "存储引擎原理（InnoDB/MyISAM/ RocksDB）、锁机制（行锁/间隙锁/意向锁）、WAL 与崩溃恢复、查询优化器",
  "scenarioPreference": "高并发数据库场景：热点数据更新、大表 DDL、分库分表、读写分离延迟",
  "keywordStyle": "偏向底层原理术语，如 redo log、undo log、next-key lock、change buffer、自适应哈希"
}
```

**DATABASE_RELATED/PROJECT_PRACTICE.json:**
```json
{
  "focusAreas": "数据库架构设计（分库分表/读写分离/数据迁移）、性能调优（索引优化/SQL 调优/参数调优）、多数据源管理",
  "scenarioPreference": "真实业务场景：订单库分片、数据归档、异构数据同步、数据库中间件选型",
  "keywordStyle": "偏向工程实践术语，如 ShardingSphere、binlog 同步、数据倾斜、热点分片、路由策略"
}
```

**AI_CODING/BASIC.json:**
```json
{
  "focusAreas": "LLM 基础概念（Prompt Engineering/Token/上下文窗口）、API 调用模式（补全/对话/嵌入）、基础工具调用",
  "scenarioPreference": "常见 AI 应用场景：问答系统、文本摘要、内容生成、简单 Agent",
  "keywordStyle": "偏向 AI 应用术语，如 System Prompt、Temperature、Few-shot、RAG"
}
```

**AI_CODING/DEEP_PRINCIPLE.json:**
```json
{
  "focusAreas": "Transformer 架构原理、注意力机制、位置编码、训练与推理优化（量化/蒸馏/RLHF）、Embedding 空间",
  "scenarioPreference": "复杂 AI 系统场景：多模态模型、长上下文处理、模型微调与评估、向量检索引擎",
  "keywordStyle": "偏向 AI 底层术语，如 KV Cache、Flash Attention、LoRA、SFT、CoT 推理"
}
```

**AI_CODING/PROJECT_PRACTICE.json:**
```json
{
  "focusAreas": "AI Agent 工程化（工具调用/ReAct/Memory）、RAG 架构设计（向量库/Chunk/检索策略）、Prompt 链与工作流编排、AI 应用可观测性",
  "scenarioPreference": "真实 AI 产品场景：智能客服、代码助手、知识库问答、文档审核系统",
  "keywordStyle": "偏向工程实践术语，如 Function Calling、Embedding 模型、Chunk 策略、Hallucination 检测"
}
```

- [ ] **Step 2: Verify files exist**

Run: `find /Users/fengfan/interviewAssistant/backend/prompts/styles -name "*.json" | wc -l`
Expected: 15

- [ ] **Step 3: Commit**

```bash
git add backend/prompts/styles/
git commit -m "feat(style): add default style profiles for all 15 direction×level combinations"
```

---

### Task 5: Inject styleInstruction into interview prompt templates

**Files:**
- Modify: `backend/prompts/interview/question.md`
- Modify: `backend/prompts/interview/batch-question.md`
- Modify: `backend/prompts/interview/feedback-stream.md`
- Modify: `backend/prompts/interview/feedback-json.md`
- Modify: `backend/prompts/interview/recommended-answer.md`
- Modify: `backend/prompts/interview/deep-dive.md`

- [ ] **Step 1: Add `{{styleInstruction}}` to question.md**

Add after the line `已有对话历史：{{history}}` (line 5):

```
风格指引：{{styleInstruction}}
```

The top section becomes:
```
技术方向：{{direction}}
题型：{{questionType}}
已有对话历史：{{history}}
风格指引：{{styleInstruction}}
```

- [ ] **Step 2: Add `{{styleInstruction}}` to batch-question.md**

Add after the `## 题型` block (after line 7):

```
## 风格指引
{{styleInstruction}}
```

- [ ] **Step 3: Add `{{styleInstruction}}` to feedback-stream.md**

Add after the line `期望关键词：{{expectedKeywords}}` (after line 7):

```
风格指引：{{styleInstruction}}
```

- [ ] **Step 4: Add `{{styleInstruction}}` to feedback-json.md**

Add after the line containing `{{expectedKeywords}}` (check file for exact line):

```
风格指引：{{styleInstruction}}
```

- [ ] **Step 5: Add `{{styleInstruction}}` to recommended-answer.md**

Add after the line `考察要点：{{expectedKeywords}}` (after line 6):

```
风格指引：{{styleInstruction}}
```

- [ ] **Step 6: Add `{{styleInstruction}}` to deep-dive.md**

Add after the line `考察要点：{{expectedKeywords}}` (after line 4):

```
风格指引：{{styleInstruction}}
```

- [ ] **Step 7: Commit**

```bash
git add backend/prompts/interview/question.md \
        backend/prompts/interview/batch-question.md \
        backend/prompts/interview/feedback-stream.md \
        backend/prompts/interview/feedback-json.md \
        backend/prompts/interview/recommended-answer.md \
        backend/prompts/interview/deep-dive.md
git commit -m "feat(style): add {{styleInstruction}} placeholder to all interview prompt templates"
```

---

### Task 6: Wire StyleService into InterviewAiService

**Files:**
- Modify: `backend/src/main/java/com/interviewassistant/service/InterviewAiService.java`

- [ ] **Step 1: Add StyleService dependency**

Add import after existing imports:

```java
import com.interviewassistant.ai.style.StyleService;
```

Add field (the class uses `@RequiredArgsConstructor`, so just add the field):

```java
private final StyleService styleService;
```

- [ ] **Step 2: Update generateQuestion to include styleInstruction**

In `generateQuestion()` (around line 47), change the `Map.of(...)` to `Map.ofEntries(...)` and add `styleInstruction`:

Change from:
```java
String userMessage = promptService.render("interview/question.md", Map.of(
        "direction", InterviewLabels.directionLabel(direction),
        "questionType", InterviewLabels.questionTypeLabel(level),
        "history", historySummary
));
```

To:
```java
String userMessage = promptService.render("interview/question.md", Map.ofEntries(
        Map.entry("direction", InterviewLabels.directionLabel(direction)),
        Map.entry("questionType", InterviewLabels.questionTypeLabel(level)),
        Map.entry("history", historySummary),
        Map.entry("styleInstruction", styleService.buildStyleInstruction(direction, level))
));
```

Note: `Map.of(...)` has a 10-entry limit. Since we're adding a 4th entry, `Map.of` still works (max 10). But using `Map.ofEntries` is safer for future extensibility.

- [ ] **Step 3: Update analyzeFeedback to include styleInstruction**

In `analyzeFeedback()` (around line 69), change:

```java
String userMessage = promptService.render("interview/feedback-json.md", Map.of(
        "direction", InterviewLabels.directionLabel(direction),
        "questionType", InterviewLabels.questionTypeLabel(level),
        "question", question,
        "answer", answer,
        "expectedKeywords", expectedKeywords != null ? expectedKeywords : List.of()
));
```

To:

```java
String userMessage = promptService.render("interview/feedback-json.md", Map.ofEntries(
        Map.entry("direction", InterviewLabels.directionLabel(direction)),
        Map.entry("questionType", InterviewLabels.questionTypeLabel(level)),
        Map.entry("question", question),
        Map.entry("answer", answer),
        Map.entry("expectedKeywords", expectedKeywords != null ? expectedKeywords : List.of()),
        Map.entry("styleInstruction", styleService.buildStyleInstruction(direction, level))
));
```

- [ ] **Step 4: Update buildFeedbackStreamPrompt to include styleInstruction**

In `buildFeedbackStreamPrompt()` (around line 84), change:

```java
return promptService.render("interview/feedback-stream.md", Map.of(
        "direction", InterviewLabels.directionLabel(direction),
        "questionType", InterviewLabels.questionTypeLabel(level),
        "question", question,
        "answer", answer,
        "expectedKeywords", expectedKeywords != null ? expectedKeywords : List.of(),
        "followUpQuestion", followUpQuestion != null ? followUpQuestion : "（请基于用户回答生成一个相关的进阶问题）"
));
```

To:

```java
return promptService.render("interview/feedback-stream.md", Map.ofEntries(
        Map.entry("direction", InterviewLabels.directionLabel(direction)),
        Map.entry("questionType", InterviewLabels.questionTypeLabel(level)),
        Map.entry("question", question),
        Map.entry("answer", answer),
        Map.entry("expectedKeywords", expectedKeywords != null ? expectedKeywords : List.of()),
        Map.entry("followUpQuestion", followUpQuestion != null ? followUpQuestion : "（请基于用户回答生成一个相关的进阶问题）"),
        Map.entry("styleInstruction", styleService.buildStyleInstruction(direction, level))
));
```

- [ ] **Step 5: Update buildRecommendedAnswerPrompt to include styleInstruction**

In `buildRecommendedAnswerPrompt()` (around line 96), change:

```java
return promptService.render("interview/recommended-answer.md", Map.of(
        "direction", InterviewLabels.directionLabel(direction),
        "questionType", InterviewLabels.questionTypeLabel(level),
        "question", question,
        "expectedKeywords", expectedKeywords != null ? expectedKeywords : List.of()
));
```

To:

```java
return promptService.render("interview/recommended-answer.md", Map.ofEntries(
        Map.entry("direction", InterviewLabels.directionLabel(direction)),
        Map.entry("questionType", InterviewLabels.questionTypeLabel(level)),
        Map.entry("question", question),
        Map.entry("expectedKeywords", expectedKeywords != null ? expectedKeywords : List.of()),
        Map.entry("styleInstruction", styleService.buildStyleInstruction(direction, level))
));
```

- [ ] **Step 6: Update buildDeepDivePrompt to include styleInstruction**

In `buildDeepDivePrompt()` (around line 113), change:

```java
return promptService.render("interview/deep-dive.md", Map.of(
        "question", question,
        "expectedKeywords", keywords,
        "contextType", contextType == DeepDiveContextType.RECOMMENDED_ANSWER ? "推荐答案" : "反馈点评",
        "contextContent", compactDeepDiveContext(contextContent, keywords, latestUserQuestion(compactMessages)),
        "history", history
));
```

To:

```java
return promptService.render("interview/deep-dive.md", Map.ofEntries(
        Map.entry("question", question),
        Map.entry("expectedKeywords", keywords),
        Map.entry("contextType", contextType == DeepDiveContextType.RECOMMENDED_ANSWER ? "推荐答案" : "反馈点评"),
        Map.entry("contextContent", compactDeepDiveContext(contextContent, keywords, latestUserQuestion(compactMessages))),
        Map.entry("history", history),
        Map.entry("styleInstruction", "")
));
```

Note: deep-dive doesn't use direction/level in its current signature, so we pass empty string. The deep-dive prompt already has the question context, so style instruction is less relevant here. Passing empty string ensures the `{{styleInstruction}}` placeholder doesn't appear literally.

- [ ] **Step 7: Update batch-question render calls to include styleInstruction**

In `generateBatchQuestions()` (around line 367), change:

```java
String userMessage = promptService.render("interview/batch-question.md", Map.of(
        "direction", InterviewLabels.directionLabel(direction),
        "questionType", InterviewLabels.questionTypeLabel(level),
        "count", String.valueOf(batchCount),
        "batchNumber", String.valueOf(batch + 1),
        "startIndex", String.valueOf(allQuestions.size() + 1)
));
```

To:

```java
String userMessage = promptService.render("interview/batch-question.md", Map.ofEntries(
        Map.entry("direction", InterviewLabels.directionLabel(direction)),
        Map.entry("questionType", InterviewLabels.questionTypeLabel(level)),
        Map.entry("count", String.valueOf(batchCount)),
        Map.entry("batchNumber", String.valueOf(batch + 1)),
        Map.entry("startIndex", String.valueOf(allQuestions.size() + 1)),
        Map.entry("styleInstruction", styleService.buildStyleInstruction(direction, level))
));
```

In `generateBatchQuestionChunk()` (around line 402), make the same change:

```java
String userMessage = promptService.render("interview/batch-question.md", Map.ofEntries(
        Map.entry("direction", InterviewLabels.directionLabel(direction)),
        Map.entry("questionType", InterviewLabels.questionTypeLabel(level)),
        Map.entry("count", String.valueOf(count)),
        Map.entry("batchNumber", String.valueOf(batchNumber)),
        Map.entry("startIndex", String.valueOf(startIndex)),
        Map.entry("styleInstruction", styleService.buildStyleInstruction(direction, level))
));
```

- [ ] **Step 8: Verify compilation**

Run: `cd /Users/fengfan/interviewAssistant/backend && ./mvnw compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 9: Run all backend tests**

Run: `cd /Users/fengfan/interviewAssistant/backend && ./mvnw test -q`
Expected: All tests pass

- [ ] **Step 10: Commit**

```bash
git add backend/src/main/java/com/interviewassistant/service/InterviewAiService.java
git commit -m "feat(style): inject styleInstruction into all interview prompt render calls"
```

---

### Task 7: Add frontend types and API functions for style profiles

**Files:**
- Modify: `frontend/src/types/settings.ts`
- Modify: `frontend/src/api/settingsApi.ts`

- [ ] **Step 1: Add TypeScript types to settings.ts**

Append to the end of the file:

```typescript
export interface StyleProfileSummary {
  direction: string
  level: string
  hasCustomization: boolean
}

export interface StyleProfile {
  direction: string
  level: string
  focusAreas: string
  scenarioPreference: string
  keywordStyle: string
}

export interface StyleProfileSaveRequest {
  focusAreas: string
  scenarioPreference: string
  keywordStyle: string
}
```

- [ ] **Step 2: Add API functions to settingsApi.ts**

Add imports for the new types (add to the existing import block):

```typescript
import type {
  // ... existing imports ...
  StyleProfileSummary,
  StyleProfile,
  StyleProfileSaveRequest,
} from '../types/settings'
```

Add these functions at the end of the file:

```typescript
export async function listStyleProfiles(): Promise<StyleProfileSummary[]> {
  const res = await fetch(`${API_BASE}/styles`)
  const json = await res.json()
  if (!json.success) throw new Error(json.message || '获取风格配置失败')
  return json.data
}

export async function getStyleProfile(direction: string, level: string): Promise<StyleProfile> {
  const res = await fetch(`${API_BASE}/styles/${encodeURIComponent(direction)}/${encodeURIComponent(level)}`)
  const json = await res.json()
  if (!json.success) throw new Error(json.message || '获取风格配置失败')
  return json.data
}

export async function saveStyleProfile(direction: string, level: string, request: StyleProfileSaveRequest): Promise<StyleProfile> {
  const res = await fetch(`${API_BASE}/styles/${encodeURIComponent(direction)}/${encodeURIComponent(level)}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })
  const json = await res.json()
  if (!json.success) throw new Error(json.message || '保存风格配置失败')
  return json.data
}
```

- [ ] **Step 3: Verify frontend builds**

Run: `cd /Users/fengfan/interviewAssistant/frontend && npm run build`
Expected: Build succeeds

- [ ] **Step 4: Commit**

```bash
git add frontend/src/types/settings.ts frontend/src/api/settingsApi.ts
git commit -m "feat(style): add frontend types and API functions for style profiles"
```

---

### Task 8: Add style profile state to settingsStore

**Files:**
- Modify: `frontend/src/stores/settingsStore.ts`

- [ ] **Step 1: Add style profile state and actions**

Add import for the new API functions and types. Update the existing import line:

```typescript
import type { PromptFile, StyleProfile, StyleProfileSummary } from '../types/settings'
```

Add new API function imports:

```typescript
import { listStyleProfiles, getStyleProfile, saveStyleProfile } from '../api/styleApi'
```

Wait — the API functions were added to `settingsApi.ts`, not a new file. So just add to the existing import:

```typescript
import * as api from '../api/settingsApi'
```

This already imports everything, so no change needed to the import.

Add these refs inside the `defineStore` callback (after the existing refs, around line 57):

```typescript
// Style profile state
const styleProfiles = ref<StyleProfileSummary[]>([])
const selectedStyleDirection = ref('GO_BACKEND')
const selectedStyleLevel = ref('BASIC')
const styleProfileDraft = ref<StyleProfile | null>(null)
const isStyleLoading = ref(false)
const isStyleSaving = ref(false)
```

Add these async functions before the `return` statement:

```typescript
async function loadStyleProfiles() {
  error.value = ''
  try {
    styleProfiles.value = await api.listStyleProfiles()
  } catch (e: any) {
    error.value = e.message || '加载风格配置失败'
  }
}

async function selectStyleProfile(direction: string, level: string) {
  selectedStyleDirection.value = direction
  selectedStyleLevel.value = level
  isStyleLoading.value = true
  error.value = ''
  try {
    styleProfileDraft.value = await api.getStyleProfile(direction, level)
  } catch (e: any) {
    error.value = e.message || '加载风格配置失败'
  } finally {
    isStyleLoading.value = false
  }
}

async function saveStyleProfileDraft() {
  if (!styleProfileDraft.value) return
  isStyleSaving.value = true
  error.value = ''
  successMessage.value = ''
  try {
    const saved = await api.saveStyleProfile(
      selectedStyleDirection.value,
      selectedStyleLevel.value,
      {
        focusAreas: styleProfileDraft.value.focusAreas,
        scenarioPreference: styleProfileDraft.value.scenarioPreference,
        keywordStyle: styleProfileDraft.value.keywordStyle,
      }
    )
    styleProfileDraft.value = saved
    successMessage.value = `${selectedStyleDirection.value} / ${selectedStyleLevel.value} 风格配置已保存`
    await loadStyleProfiles()
  } catch (e: any) {
    error.value = e.message || '保存风格配置失败'
  } finally {
    isStyleSaving.value = false
  }
}
```

Add to the `return` statement:

```typescript
styleProfiles, selectedStyleDirection, selectedStyleLevel, styleProfileDraft,
isStyleLoading, isStyleSaving,
loadStyleProfiles, selectStyleProfile, saveStyleProfileDraft,
```

- [ ] **Step 2: Verify frontend builds**

Run: `cd /Users/fengfan/interviewAssistant/frontend && npm run build`
Expected: Build succeeds

- [ ] **Step 3: Commit**

```bash
git add frontend/src/stores/settingsStore.ts
git commit -m "feat(style): add style profile state and actions to settingsStore"
```

---

### Task 9: Add style profile UI card to SettingsView

**Files:**
- Modify: `frontend/src/views/SettingsView.vue`

- [ ] **Step 1: Add style profile card to template**

Insert a new section between the Obsidian Vault card (ending `</section>` around line 179) and the Prompt Manager section (starting around line 183). Add this new card:

```html
<!-- Style Profiles Card -->
<section class="bg-surface-container-lowest p-6 rounded-xl shadow-sm">
  <div class="flex items-center gap-3 mb-1">
    <span class="material-symbols-outlined text-primary">palette</span>
    <h3 class="font-headline font-bold text-on-surface">提示词风格</h3>
  </div>
  <p class="text-sm text-on-surface-variant mb-5 ml-9">
    为不同的面试方向和题型定制出题风格，让题目更有针对性。保存后下一次 AI 请求立即生效。
  </p>

  <label class="text-xs font-label text-on-surface-variant block mb-2">面试方向</label>
  <div class="flex flex-wrap gap-2 mb-4">
    <button
      v-for="d in ['GO_BACKEND', 'REACT_FRONTEND', 'SYSTEM_DESIGN', 'DATABASE_RELATED', 'AI_CODING']"
      :key="d"
      @click="store.selectStyleProfile(d, store.selectedStyleLevel)"
      class="text-xs px-3 py-1.5 rounded-full transition-colors"
      :class="store.selectedStyleDirection === d ? 'bg-primary text-on-primary' : 'bg-surface-container-high text-on-surface-variant hover:text-on-surface'"
    >
      {{ directionLabel(d) }}
    </button>
  </div>

  <label class="text-xs font-label text-on-surface-variant block mb-2">题型</label>
  <div class="flex flex-wrap gap-2 mb-4">
    <button
      v-for="l in ['BASIC', 'DEEP_PRINCIPLE', 'PROJECT_PRACTICE']"
      :key="l"
      @click="store.selectStyleProfile(store.selectedStyleDirection, l)"
      class="text-xs px-3 py-1.5 rounded-full transition-colors"
      :class="store.selectedStyleLevel === l ? 'bg-primary text-on-primary' : 'bg-surface-container-high text-on-surface-variant hover:text-on-surface'"
    >
      {{ levelLabel(l) }}
    </button>
  </div>

  <div v-if="store.isStyleLoading" class="flex items-center gap-2 text-primary mb-4">
    <span class="material-symbols-outlined animate-spin text-base">progress_activity</span>
    <span class="text-sm">加载中...</span>
  </div>

  <template v-else-if="store.styleProfileDraft">
    <label class="text-xs font-label text-on-surface-variant block mb-2">出题侧重领域</label>
    <textarea
      v-model="store.styleProfileDraft.focusAreas"
      class="w-full min-h-[80px] bg-surface-container-high text-on-surface border-none rounded-lg px-4 py-3 text-sm focus:ring-2 focus:ring-primary outline-none resize-y"
      placeholder="例如：Go runtime 原理、并发模型、GC 调优"
    ></textarea>

    <label class="text-xs font-label text-on-surface-variant block mb-2 mt-3">场景偏好</label>
    <textarea
      v-model="store.styleProfileDraft.scenarioPreference"
      class="w-full min-h-[80px] bg-surface-container-high text-on-surface border-none rounded-lg px-4 py-3 text-sm focus:ring-2 focus:ring-primary outline-none resize-y"
      placeholder="例如：微服务拆分、分布式事务、高可用缓存"
    ></textarea>

    <label class="text-xs font-label text-on-surface-variant block mb-2 mt-3">关键词风格</label>
    <textarea
      v-model="store.styleProfileDraft.keywordStyle"
      class="w-full min-h-[60px] bg-surface-container-high text-on-surface border-none rounded-lg px-4 py-3 text-sm focus:ring-2 focus:ring-primary outline-none resize-y"
      placeholder="例如：偏向底层原理术语，如 GMP 调度模型、三色标记法"
    ></textarea>

    <div class="flex justify-end mt-4">
      <button
        @click="store.saveStyleProfileDraft()"
        :disabled="store.isStyleSaving"
        class="bg-primary text-on-primary font-label font-medium rounded-lg px-6 py-2.5 shadow-md hover:opacity-90 transition-opacity flex items-center gap-2 disabled:opacity-50"
      >
        <span v-if="store.isStyleSaving" class="material-symbols-outlined animate-spin text-sm">progress_activity</span>
        <span v-else class="material-symbols-outlined text-sm">save</span>
        {{ store.isStyleSaving ? '保存中...' : '保存风格' }}
      </button>
    </div>
  </template>
</section>
```

- [ ] **Step 2: Add helper functions and onMounted hook update**

Add these helper functions in the `<script setup>` block:

```typescript
function directionLabel(d: string): string {
  const labels: Record<string, string> = {
    GO_BACKEND: 'Go 后端',
    REACT_FRONTEND: 'React 前端',
    SYSTEM_DESIGN: '系统设计',
    DATABASE_RELATED: '数据库相关',
    AI_CODING: 'AI Agent',
  }
  return labels[d] || d
}

function levelLabel(l: string): string {
  const labels: Record<string, string> = {
    BASIC: '基础八股',
    DEEP_PRINCIPLE: '深度原理',
    PROJECT_PRACTICE: '项目实战',
  }
  return labels[l] || l
}
```

Update the `onMounted` to also load style profiles:

```typescript
onMounted(() => {
  store.loadModel()
  store.loadVaultConfig()
  store.loadPrompts()
  store.loadStyleProfiles()
  store.selectStyleProfile('GO_BACKEND', 'BASIC')
})
```

- [ ] **Step 3: Verify frontend builds**

Run: `cd /Users/fengfan/interviewAssistant/frontend && npm run build`
Expected: Build succeeds

- [ ] **Step 4: Commit**

```bash
git add frontend/src/views/SettingsView.vue
git commit -m "feat(style): add style profile management card to Settings page"
```

---

### Task 10: End-to-end verification

**Files:** None (verification only)

- [ ] **Step 1: Full backend compilation and tests**

Run: `cd /Users/fengfan/interviewAssistant/backend && ./mvnw test -q`
Expected: All tests pass

- [ ] **Step 2: Frontend build**

Run: `cd /Users/fengfan/interviewAssistant/frontend && npm run build`
Expected: Build succeeds

- [ ] **Step 3: Manual smoke test**

1. Start backend: `cd backend && mvn spring-boot:run`
2. Start frontend: `cd frontend && npm run dev`
3. Open Settings page
4. Verify "提示词风格" card appears between Obsidian Vault and Prompt Manager
5. Click different direction and level buttons, verify the form updates
6. Edit a field, click save, verify success message
7. Refresh the page, verify the saved values persist
8. Go to Interview room, start a mock interview, verify questions reflect the style
