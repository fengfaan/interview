# ReAct Agent Function Call Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate DeepDiveAgent from manual JSON-based tool selection to Spring AI 1.1.1 native function calling, supporting multi-round tool use with `agent_step` SSE events.

**Architecture:** Manual ReAct loop that calls `OpenAiChatModel.call(Prompt)` with `internalToolExecutionEnabled(false)`. On each iteration, check `ChatResponse` for tool calls, execute them locally, send `agent_step` SSE events, append `ToolResponseMessage` to conversation history, and loop. After the loop ends (no more tool calls or max rounds reached), stream the final answer via `AiGateway.streamText()`.

**Tech Stack:** Spring AI 1.1.1 (`ToolCallback`, `OpenAiChatOptions`, `AssistantMessage.ToolCall`, `ToolResponseMessage`), Java 17, JUnit 5 + Mockito.

---

## File Structure

| Action | File | Responsibility |
|--------|------|----------------|
| Create | `agent/KnowledgeTools.java` | `@Tool`-annotated POJO wrapping `ObsidianService.searchNotes()` |
| Modify | `config/AiConfig.java` | Add `getCurrentChatModel()` getter exposing `OpenAiChatModel` |
| Modify | `agent/DeepDiveAgent.java` | Rewrite with manual ReAct loop using function call |
| Modify | `service/AiGateway.java` | Add `callWithTools()` method for non-streaming tool-call requests |
| Create | `prompts/interview/deep-dive-agent-system.md` | System prompt guiding LLM to use `search_notes` tool |
| Delete | `agent/AgentDecision.java` | No longer needed — LLM decides via function call |
| Delete | `prompts/interview/deep-dive-agent-decide.md` | No longer needed — replaced by agent system prompt |
| Rewrite | `test/agent/DeepDiveAgentTest.java` | Update all tests for new ReAct loop behavior |

---

### Task 1: Expose `OpenAiChatModel` from `AiConfig`

**Files:**
- Modify: `backend/src/main/java/com/interviewassistant/config/AiConfig.java:47-58`

- [ ] **Step 1: Add `currentChatModel` field and getter**

The `OpenAiChatModel` is already created at line 132 but only stored indirectly via `ChatClient.builder(model).build()`. Add a field to hold it and a getter.

Add after line 49 (`private volatile String currentKeyMask = "";`):

```java
private volatile OpenAiChatModel currentChatModel;
```

Add getter after `getCurrentKeyMask()` (after line 74):

```java
public OpenAiChatModel getCurrentChatModel() {
    if (!realKeyConfigured || currentChatModel == null) {
        throw new IllegalStateException("请先在设置页面配置 API Key");
    }
    return currentChatModel;
}
```

In `refreshClient()`, after line 132 (`OpenAiChatModel model = modelBuilder.build();`), add:

```java
this.currentChatModel = model;
```

In `clearClient()`, after line 90 (`this.currentChatClient = null;`), add:

```java
this.currentChatModel = null;
```

- [ ] **Step 2: Run compilation to verify**

Run: `cd backend && ./mvnw compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/interviewassistant/config/AiConfig.java
git commit -m "feat(config): expose OpenAiChatModel for direct tool-call access"
```

---

### Task 2: Create `KnowledgeTools` — `@Tool` wrapper for `ObsidianService`

**Files:**
- Create: `backend/src/main/java/com/interviewassistant/agent/KnowledgeTools.java`

- [ ] **Step 1: Create the tool class**

```java
package com.interviewassistant.agent;

import com.interviewassistant.dto.knowledge.NoteItem;
import com.interviewassistant.service.ObsidianService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class KnowledgeTools {

    private final ObsidianService obsidianService;

    public KnowledgeTools(ObsidianService obsidianService) {
        this.obsidianService = obsidianService;
    }

    @Tool(description = "搜索知识库笔记。在 Obsidian 面试知识库中根据关键词查找相关笔记，返回笔记标题和标签。当候选人追问涉及需要补充资料的具体技术知识点时使用此工具。")
    public String searchNotes(
            @ToolParam(description = "搜索关键词，2-5个字，聚焦于具体技术概念，如'B+树'、'Redis集群'、'CAP定理'") String keyword) {
        if (!obsidianService.isVaultConfigured()) {
            return "知识库未配置，无法搜索。请直接基于现有信息回答。";
        }
        try {
            List<NoteItem> notes = obsidianService.searchNotes(keyword);
            if (notes.isEmpty()) {
                return "未找到与「" + keyword + "」相关的笔记。请直接基于现有信息回答。";
            }
            StringBuilder sb = new StringBuilder();
            sb.append("找到 ").append(notes.size()).append(" 条相关笔记：\n");
            for (NoteItem note : notes) {
                sb.append("- ").append(note.getTitle());
                if (note.getTags() != null && !note.getTags().isEmpty()) {
                    sb.append(" [").append(String.join(", ", note.getTags())).append("]");
                }
                sb.append(" (").append(note.getId()).append(")\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "搜索知识库时出错：" + e.getMessage() + "。请直接基于现有信息回答。";
        }
    }
}
```

