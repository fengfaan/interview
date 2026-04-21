## Why

需要一个可实际使用的 AI 面试准备工具，帮助用户通过模拟面试练习和简历优化来提升求职竞争力。当前阶段目标是交付最小可用版本，验证核心流程可行后再迭代。技术方案和需求文档已完成，设计稿已出图，现在需要把三者整合落地。

## What Changes

- 新建 Spring Boot 3 后端项目，集成 Spring AI + 智谱（ZhiPu AI）作为 LLM Provider
- 新建 Vue 3 + TypeScript + Vite 前端项目，实现两个核心页面
- 实现面试演练室：方向/强度选择 → AI 生成问题 → 用户回答 → 关键词命中分析（JSON）+ 流式点评（SSE）→ 深度追问循环
- 实现简历调优台：JD 输入 + 简历编辑 → 匹配度分析（JSON）→ 优化建议 → STAR 改写（SSE）→ 一键应用
- 实现统一侧边栏导航，桌面端优先
- 使用 LocalStorage 做状态持久化，不引入数据库
- 不做登录注册、用户体系、支付、文件上传解析等

## Capabilities

### New Capabilities

- `interview-room`: 面试演练室 — 问题生成、回答提交、关键词命中分析、流式点评、深度追问的完整闭环
- `resume-optimizer`: 简历调优台 — JD/简历输入、匹配度分析、优化建议列表、STAR 改写流式输出、一键应用到简历
- `app-shell`: 应用骨架 — Vue 项目初始化、路由、侧边栏导航、布局系统、设计系统（配色/字体/间距）
- `backend-core`: 后端核心 — Spring Boot 项目初始化、Spring AI 智谱集成、CORS 配置、统一错误响应、SSE 工具类
- `local-storage`: 本地存储 — 两个页面的状态持久化、草稿保存、会话恢复

### Modified Capabilities

（无已有 capability 需要修改）

## Impact

- **新增代码**：`frontend/` 和 `backend/` 两个独立项目目录
- **API 契约**：6 个后端接口（2 个面试 JSON、1 个面试 SSE、1 个简历 JSON、1 个简历 SSE、1 个生成问题）
- **外部依赖**：Spring AI ZhiPu AI starter、Vue 3、Pinia、Vue Router、Tailwind CSS
- **运行时依赖**：需要 ZhiPu AI API Key（通过环境变量 `ZHIPU_API_KEY` 配置）
- **开发环境**：Node.js 18+、JDK 17+、Maven
