# ReAct Agent Function Call 重构设计

## 背景

当前 `DeepDiveAgent` 通过 `AiGateway.generateJson()` 让 LLM 返回结构化 JSON（`AgentDecision`）来模拟工具选择，这不是原生 function call。需要迁移到 Spring AI 1.1.1 的原生 function calling API，并支持多轮工具调用。

## 方案

手动 ReAct 循环 + Spring AI function call。不使用 Spring AI 的自动 `ToolCallAdvisor`（因为它不暴露中间工具调用事件），改为手动循环以便在每轮工具调用时发送 `agent_step` SSE 事件给前端。

### 流程

```
DeepDiveAgent.execute():
  1. 构建 system prompt + user prompt
  2. 循环 (最多 3 轮):
     a. 调用 ChatModel.call(Prompt)，携带 tools 定义
     b. 检查 ChatResponse 是否包含 assistantMessage.toolCalls
     c. 如果有 tool_call:
        - 执行 searchNotes 工具
        - 发送 agent_step SSE 事件
        - 将 assistantMessage + ToolResponseMessage 追加到消息历史
        - 继续循环
     d. 如果无 tool_call（纯文本回复）:
        - 结束循环
  3. 用 AiGateway.streamText() 流式输出最终回答
```

### 文件变更

#### 新增文件

**`agent/KnowledgeTools.java`** — 搜索知识库工具类

```java
@Component
public class KnowledgeTools {

    private final ObsidianService obsidianService;

    @Tool(description = "搜索知识库笔记。根据关键词在 Obsidian 面试知识库中查找相关笔记，返回匹配的笔记标题和标签。当追问涉及需要补充资料的知识点时使用此工具。")
    public String searchNotes(
            @ToolParam(description = "搜索关键词，2-5个字，应聚焦于具体技术概念") String keyword) {
        if (!obsidianService.isVaultConfigured()) {
            return "知识库未配置，无法搜索。";
        }
        List<NoteItem> notes = obsidianService.searchNotes(keyword);
        if (notes.isEmpty()) {
            return "未找到与「" + keyword + "」相关的笔记。";
        }
        // 返回格式化的笔记列表
    }
}
```

**`prompts/interview/deep-dive-agent-system.md`** — agent 系统提示词

引导 LLM 使用 `search_notes` 工具检索知识库，说明何时该搜索、何时直接回答。替代原来的 `deep-dive-agent-decide.md`。

#### 修改文件

**`agent/DeepDiveAgent.java`** — 重写核心逻辑

- 移除 `decide()` 方法和 `searchKnowledge()` 调用
- 新增手动 ReAct 循环：调用 `ChatModel.call()` 检查 tool calls，执行工具，发送 SSE 事件，循环直到 LLM 不再请求工具
- 最大 3 轮工具调用
- 每轮工具调用后发送 `agent_step` SSE 事件（格式不变：keyword + noteTitles）
- 循环结束后，将所有工具结果合并到最终 prompt，调用 `streamText()` 流式输出

**`service/AiGateway.java`** — 新增方法

- 新增 `callWithTools(String systemPrompt, List<Message> messages, ToolCallback... tools)` 方法
- 返回 `ChatResponse`（而非文本），以便调用方检查 tool calls
- 内部使用 `ChatModel.call(Prompt)` 并传入包含 tool 定义的 options

#### 删除文件

- **`agent/AgentDecision.java`** — 不再需要手动决策 DTO
- **`prompts/interview/deep-dive-agent-decide.md`** — 不再需要决策 prompt

#### 不变

- 前端代码无变更 — `agent_step` SSE 事件格式保持 `{ keyword, notes: string[] }`
- `ObsidianService.searchNotes()` 保持不变
- `SseUtils.sendAgentStep()` 保持不变

### 关键实现细节

1. **ChatModel 直接调用**：通过 `AiConfig` 获取底层 `OpenAiChatModel`（而非 `ChatClient`），直接调用 `model.call(Prompt)` 获取完整 `ChatResponse`，检查 `result.output.toolCalls`
2. **工具注册**：通过 `MethodToolCallbackProvider` 从 `KnowledgeTools` 提取 `ToolCallback`，设置到 `OpenAiChatOptions` 的 `toolCallbacks` 中
3. **消息历史**：使用 Spring AI 的 `UserMessage`、`AssistantMessage`、`ToolResponseMessage` 维护对话上下文
4. **流式输出**：最后一轮工具结果注入 user prompt 后，走现有的 `AiGateway.streamText()` 流式输出
5. **错误处理**：工具执行失败时返回错误消息给 LLM（不抛异常），让 LLM 决定是否继续或直接回答；整体异常时发送 `AGENT_ERROR` SSE 事件

### 约束

- 最大 3 轮工具调用，防止无限循环
- 每轮工具调用同步执行（因为需要发送 SSE 事件）
- 如果知识库未配置，工具直接返回提示信息，LLM 会据此直接回答
