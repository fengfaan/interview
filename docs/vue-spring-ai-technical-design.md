# AI Career Prep Hub 技术设计方案

## 1. 技术目标

当前阶段目标是做出一个可以实际使用的最小可用版本，先不引入登录、权限、支付、复杂历史记录等能力。

技术选型：

- 前端：Vue 3 + TypeScript + Vite
- 后端：Spring Boot 3 + Spring AI
- 通信：REST JSON 为主，AI 长文本输出使用 SSE Streaming
- 状态保存：前端 LocalStorage
- 数据库：暂不需要，后续如需保存长期记录再引入

核心页面：

- 面试演练室
- 简历调优台

## 2. 总体架构

```text
Browser
  |
  | Vue 3 SPA
  | - 页面交互
  | - 本地草稿保存
  | - SSE 流式渲染
  |
  v
Spring Boot API
  |
  | Spring AI
  | - Prompt 编排
  | - 模型调用
  | - 结构化 JSON 输出
  | - Streaming 输出
  |
  v
LLM Provider
```

### 2.1 前端职责

- 提供两个可用页面
- 负责用户输入、结果展示、快捷键、Loading 状态
- 通过 LocalStorage 保存页面草稿与 AI 结果
- 对 SSE 返回内容进行流式追加渲染

### 2.2 后端职责

- 封装 Spring AI 调用
- 提供面试问题生成、回答点评、深度追问、简历匹配分析、STAR 改写接口
- 将 Prompt 逻辑集中在后端，避免前端散落业务规则
- 对 AI 输出做基础格式约束与容错

## 3. 项目结构建议

### 3.1 根目录

```text
interviewAssistant/
  frontend/
  backend/
  docs/
```

### 3.2 前端目录

```text
frontend/
  src/
    api/
      interviewApi.ts
      resumeApi.ts
      streamClient.ts
    components/
      AppSidebar.vue
      BaseButton.vue
      BaseTextarea.vue
      LoadingText.vue
    layouts/
      AppLayout.vue
    router/
      index.ts
    stores/
      interviewStore.ts
      resumeStore.ts
    views/
      MockInterviewRoomView.vue
      ResumeOptimizerView.vue
    types/
      interview.ts
      resume.ts
    utils/
      localStorage.ts
      shortcuts.ts
    App.vue
    main.ts
```

### 3.3 后端目录

```text
backend/
  src/main/java/com/interviewassistant/
    InterviewAssistantApplication.java
    config/
      AiConfig.java
      CorsConfig.java
    controller/
      InterviewController.java
      ResumeController.java
    service/
      InterviewAiService.java
      ResumeAiService.java
    prompt/
      InterviewPrompts.java
      ResumePrompts.java
    dto/
      interview/
      resume/
    common/
      ApiResponse.java
      SseUtils.java
```

## 4. 前端设计

### 4.1 路由

- `/interview`：面试演练室
- `/resume`：简历调优台
- `/`：默认重定向到 `/interview`

### 4.2 页面布局

使用一个统一布局：

- 左侧固定侧边栏
- 右侧页面内容区
- 桌面端优先，移动端暂不做复杂适配，只保证可滚动可使用

### 4.3 面试演练室组件拆分

```text
MockInterviewRoomView.vue
  InterviewConfigBar.vue
  InterviewQuestionCard.vue
  InterviewAnswerEditor.vue
  InterviewFeedbackPanel.vue
  KeywordHitList.vue
  FollowUpCard.vue
```

首版可以先把子组件合并在页面内实现，等逻辑稳定后再拆分。

### 4.4 简历调优台组件拆分

```text
ResumeOptimizerView.vue
  JobDescriptionEditor.vue
  ResumeMarkdownEditor.vue
  MatchScorePanel.vue
  OptimizationSuggestionList.vue
  StarRewritePanel.vue
```

首版同样可以先在页面内完成，避免过早抽象。

### 4.5 前端状态

建议使用 Pinia。如果为了更快启动，也可以直接使用 Vue `ref/reactive` 加 LocalStorage composable。

面试模块 LocalStorage key：

- `ai-career-prep.mock-interview`

简历模块 LocalStorage key：

- `ai-career-prep.resume-optimizer`

### 4.6 SSE 流式处理

前端建议封装统一方法：

```ts
export async function streamPost(
  url: string,
  body: unknown,
  onChunk: (text: string) => void
): Promise<void>
```

实现方式：

- 使用 `fetch`
- 读取 `response.body.getReader()`
- 用 `TextDecoder` 解码 chunk
- 将 chunk 追加到当前 AI 输出字段

## 5. 后端设计

### 5.1 Spring AI 使用方式

后端统一通过 Spring AI `ChatClient` 调用模型。

建议在 Service 中封装：

- 非流式接口：适合生成结构化 JSON
- 流式接口：适合点评、示范回答、STAR 改写等长文本

### 5.2 配置项

通过环境变量或 `application.yml` 配置模型供应商：

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4.1-mini
```

模型名后续可以按实际供应商调整。技术方案只要求后端通过 Spring AI 适配，不把模型细节写死在前端。

### 5.3 CORS

开发阶段允许：

- `http://localhost:5173`

生产部署时再收紧。

## 6. API 设计

统一前缀：

- `/api/interview`
- `/api/resume`

### 6.1 面试：生成问题

`POST /api/interview/question`

请求：

```json
{
  "direction": "GO_BACKEND",
  "level": "DEEP_PRINCIPLE",
  "history": []
}
```

