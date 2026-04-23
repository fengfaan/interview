## 1. 后端 - Prompt 模板

- [ ] 1.1 创建 `backend/prompts/interview/batch-question.md`，要求 AI 一次输出 10 道题的结构化 JSON（包含 question, answer, keywords）

## 2. 后端 - 批量出题 API

- [ ] 2.1 创建 `BatchQuestionResponse` DTO（包含 questions 数组，每项有 id, question, answer, keywords）
- [ ] 2.2 创建 `BatchQuestionRequest` DTO（包含 direction, level, count）
- [ ] 2.3 在 `InterviewAiService` 中添加批量出题方法：分批调用 AI（每批 10 题），合并结果，带重试和间隔控制
- [ ] 2.4 在 `InterviewController` 中添加 `POST /api/interview/batch-questions` 端点，接受方向/题型/数量参数，返回题目+答案列表

## 3. 前端 - 类型定义和 API

- [ ] 3.1 在 `types/interview.ts` 中添加 `BatchQuestionItem` 和 `BatchQuestionResponse` 类型
- [ ] 3.2 在 `api/interviewApi.ts` 中添加 `generateBatchQuestions` API 调用

## 4. 前端 - Store

- [ ] 4.1 创建 `stores/rapidQuestionStore.ts`，管理：题目列表、生成状态、进度、展开状态、参数选择

## 5. 前端 - 快速刷题页面

- [ ] 5.1 创建 `views/RapidQuestionView.vue`，包含：参数选择区（方向/题型/数量）、题目列表（折叠展开）、保存按钮、重新出题按钮
- [ ] 5.2 实现题目折叠展开交互（CSS transition，每题独立状态）
- [ ] 5.3 实现生成进度显示
- [ ] 5.4 实现单题保存和批量保存到知识库（复用 knowledgeStore）

## 6. 前端 - 路由和导航

- [ ] 6.1 在 `router/index.ts` 中添加 `/rapid` 路由
- [ ] 6.2 在 `AppSidebar.vue` 中添加「快速刷题」导航项（位于面试演练室和知识库之间）
