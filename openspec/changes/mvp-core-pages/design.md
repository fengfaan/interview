## Context

AI Career Prep Hub 是一个面试准备工具，包含面试演练室和简历调优台两个核心模块。当前阶段交付 MVP，不做用户体系。项目已有完整的技术设计文档（docs/vue-spring-ai-technical-design.md）和产品需求文档（docs/core-modules-requirements.md），以及 Stitch 导出的设计稿（stitch-export/）。

**关键决策记录**（来自 explore 阶段讨论）：
1. 布局以文档为准，不跟随设计稿的三栏布局
2. 面试反馈接口拆成 JSON（关键词命中）+ SSE（流式点评）两个端点
3. LLM Provider 使用智谱（ZhiPu AI）
4. 状态管理使用 Pinia

## Goals / Non-Goals

**Goals:**
- 可本地启动运行的最小可用产品
- 面试演练室完整闭环：出题 → 回答 → 关键词分析 + 流式点评 → 追问
- 简历调优台完整闭环：输入 JD/简历 → 匹配分析 → 建议 → STAR 改写
- LocalStorage 状态持久化，刷新不丢失
- 统一设计系统（配色、字体、间距）

**Non-Goals:**
- 登录注册 / 用户体系
- 数据库 / 服务端存储
- 文件上传解析（PDF/DOCX）
- 语音输入
- 多模型切换 UI
- 移动端复杂适配（仅保证可滚动可用）
- Dashboard / Analytics / History 页面
- 简历导出 PDF

## Decisions

### D1: 前端技术栈 — Vue 3 + TypeScript + Vite + Pinia + Tailwind CSS

Tailwind CSS 用于快速还原设计系统的配色和间距。设计稿已导出 Tailwind 配色变量（surface、primary、secondary 等），可直接复用。

### D2: 后端技术栈 — Spring Boot 3 + Spring AI + ZhiPu AI

使用 `spring-ai-zhipuai-spring-boot-starter` 集成智谱。模型选择 `glm-4-flash`（性价比）或 `glm-4-plus`（效果），通过配置切换，不改代码。

### D3: 面试反馈接口拆分

原方案是一个 SSE 端点返回混合 Markdown。拆为两个：
- `POST /api/interview/feedback` — 同步 JSON，返回关键词命中 + 评分，前端立即渲染 Keyword Radar
- `POST /api/interview/feedback/stream` — SSE 流式 Markdown，返回点评 + 示范回答 + 深度追问

**替代方案**：在 SSE 流中用 `event:` 字段区分不同类型的事件。被否决的原因是增加 SSE 解析复杂度，且关键词命中需要先于流式内容返回。

### D4: 面试布局采用上下三段式（文档方案）

- 顶部：配置栏（方向、强度、开始）
- 中部：面试官问题气泡（左）+ 用户作答区（右）
- 底部：反馈抽屉（默认折叠，提交后展开）

**替代方案**：设计稿的三栏布局（左 Q&A + 右面板）。被否决的原因是文档方案更符合需求描述中的交互逻辑，且底部抽屉在移动端更容易适配。

### D5: 简历布局采用左右分栏 + 底部 STAR 编辑器

- 左栏：JD 输入（上）+ 简历编辑（下）
- 右栏：匹配度分析 + 优化建议列表
- 底部：点击建议后展开 STAR 重构工作区

### D6: "Apply Fix" 纯前端实现

用户点击"应用到简历"时，前端根据 `sourceText` 字段在简历文本中定位并替换为 STAR 改写内容，不调用后端接口。

### D7: 深度追问的衔接方式

反馈 JSON 中包含 `followUpQuestion` 字段。用户点击"Continue Challenge"时，前端将此问题直接作为下一轮题目，跳过"生成问题"步骤。

### D8: History 管理策略

- History 数组只保留最近 5 轮 Q&A 的摘要（问题 + 回答前 200 字）
- 超过 10 轮时提示"本轮面试结束"
- LocalStorage 只保留最近 3 次会话

## Risks / Trade-offs

**[智谱 API 稳定性]** → 后端对 AI 调用做 60s 超时 + 重试（1 次），前端展示可重试错误提示

**[Context Window 膨胀]** → History 限制最近 5 轮摘要，超出截断（见 D8）

**[SSE 流式中途断开]** → 前端检测连接断开，显示"生成中断"并提供"重新生成"按钮

**[LocalStorage 容量]** → 约 5MB 限制，保留最近 3 次会话，旧数据自动清理

**[Markdown 渲染]** → SSE 流式 Markdown 需要前端实时渲染，使用 `markdown-it` 或 `marked` 库

**[智谱模型输出格式不稳定]** → 结构化 JSON 接口使用 Spring AI 的结构化输出能力（`BeanOutputConverter`）约束格式；SSE 流式接口容忍格式偏差