响应：

```json
{
  "questionId": "q_001",
  "question": "请解释一下 Go 的垃圾回收机制。",
  "expectedKeywords": ["三色标记", "写屏障", "STW", "并发标记"]
}
```

### 6.2 面试：提交回答并生成点评

`POST /api/interview/feedback/stream`

请求：

```json
{
  "direction": "GO_BACKEND",
  "level": "DEEP_PRINCIPLE",
  "question": "请解释一下 Go 的垃圾回收机制。",
  "answer": "用户回答内容",
  "expectedKeywords": ["三色标记", "写屏障", "STW", "并发标记"]
}
```

响应：

- `text/event-stream`
- 流式返回一段 Markdown 文本

建议输出结构：

```markdown
## 命中情况

- 命中：三色标记、STW
- 遗漏：写屏障、并发标记

## 点评

...

## 口语化示范

...

## 深度追问

...
```

首版先用 Markdown 流式文本，避免一开始强依赖复杂 JSON Streaming。

### 6.3 面试：跳过题目

跳过题目可以复用生成问题接口，并把当前题目写入 `history`，标记为 skipped。

### 6.4 简历：匹配分析

`POST /api/resume/analyze`

请求：

```json
{
  "jobDescription": "JD 内容",
  "resume": "简历 Markdown 内容"
}
```

响应：

```json
{
  "score": 72,
  "dimensions": [
    {
      "name": "并发编程",
      "score": 90,
      "reason": "简历中有明确的高并发项目描述"
    },
    {
      "name": "架构能力",
      "score": 40,
      "reason": "缺少系统拆分、容量规划和权衡描述"
    }
  ],
  "suggestions": [
    {
      "id": "s_001",
      "priority": "HIGH",
      "title": "补充架构设计的量化结果",
      "reason": "JD 强调架构能力，但简历项目描述偏执行层面。",
      "sourceText": "负责订单系统开发"
    }
  ]
}
```

### 6.5 简历：STAR 改写

`POST /api/resume/rewrite/stream`

请求：

```json
{
  "jobDescription": "JD 内容",
  "resume": "简历 Markdown 内容",
  "suggestion": {
    "id": "s_001",
    "title": "补充架构设计的量化结果",
    "sourceText": "负责订单系统开发"
  }
}
```

响应：

- `text/event-stream`
- 流式返回 Markdown

建议输出：

```markdown
### 原始描述

负责订单系统开发

### STAR 改写范例

通过 [XX 技术] 重构订单链路，解决了 [XX 问题]，使 [核心指标] 提升 [XX%]。

### 填写建议

- [XX 技术]：替换为真实技术栈
- [核心指标]：替换为延迟、吞吐、稳定性或人效指标
```

## 7. Prompt 设计原则

### 7.1 面试 Prompt

角色：

- 资深技术面试官
- 目标是帮助用户练习表达，而不是只给标准答案

约束：

- 问题要贴近真实面试
- 点评要指出命中点、遗漏点、表达问题
- 示范回答不超过 300 字
- 深度追问必须基于用户刚才的回答生成

### 7.2 简历 Prompt

角色：

- 技术招聘视角的简历优化顾问

约束：

- 只基于用户提供的 JD 和简历内容分析
- 不编造不存在的经历
- 可以使用占位符引导用户补充真实数据
- 建议要能直接转化为简历修改动作

## 8. 最小可交付版本

### 8.1 前端必须完成

- Vue 项目可启动
- 侧边导航可切换两个页面
- 面试演练室可以：
  - 选择方向和强度
  - 生成题目
  - 输入回答
  - 流式查看点评
  - 保存当前状态
- 简历调优台可以：
  - 输入 JD
  - 输入简历
  - 生成匹配分析
  - 查看优化建议
  - 流式生成 STAR 改写
  - 保存当前状态

### 8.2 后端必须完成

- Spring Boot 项目可启动
- 接入 Spring AI
- 提供 4 个核心接口：
  - `POST /api/interview/question`
  - `POST /api/interview/feedback/stream`
  - `POST /api/resume/analyze`
  - `POST /api/resume/rewrite/stream`
- 支持本地开发 CORS

### 8.3 暂不做

- 登录注册
- 用户权限
- 服务端历史记录
- 文件上传解析
- 简历导出 PDF
- 复杂图表
- 多模型切换 UI

## 9. 开发顺序建议

1. 搭建 `backend` Spring Boot + Spring AI 基础项目
2. 实现面试问题生成接口
3. 实现面试反馈 Streaming 接口
4. 搭建 `frontend` Vue + Vite 基础项目
5. 完成侧边导航和面试演练室页面
6. 实现简历分析接口
7. 实现 STAR 改写 Streaming 接口
8. 完成简历调优台页面
9. 加入 LocalStorage、快捷键和基础错误提示

## 10. 风险与处理

### 10.1 AI 输出格式不稳定

首版避免过度依赖复杂结构化流式 JSON。长文本使用 Markdown Streaming，结构化分析使用普通 JSON 接口。

### 10.2 用户输入过长

后端需要对 JD 和简历长度做基础限制。首版可限制单字段 20,000 字符以内。

### 10.3 模型调用失败

前端展示可重试错误；后端返回清晰错误信息，不吞异常。

### 10.4 没有数据库

首版用 LocalStorage 保持可用。等功能稳定后，再考虑用户体系和服务端存储。