- [ ] **Step 2: Run compilation to verify**

Run: `cd backend && ./mvnw compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/interviewassistant/agent/KnowledgeTools.java
git commit -m "feat(agent): add KnowledgeTools with @Tool-annotated searchNotes"
```

---

### Task 3: Create agent system prompt

**Files:**
- Create: `backend/prompts/interview/deep-dive-agent-system.md`

- [ ] **Step 1: Create the prompt template**

This replaces both the old `deep-dive-agent-decide.md` decision prompt and the old `deep-dive.md` prompt for the ReAct flow. The LLM will receive this as the system prompt and the deep-dive context as the user prompt. It instructs the LLM on when and how to use the `search_notes` tool.

```markdown
你是一位经验丰富的技术面试教练，正在帮助候选人深入理解面试题中的知识点。

你有一个工具可以调用：
- search_notes：搜索 Obsidian 面试知识库中的笔记，返回相关笔记的标题和标签

使用工具的时机：
1. 当候选人追问涉及当前上下文中未充分展开的知识点（如具体技术细节、底层原理、对比分析）时，调用 search_notes 搜索相关笔记
2. 当追问可以基于当前上下文充分回答，或属于泛泛的讨论时，直接回答，不调用工具
3. 不要搜索与原始面试题无关的话题

回答要求：
1. 用深入浅出的方式讲解，善用类比和实际例子帮助理解
2. 将讲解内容与原始面试题和考察要点关联起来
3. 如果候选人的理解有偏差，温和地纠正
4. 使用 Markdown 格式输出，可以包含代码示例
5. 回答要有针对性，不要泛泛而谈
6. 如果搜索到了知识库笔记，在回答中适当引用其中的内容，帮助候选人更深入理解
```

- [ ] **Step 2: Commit**

```bash
git add prompts/interview/deep-dive-agent-system.md
git commit -m "feat(prompts): add deep-dive-agent-system prompt for ReAct function call"
```

---

### Task 4: Add `callWithTools()` to `AiGateway`

**Files:**
- Modify: `backend/src/main/java/com/interviewassistant/service/AiGateway.java`

- [ ] **Step 1: Add imports and `callWithTools` method**

Add these imports:

```java
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallback;
```

Add method after `generateText(String, String)` (after line 95):

```java
public ChatResponse callWithTools(List<Message> messages, ToolCallback... toolCallbacks) {
    OpenAiChatOptions options = OpenAiChatOptions.builder()
            .model(aiConfig.getCurrentModel())
            .temperature(0.7)
            .toolCallbacks(toolCallbacks)
            .internalToolExecutionEnabled(false)
            .build();
    Prompt prompt = new Prompt(messages, options);
    return callWithTimeout(() -> aiConfig.getCurrentChatModel().call(prompt), "工具调用 AI 生成");
}
```

- [ ] **Step 2: Run compilation to verify**

Run: `cd backend && ./mvnw compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/interviewassistant/service/AiGateway.java
git commit -m "feat(gateway): add callWithTools for non-streaming tool-call requests"
```

---

### Task 5: Rewrite `DeepDiveAgent` with ReAct loop

**Files:**
- Modify: `backend/src/main/java/com/interviewassistant/agent/DeepDiveAgent.java`

This is the core change. The agent now:
1. Builds the user prompt from context (reusing `interviewAiService.buildDeepDivePrompt()`)
2. Runs a manual ReAct loop: call LLM with tools → check for tool calls → execute tools → send `agent_step` SSE → loop
3. After loop ends, streams the final answer via `aiGateway.streamText()`

- [ ] **Step 1: Rewrite `DeepDiveAgent.java`**

