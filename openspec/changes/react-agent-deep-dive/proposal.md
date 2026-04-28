## Why

深度追问功能目前是单轮 AI 调用——前端把完整历史发给后端，后端一次性返回回答。当上下文很长时（推荐答案 + 多轮对话历史），prompt 超出模型窗口或质量下降。需要在 AiGateway 之上引入一个轻量 Agent 层，支持 ReAct 模式（推理+行动），先接入"深度追问 + 知识库检索"场景：Agent 可以判断是否需要检索 Obsidian 知识库来补充回答，而不是仅依赖 prompt 中的上下文。

同时，`buildDeepDivePrompt` 需要上下文压缩能力——当推荐答案或反馈点评过长时，保留与关键词和追问相关的句子，而不是原样灌入 prompt。

## What Changes

- 新增 `DeepDiveAgent`，在 `AiGateway` 之上实现 ReAct 循环：思考 → 决定是否检索知识库 → 执行检索 → 整合结果 → 回答
- 新增上下文压缩工具方法，按句子拆分长文本，保留与关键词/追问相关的内容，控制在 4000 字符以内
- 新增长对话历史截断（保留最近 N 轮）和单条消息压缩
- `InterviewAiService.buildDeepDivePrompt()` 调用压缩方法处理 `contextContent` 和 `messages`
- 深度追问 SSE 端点可以返回 Agent 中间步骤（检索了哪些知识库笔记），前端可选展示

## Capabilities

### New Capabilities
- `context-compactor`: 上下文压缩——按句子拆分、按关键词/追问相关性评分、截断到目标长度，fallback 到头尾保留策略
- `react-agent`: ReAct Agent 层——思考→行动→观察循环，支持知识库检索作为 Tool，输出流式回答

### Modified Capabilities

## Impact

- **后端**: 新增 `DeepDiveAgent`、`ContextCompactor` 类，修改 `InterviewAiService.buildDeepDivePrompt()` 调用压缩
- **API**: `POST /api/interview/deep-dive/stream` 端点行为不变（仍是 SSE），但内部走 Agent 层而非直调 AiGateway
- **依赖**: 可能需要 `ObsidianService` 暴露检索方法（当前已有文件遍历能力，需加关键词搜索）
- **测试**: 已有 10 个测试定义了压缩行为（3 个长上下文 + 2 个长历史 + 1 个长消息 + 2 个 null/empty + 1 个短上下文 + 1 个全低价值 fallback），需全部通过
