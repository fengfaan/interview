## Context

当前简历模块包含两个功能：
1. **JD匹配分析** (`POST /api/resume/analyze`) — 需要 JD + 简历，返回匹配度评分、维度评分、优化建议（`AnalyzeResponse`）
2. **STAR 改写** (`POST /api/resume/rewrite/stream`) — 基于单条建议的 SSE 流式改写

后端架构：`ResumeController` → `ResumeAiService`(同步) / `ResumeStreamService`(流式) → `AiGateway` → LLM Provider。Prompt 模板在 `backend/prompts/resume/` 下，已有 `system.md`、`analyze.md`、`rewrite.md`。

前端架构：`ResumeOptimizerView.vue` 左右分栏（左侧输入JD+简历，右侧展示分析结果），`resumeStore.ts` 管理状态并持久化到 LocalStorage，`resumeApi.ts` 封装 API 调用。

两个新功能需要在现有架构上扩展，不改动已有接口。

## Goals / Non-Goals

**Goals:**
- 提供独立于 JD 的简历结构体检能力，诊断宏观布局问题
- 提供多版本深度润色能力，比现有 STAR 改写更丰富（多版本、动词强化、数据化追问）
- 复用现有 AiGateway + PromptService 架构，保持一致的代码风格
- 前端交互清晰直观，结构分析用体检报告卡片，润色用对比视图

**Non-Goals:**
- 不做 PDF/Word 文件上传解析（首期只支持文本粘贴，与现有交互一致）
- 不做简历模板生成或格式化导出
- 不修改现有 `/api/resume/analyze` 和 `/api/resume/rewrite/stream` 的行为
- 不做雷达图可视化（首期用分项评分卡片即可，减少前端复杂度）

## Decisions

### 1. 结构分析用同步 JSON 接口而非流式

**选择**: `POST /api/resume/structure-analysis` 返回同步 JSON

**理由**: 结构分析的输出是结构化评分报告（JSON），不是长文本。同步调用更简单，前端可以直接渲染结果。与现有 `analyze` 接口保持一致的模式。

**替代方案**: SSE 流式 — 对于短 JSON 输出没有体验优势，反而增加前端解析复杂度。

### 2. 深度润色用 SSE 流式接口

**选择**: `POST /api/resume/polish/stream` 返回 SSE 流

**理由**: 润色输出是多版本长文本（技术深度版 + 业务结果版 + 数据化追问），内容量大，流式输出用户体验更好。与现有 `rewrite/stream` 保持一致。

### 3. 结构分析独立于 JD，润色可选 JD

**选择**: 结构分析只需简历文本；润色接受可选的 JD 参数用于关键词融入

**理由**: 结构诊断是简历自身的问题（缺模块、排序乱），不依赖目标职位。润色时如果有 JD 可以更有针对性地融入关键词，但没有 JD 也能独立进行基础润色。

### 4. 复用现有 ResumeController 而非新建 Controller

**选择**: 在 `ResumeController` 中新增两个端点

**理由**: 功能属于同一领域，共享 `/api/resume` 前缀。保持现有的 Controller-per-domain 模式。

### 5. 新增 DTO 而非扩展现有 DTO

**选择**: 新建 `StructureAnalysisRequest/Response`、`PolishStreamRequest`

**理由**: 结构分析不需要 JD（与 `AnalyzeRequest` 不同），输出格式也不同（模块检查 + 红黄绿灯 vs 维度评分 + 建议）。独立 DTO 语义清晰，避免可选字段滥用。

### 6. 前端在现有页面新增 Tab 切换

**选择**: `ResumeOptimizerView` 顶部加 Tab（JD匹配分析 | 结构体检 | 深度润色），共享简历输入区

**理由**: 三个功能共用简历内容输入，用 Tab 切换分析模式最自然。用户可以先做结构体检，再做 JD 匹配，最后逐段润色。

**替代方案**: 新建独立页面 — 需要用户重复粘贴简历内容，体验差。

## Risks / Trade-offs

- **Prompt 输出稳定性** → 结构分析的 JSON 输出依赖 LLM 严格遵守格式。通过 AiGateway.generateJson 的重试机制(2次)缓解，prompt 中明确约束输出格式。
- **润色多版本输出解析** → 流式输出多个版本需要约定分隔标记。通过 prompt 约定 Markdown 标题分隔（`### 版本一：技术深度版`），前端按标题切分。
- **Tab 切换增加页面复杂度** → 三个 Tab 共享简历输入但各自有独立的结果区域，resumeStore 状态会膨胀。通过清晰的状态命名（`structureResult`、`polishResult`）管理。