```java
package com.interviewassistant.agent;

import com.interviewassistant.common.SseUtils;
import com.interviewassistant.dto.interview.ChatMessage;
import com.interviewassistant.dto.interview.DeepDiveContextType;
import com.interviewassistant.dto.knowledge.NoteItem;
import com.interviewassistant.service.AiGateway;
import com.interviewassistant.service.InterviewAiService;
import com.interviewassistant.service.ObsidianService;
import com.interviewassistant.service.PromptService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DeepDiveAgent {

    private static final int MAX_TOOL_ROUNDS = 3;

    private final AiGateway aiGateway;
    private final PromptService promptService;
    private final ObsidianService obsidianService;
    private final Executor executor;
    private final InterviewAiService interviewAiService;
    private final KnowledgeTools knowledgeTools;
    private final ToolCallback[] toolCallbacks;
    private final ObjectMapper objectMapper;

    public DeepDiveAgent(AiGateway aiGateway,
                         PromptService promptService,
                         ObsidianService obsidianService,
                         @Qualifier("sseTaskExecutor") Executor executor,
                         InterviewAiService interviewAiService,
                         KnowledgeTools knowledgeTools,
                         ObjectMapper objectMapper) {
        this.aiGateway = aiGateway;
        this.promptService = promptService;
        this.obsidianService = obsidianService;
        this.executor = executor;
        this.interviewAiService = interviewAiService;
        this.knowledgeTools = knowledgeTools;
        this.objectMapper = objectMapper;
        this.toolCallbacks = MethodToolCallbackProvider.builder()
                .toolObjects(knowledgeTools)
                .build()
                .getToolCallbacks()
                .toArray(new ToolCallback[0]);
    }

    public SseEmitter execute(String question, List<String> expectedKeywords,
                               DeepDiveContextType contextType, String contextContent,
                               List<ChatMessage> messages) {
        SseEmitter emitter = SseUtils.createShortEmitter();
        executor.execute(() -> {
            try {
                String userPrompt = interviewAiService.buildDeepDivePrompt(
                        question, expectedKeywords, contextType, contextContent, messages);

                String systemPrompt = promptService.load("interview/deep-dive-agent-system.md");

                List<org.springframework.ai.chat.messages.Message> chatMessages = new ArrayList<>();
                chatMessages.add(new SystemMessage(systemPrompt));
                chatMessages.add(new UserMessage(userPrompt));

                String finalPrompt = runReActLoop(emitter, chatMessages);

                aiGateway.streamText(emitter, systemPrompt, finalPrompt,
                        "深度追问生成失败", "启动深度追问失败");
            } catch (Exception e) {
                log.error("DeepDiveAgent execution failed", e);
                SseUtils.sendError(emitter, "AGENT_ERROR", "深度追问智能检索失败，请重试");
            }
        });
        return emitter;
    }

    String runReActLoop(SseEmitter emitter,
                        List<org.springframework.ai.chat.messages.Message> chatMessages) {
        StringBuilder toolResults = new StringBuilder();
        for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {
            ChatResponse response = aiGateway.callWithTools(chatMessages, toolCallbacks);

            if (response == null || response.getResult() == null
                    || response.getResult().getOutput() == null) {
                log.warn("Empty response from LLM at round {}", round);
                break;
            }

            AssistantMessage assistant = response.getResult().getOutput();
            if (!assistant.hasToolCalls()) {
                break;
            }

            chatMessages.add(assistant);

            List<ToolResponseMessage.ToolResponse> toolResponses = new ArrayList<>();
            for (AssistantMessage.ToolCall toolCall : assistant.getToolCalls()) {
                log.info("Agent tool call round={}: name={} args={}", round, toolCall.name(), toolCall.arguments());

                String result = executeToolCall(toolCall);
                toolResponses.add(new ToolResponseMessage.ToolResponse(
                        toolCall.id(), toolCall.name(), result));

                if ("searchNotes".equals(toolCall.name())) {
                    sendAgentStepFromResult(emitter, toolCall.arguments(), result);
                }

                toolResults.append("\n\n【知识库检索结果（工具调用 ").append(round + 1).append("）】\n")
                        .append(result);
            }

            ToolResponseMessage toolResponseMessage = ToolResponseMessage.builder()
                    .responses(toolResponses)
                    .build();
            chatMessages.add(toolResponseMessage);
        }

        String originalUserPrompt = ((UserMessage) chatMessages.get(1)).getText();
        if (toolResults.isEmpty()) {
            return originalUserPrompt;
        }
        return originalUserPrompt + toolResults;
    }

    private String executeToolCall(AssistantMessage.ToolCall toolCall) {
        for (ToolCallback callback : toolCallbacks) {
            if (callback.getToolDefinition().name().equals(toolCall.name())) {
                try {
                    return callback.call(toolCall.arguments());
                } catch (Exception e) {
                    log.warn("Tool execution failed for {}: {}", toolCall.name(), e.getMessage());
                    return "工具执行失败：" + e.getMessage() + "。请直接基于现有信息回答。";
                }
            }
        }
        return "未知工具：" + toolCall.name();
    }

    private void sendAgentStepFromResult(SseEmitter emitter, String arguments, String result) {
        try {
            String keyword = "";
            JsonNode args = objectMapper.readTree(arguments);
            if (args.has("keyword")) {
                keyword = args.get("keyword").asText();
            }
            List<String> noteTitles = extractNoteTitles(result);
            if (!keyword.isEmpty() && !noteTitles.isEmpty()) {
                SseUtils.sendAgentStep(emitter, keyword, noteTitles);
            }
        } catch (Exception e) {
            log.debug("Failed to parse tool call arguments for agent_step: {}", e.getMessage());
        }
    }

    List<String> extractNoteTitles(String searchResult) {
        List<String> titles = new ArrayList<>();
        for (String line : searchResult.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("- ")) {
                String rest = trimmed.substring(2);
                int bracketIdx = rest.indexOf('[');
                int parenIdx = rest.indexOf('(');
                int endIdx = rest.length();
                if (bracketIdx > 0) endIdx = Math.min(endIdx, bracketIdx);
                if (parenIdx > 0) endIdx = Math.min(endIdx, parenIdx);
                titles.add(rest.substring(0, endIdx).trim());
            }
        }
        return titles;
    }
}
```

- [ ] **Step 2: Run compilation to verify**

Run: `cd backend && ./mvnw compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/interviewassistant/agent/DeepDiveAgent.java
git commit -m "feat(agent): rewrite DeepDiveAgent with ReAct loop using native function call"
```

---

### Task 6: Delete obsolete files

**Files:**
- Delete: `backend/src/main/java/com/interviewassistant/agent/AgentDecision.java`
- Delete: `backend/prompts/interview/deep-dive-agent-decide.md`

- [ ] **Step 1: Delete the files**

```bash
rm src/main/java/com/interviewassistant/agent/AgentDecision.java
rm prompts/interview/deep-dive-agent-decide.md
```

- [ ] **Step 2: Run compilation to verify nothing references them**

Run: `cd backend && ./mvnw compile -q`
Expected: BUILD SUCCESS (no compile errors from dangling imports)

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "chore: remove AgentDecision DTO and decision prompt (replaced by function call)"
```

---

### Task 7: Rewrite `DeepDiveAgentTest`

**Files:**
- Rewrite: `backend/src/test/java/com/interviewassistant/agent/DeepDiveAgentTest.java`

The tests need to change because:
- No more `AgentDecision` — the LLM decides via function call
- `decide()` method is gone
- New methods to test: `runReActLoop()`, `executeToolCall()`, `extractNoteTitles()`
- `aiGateway.generateJson()` is no longer called by DeepDiveAgent; `aiGateway.callWithTools()` is used instead
- `KnowledgeTools` and `ObjectMapper` are new constructor dependencies

- [ ] **Step 1: Rewrite the test file**

```java
package com.interviewassistant.agent;

import com.interviewassistant.common.SseUtils;
import com.interviewassistant.dto.interview.ChatMessage;
import com.interviewassistant.dto.interview.ChatRole;
import com.interviewassistant.dto.interview.DeepDiveContextType;
import com.interviewassistant.dto.knowledge.NoteItem;
import com.interviewassistant.service.AiGateway;
import com.interviewassistant.service.InterviewAiService;
import com.interviewassistant.service.ObsidianService;
import com.interviewassistant.service.PromptService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeepDiveAgentTest {

    @Mock private AiGateway aiGateway;
    @Mock private PromptService promptService;
    @Mock private ObsidianService obsidianService;
    @Mock private InterviewAiService interviewAiService;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private KnowledgeTools knowledgeTools;
    private DeepDiveAgent agent;

    @BeforeEach
    void setUp() {
        knowledgeTools = new KnowledgeTools(obsidianService);
        agent = new DeepDiveAgent(aiGateway, promptService, obsidianService, executor,
                interviewAiService, knowledgeTools, objectMapper);
    }

    @Test
    void extractNoteTitles_parsesSearchResult() {
        String result = "找到 2 条相关笔记：\n- B+树索引详解 [索引] (btree.md)\n- 红黑树对比 [树结构] (rbtree.md)\n";
        List<String> titles = agent.extractNoteTitles(result);
        assertEquals(List.of("B+树索引详解", "红黑树对比"), titles);
    }

    @Test
    void extractNoteTitles_returnsEmpty_whenNoMatches() {
        List<String> titles = agent.extractNoteTitles("未找到相关笔记。");
        assertTrue(titles.isEmpty());
    }

    @Test
    void runReActLoop_noToolCalls_returnsOriginalPrompt() {
        SseEmitter emitter = SseUtils.createShortEmitter();
        var chatMessages = new java.util.ArrayList<org.springframework.ai.chat.messages.Message>();
        chatMessages.add(new SystemMessage("system"));
        chatMessages.add(new UserMessage("user prompt"));

        AssistantMessage assistantNoTools = new AssistantMessage("这是回答");
        ChatResponse response = ChatResponse.builder()
                .generations(List.of(new Generation(assistantNoTools)))
                .build();
        when(aiGateway.callWithTools(anyList(), any(ToolCallback[].class))).thenReturn(response);

        String result = agent.runReActLoop(emitter, chatMessages);

        assertEquals("user prompt", result);
    }

    @Test
    void runReActLoop_withToolCall_appendsResults() {
        SseEmitter emitter = SseUtils.createShortEmitter();
        var chatMessages = new java.util.ArrayList<org.springframework.ai.chat.messages.Message>();
        chatMessages.add(new SystemMessage("system"));
        chatMessages.add(new UserMessage("user prompt"));

        // Round 1: LLM requests tool call
        AssistantMessage.ToolCall toolCall = new AssistantMessage.ToolCall(
                "call_1", "function", "searchNotes", "{\"keyword\":\"B+树\"}");
        AssistantMessage assistantWithTool = AssistantMessage.builder()
                .text("")
                .toolCalls(List.of(toolCall))
                .build();
        ChatResponse round1Response = ChatResponse.builder()
                .generations(List.of(new Generation(assistantWithTool)))
                .build();

        // Round 2: LLM responds with text (no more tool calls)
        AssistantMessage assistantFinal = new AssistantMessage("B+树是一种平衡树...");
        ChatResponse round2Response = ChatResponse.builder()
                .generations(List.of(new Generation(assistantFinal)))
                .build();

        when(aiGateway.callWithTools(anyList(), any(ToolCallback[].class)))
                .thenReturn(round1Response, round2Response);
        when(obsidianService.isVaultConfigured()).thenReturn(true);
        NoteItem note = new NoteItem("btree.md", "B+树索引详解", "BACKEND", List.of("索引"), "2026-01-01", "btree.md");
        when(obsidianService.searchNotes("B+树")).thenReturn(List.of(note));

        String result = agent.runReActLoop(emitter, chatMessages);

        assertTrue(result.contains("知识库检索结果"));
        assertTrue(result.contains("B+树索引详解"));
        verify(obsidianService).searchNotes("B+树");
    }

    @Test
    void runReActLoop_stopsAfterMaxRounds() {
        SseEmitter emitter = SseUtils.createShortEmitter();
        var chatMessages = new java.util.ArrayList<org.springframework.ai.chat.messages.Message>();
        chatMessages.add(new SystemMessage("system"));
        chatMessages.add(new UserMessage("user prompt"));

        // LLM keeps requesting tool calls
        AssistantMessage.ToolCall toolCall = new AssistantMessage.ToolCall(
                "call_1", "function", "searchNotes", "{\"keyword\":\"test\"}");
        AssistantMessage assistantWithTool = AssistantMessage.builder()
                .text("")
                .toolCalls(List.of(toolCall))
                .build();
        ChatResponse alwaysToolCall = ChatResponse.builder()
                .generations(List.of(new Generation(assistantWithTool)))
                .build();

        when(aiGateway.callWithTools(anyList(), any(ToolCallback[].class)))
                .thenReturn(alwaysToolCall);
        when(obsidianService.isVaultConfigured()).thenReturn(false);

        String result = agent.runReActLoop(emitter, chatMessages);

        // Should stop after MAX_TOOL_ROUNDS (3) iterations
        verify(aiGateway, times(3)).callWithTools(anyList(), any(ToolCallback[].class));
        assertTrue(result.contains("工具调用 1"));
        assertTrue(result.contains("工具调用 2"));
        assertTrue(result.contains("工具调用 3"));
    }

    @Test
    void execute_answerDirectly_streamsDirectly() throws Exception {
        when(interviewAiService.buildDeepDivePrompt(anyString(), any(), any(), anyString(), any()))
                .thenReturn("built-prompt");
        when(promptService.load("interview/deep-dive-agent-system.md")).thenReturn("agent-system");

        // No tool calls
        AssistantMessage assistantNoTools = new AssistantMessage("回答");
        ChatResponse response = ChatResponse.builder()
                .generations(List.of(new Generation(assistantNoTools)))
                .build();
        when(aiGateway.callWithTools(anyList(), any(ToolCallback[].class))).thenReturn(response);

        ChatMessage msg = new ChatMessage();
        msg.setRole(ChatRole.USER);
        msg.setContent("追问");

        SseEmitter emitter = agent.execute("Q", List.of(), DeepDiveContextType.RECOMMENDED_ANSWER, "ctx", List.of(msg));

        assertNotNull(emitter);
        Thread.sleep(300);
        verify(obsidianService, never()).searchNotes(anyString());
        verify(aiGateway).streamText(any(SseEmitter.class), eq("agent-system"), eq("built-prompt"), anyString(), anyString());
    }

    @Test
    void execute_searchNotes_searchesAndStreams() throws Exception {
        when(interviewAiService.buildDeepDivePrompt(anyString(), any(), any(), anyString(), any()))
                .thenReturn("built-prompt");
        when(promptService.load("interview/deep-dive-agent-system.md")).thenReturn("agent-system");

        AssistantMessage.ToolCall toolCall = new AssistantMessage.ToolCall(
                "call_1", "function", "searchNotes", "{\"keyword\":\"B+树\"}");
        AssistantMessage assistantWithTool = AssistantMessage.builder()
                .text("")
                .toolCalls(List.of(toolCall))
                .build();
        ChatResponse round1 = ChatResponse.builder()
                .generations(List.of(new Generation(assistantWithTool)))
                .build();

        AssistantMessage assistantFinal = new AssistantMessage("回答");
        ChatResponse round2 = ChatResponse.builder()
                .generations(List.of(new Generation(assistantFinal)))
                .build();

        when(aiGateway.callWithTools(anyList(), any(ToolCallback[].class)))
                .thenReturn(round1, round2);
        when(obsidianService.isVaultConfigured()).thenReturn(true);
        NoteItem note = new NoteItem("btree.md", "B+树索引详解", "BACKEND", List.of("索引"), "2026-01-01", "btree.md");
        when(obsidianService.searchNotes("B+树")).thenReturn(List.of(note));

        ChatMessage msg = new ChatMessage();
        msg.setRole(ChatRole.USER);
        msg.setContent("B+树怎么实现范围查询？");

        SseEmitter emitter = agent.execute("Q", List.of(), DeepDiveContextType.RECOMMENDED_ANSWER, "ctx", List.of(msg));

        assertNotNull(emitter);
        Thread.sleep(300);
        verify(obsidianService).searchNotes("B+树");
        verify(aiGateway).streamText(any(SseEmitter.class), eq("agent-system"), contains("B+树索引详解"), anyString(), anyString());
    }
}
```

- [ ] **Step 2: Run tests to verify they pass**

Run: `cd backend && ./mvnw test -pl . -Dtest=DeepDiveAgentTest -q`
Expected: All 7 tests PASS

- [ ] **Step 3: Commit**

```bash
git add src/test/java/com/interviewassistant/agent/DeepDiveAgentTest.java
git commit -m "test(agent): rewrite DeepDiveAgentTest for ReAct function call loop"
```

---

### Task 8: Run full test suite and verify

- [ ] **Step 1: Run all backend tests**

Run: `cd backend && ./mvnw test -q`
Expected: BUILD SUCCESS, all tests pass

- [ ] **Step 2: Start the backend and do a smoke test**

Run: `cd backend && mvn spring-boot:run`
Then hit any endpoint (e.g., `curl http://localhost:8080/api/settings/model`) to verify the app starts without errors.

- [ ] **Step 3: Final commit if any fixes needed**

```bash
git add -A
git commit -m "fix: address test failures from function call migration"
```
