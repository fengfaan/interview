# DeepDive ReAct Agent Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a ReAct Agent layer to the deep-dive feature so the model can decide whether to search the Obsidian knowledge base before answering, returning an `agent_step` SSE event to the frontend when it does.

**Architecture:** A new `DeepDiveAgent` service replaces the direct `aiGateway.streamText()` call in `InterviewStreamService.streamDeepDive()`. The agent first asks the LLM to choose between `answer_directly` and `search_notes` (structured JSON). If `search_notes` is chosen, it calls `ObsidianService.searchNotes()`, sends an `agent_step` SSE event with the results, then streams the final answer with the knowledge context injected. Maximum 1 tool call per request (MVP).

**Tech Stack:** Java 17, Spring Boot 3.3, Spring AI 1.1, Lombok, JUnit 5 + Mockito, SSE (SseEmitter), Vue 3 + TypeScript

---

## File Structure

### New Files

| File | Responsibility |
|------|---------------|
| `backend/src/main/java/com/interviewassistant/agent/DeepDiveAgent.java` | ReAct agent: decide → act → observe → stream answer |
| `backend/src/main/java/com/interviewassistant/agent/AgentDecision.java` | DTO for the agent's structured decision (action, keyword, reason) |
| `backend/src/test/java/com/interviewassistant/agent/DeepDiveAgentTest.java` | Unit tests for agent decision parsing and execution flow |
| `backend/prompts/interview/deep-dive-agent-decide.md` | Prompt template asking the LLM to choose an action |

### Modified Files

| File | Changes |
|------|---------|
| `backend/src/main/java/com/interviewassistant/service/InterviewStreamService.java` | Inject `DeepDiveAgent`, call it instead of `aiGateway.streamText()` in `streamDeepDive()` |
| `backend/src/main/java/com/interviewassistant/common/SseUtils.java` | Add `sendAgentStep()` method for `agent_step` event type |
| `frontend/src/api/deepDiveApi.ts` | Switch from `streamPost` to `streamPostEvents`, handle `agent_step` events |
| `frontend/src/stores/deepDiveStore.ts` | Add `agentStepInfo` ref, parse `agent_step` events |
| `frontend/src/types/deepDive.ts` | Add `AgentStepInfo` type |
| `frontend/src/views/MockInterviewRoomView.vue` | Display agent step info in the deep-dive drawer |

---

## Task 1: AgentDecision DTO

**Files:**
- Create: `backend/src/main/java/com/interviewassistant/agent/AgentDecision.java`
- Test: N/A (pure DTO, tested indirectly via agent tests)

- [ ] **Step 1: Create the AgentDecision record**

```java
package com.interviewassistant.agent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Structured output from the agent's decision call.
 * action: "answer_directly" or "search_notes"
 * keyword: search keyword (only when action=search_notes)
 * reason: why the agent chose this action (for logging/observability)
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AgentDecision {
    private String action;
    private String keyword;
    private String reason;

    public boolean wantsSearch() {
        return "search_notes".equals(action) && keyword != null && !keyword.isBlank();
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/java/com/interviewassistant/agent/AgentDecision.java
git commit -m "feat(agent): add AgentDecision DTO for ReAct agent decisions"
```

---

## Task 2: Agent Decision Prompt Template

**Files:**
- Create: `backend/prompts/interview/deep-dive-agent-decide.md`

- [ ] **Step 1: Create the decision prompt**

This prompt asks the LLM to decide whether to search the knowledge base. The LLM returns structured JSON.

```markdown
你是一个决策助手。你需要判断回答候选人追问是否需要检索知识库补充资料。

原始面试题：{{question}}
考察要点：{{expectedKeywords}}
候选人最新追问：{{latestQuestion}}
上下文摘要（前200字）：{{contextPreview}}

判断规则：
1. 如果追问涉及当前上下文中未充分展开的知识点（如具体技术细节、对比、底层原理），且知识库可能收录了相关笔记 → action 为 "search_notes"，keyword 为 2-5 个字的搜索关键词
2. 如果追问可以基于当前上下文充分回答，或属于泛泛的讨论 → action 为 "answer_directly"
3. 不要搜索与原始面试题无关的话题

请直接返回 JSON，不要加 markdown 代码块：
```

Note: The prompt ends without a closing triple-backtick code fence — the template literally ends at the JSON instruction, prompting the LLM to output raw JSON.

- [ ] **Step 2: Commit**

```bash
git add backend/prompts/interview/deep-dive-agent-decide.md
git commit -m "feat(agent): add agent decision prompt template"
```

---

## Task 3: DeepDiveAgent Service

**Files:**
- Create: `backend/src/main/java/com/interviewassistant/agent/DeepDiveAgent.java`
- Create: `backend/src/test/java/com/interviewassistant/agent/DeepDiveAgentTest.java`

### Part A: Write failing tests first

- [ ] **Step 1: Write DeepDiveAgentTest**

```java
package com.interviewassistant.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewassistant.dto.knowledge.NoteItem;
import com.interviewassistant.service.AiGateway;
import com.interviewassistant.service.ObsidianService;
import com.interviewassistant.service.PromptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
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
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private DeepDiveAgent agent;

    @BeforeEach
    void setUp() {
        agent = new DeepDiveAgent(aiGateway, promptService, obsidianService, executor, objectMapper);
    }

    @Test
    void decide_returnsAgentDecision_whenValidJson() {
        AgentDecision decision = new AgentDecision();
        decision.setAction("search_notes");
        decision.setKeyword("B+树");
        decision.setReason("用户追问B+树底层结构");

        when(aiGateway.generateJson(eq("你是一个决策助手。"), anyString(), eq(AgentDecision.class)))
                .thenReturn(new AiGateway.JsonResult<>(decision, "test-model"));

        AgentDecision result = agent.decide("什么是B+树", List.of("索引"), "B+树叶子节点", "上下文摘要...");
        assertTrue(result.wantsSearch());
        assertEquals("B+树", result.getKeyword());
    }

    @Test
    void decide_answerDirectly_doesNotWantSearch() {
        AgentDecision decision = new AgentDecision();
        decision.setAction("answer_directly");
        decision.setReason("上下文已充分");

        when(aiGateway.generateJson(anyString(), anyString(), eq(AgentDecision.class)))
                .thenReturn(new AiGateway.JsonResult<>(decision, "test-model"));

        AgentDecision result = agent.decide("Q", List.of(), "追问", "上下文");
        assertFalse(result.wantsSearch());
    }

    @Test
    void decide_fallsBackToAnswerDirectly_whenJsonParseFails() {
        when(aiGateway.generateJson(anyString(), anyString(), eq(AgentDecision.class)))
                .thenThrow(new RuntimeException("JSON parse failed"));

        AgentDecision result = agent.decide("Q", List.of(), "追问", "上下文");
        assertFalse(result.wantsSearch());
        assertEquals("answer_directly", result.getAction());
    }

    @Test
    void searchKnowledge_returnsNoteTitles() {
        NoteItem note = new NoteItem("test.md", "B+树索引详解", "BACKEND", List.of("索引"), "2026-01-01", "test.md");
        when(obsidianService.searchNotes("B+树")).thenReturn(List.of(note));

        List<NoteItem> results = agent.searchKnowledge("B+树");
        assertEquals(1, results.size());
        assertEquals("B+树索引详解", results.get(0).getTitle());
    }

    @Test
    void searchKnowledge_returnsEmptyList_onException() {
        when(obsidianService.searchNotes(anyString())).thenThrow(new RuntimeException("vault not configured"));

        List<NoteItem> results = agent.searchKnowledge("B+树");
        assertTrue(results.isEmpty());
    }

    @Test
    void buildSearchContext_formatsNotesCorrectly() {
        NoteItem note = new NoteItem("btree.md", "B+树索引详解", "BACKEND", List.of("索引"), "2026-01-01", "btree.md");
        String context = agent.buildSearchContext(List.of(note));
        assertTrue(context.contains("B+树索引详解"));
        assertTrue(context.contains("btree.md"));
    }

    @Test
    void buildSearchContext_returnsEmptyString_whenNoNotes() {
        String context = agent.buildSearchContext(List.of());
        assertEquals("", context);
    }

    @Test
    void execute_answerDirectly_streamsDirectly() throws Exception {
        AgentDecision decision = new AgentDecision();
        decision.setAction("answer_directly");

        when(aiGateway.generateJson(anyString(), anyString(), eq(AgentDecision.class)))
                .thenReturn(new AiGateway.JsonResult<>(decision, "test-model"));
        when(promptService.load("interview/system.md")).thenReturn("system-prompt");

        SseEmitter emitter = agent.execute("Q", List.of(), "RECOMMENDED_ANSWER", "ctx", List.of());

        assertNotNull(emitter);
        // The emitter is returned immediately; streaming happens asynchronously
        // We verify aiGateway.streamText is called (not searchNotes)
        Thread.sleep(200);
        verify(obsidianService, never()).searchNotes(anyString());
    }

    @Test
    void execute_searchNotes_searchesAndStreams() throws Exception {
        AgentDecision decision = new AgentDecision();
        decision.setAction("search_notes");
        decision.setKeyword("B+树");

        NoteItem note = new NoteItem("btree.md", "B+树索引详解", "BACKEND", List.of("索引"), "2026-01-01", "btree.md");

        when(aiGateway.generateJson(anyString(), anyString(), eq(AgentDecision.class)))
                .thenReturn(new AiGateway.JsonResult<>(decision, "test-model"));
        when(obsidianService.searchNotes("B+树")).thenReturn(List.of(note));
        when(promptService.load("interview/system.md")).thenReturn("system-prompt");

        SseEmitter emitter = agent.execute("Q", List.of(), "RECOMMENDED_ANSWER", "ctx", List.of());

        assertNotNull(emitter);
        Thread.sleep(200);
        verify(obsidianService).searchNotes("B+树");
        verify(aiGateway).streamText(any(SseEmitter.class), eq("system-prompt"), contains("B+树索引详解"), anyString(), anyString());
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd backend && ./mvnw test -Dtest="DeepDiveAgentTest" -Dsurefire.useFile=false`
Expected: Compilation error — `DeepDiveAgent` does not exist yet.

### Part B: Implement DeepDiveAgent

- [ ] **Step 3: Implement DeepDiveAgent**

```java
package com.interviewassistant.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewassistant.common.SseUtils;
import com.interviewassistant.dto.interview.ChatMessage;
import com.interviewassistant.dto.interview.DeepDiveContextType;
import com.interviewassistant.dto.knowledge.NoteItem;
import com.interviewassistant.service.AiGateway;
import com.interviewassistant.service.InterviewAiService;
import com.interviewassistant.service.ObsidianService;
import com.interviewassistant.service.PromptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DeepDiveAgent {

    private final AiGateway aiGateway;
    private final PromptService promptService;
    private final ObsidianService obsidianService;
    private final Executor executor;
    private final ObjectMapper objectMapper;
    private final InterviewAiService interviewAiService;

    public DeepDiveAgent(AiGateway aiGateway,
                         PromptService promptService,
                         ObsidianService obsidianService,
                         @Qualifier("sseTaskExecutor") Executor executor,
                         ObjectMapper objectMapper,
                         InterviewAiService interviewAiService) {
        this.aiGateway = aiGateway;
        this.promptService = promptService;
        this.obsidianService = obsidianService;
        this.executor = executor;
        this.objectMapper = objectMapper;
        this.interviewAiService = interviewAiService;
    }

    /**
     * Execute the ReAct agent loop for a deep-dive request.
     * 1. Build prompt (with compaction)
     * 2. Decide: answer_directly or search_notes
     * 3. If search_notes: search, send agent_step, inject results into prompt
     * 4. Stream final answer
     */
    public SseEmitter execute(String question, List<String> expectedKeywords,
                               DeepDiveContextType contextType, String contextContent,
                               List<ChatMessage> messages) {
        SseEmitter emitter = SseUtils.createShortEmitter();
        executor.execute(() -> {
            try {
                String prompt = interviewAiService.buildDeepDivePrompt(
                        question, expectedKeywords, contextType, contextContent, messages);

                String contextPreview = contextContent != null && contextContent.length() > 200
                        ? contextContent.substring(0, 200)
                        : (contextContent != null ? contextContent : "");
                String latestQuestion = messages != null && !messages.isEmpty()
                        ? messages.get(messages.size() - 1).getContent()
                        : question;

                AgentDecision decision = decide(question, expectedKeywords, latestQuestion, contextPreview);

                String finalPrompt = prompt;
                if (decision.wantsSearch()) {
                    List<NoteItem> notes = searchKnowledge(decision.getKeyword());
                    if (!notes.isEmpty()) {
                        String searchContext = buildSearchContext(notes);
                        SseUtils.sendAgentStep(emitter, decision.getKeyword(),
                                notes.stream().map(NoteItem::getTitle).collect(Collectors.toList()));

                        finalPrompt = prompt + "\n\n【知识库检索结果（关键词：" + decision.getKeyword() + "）】\n"
                                + searchContext
                                + "\n请在回答中适当引用知识库中的相关内容，帮助候选人更深入理解。";
                    }
                    log.info("Agent decision: search_notes keyword={} notes={}", decision.getKeyword(), notes.size());
                } else {
                    log.info("Agent decision: answer_directly reason={}", decision.getReason());
                }

                String systemPrompt = promptService.load("interview/system.md");
                aiGateway.streamText(emitter, systemPrompt, finalPrompt,
                        "深度追问生成失败", "启动深度追问失败");
            } catch (Exception e) {
                log.error("DeepDiveAgent execution failed", e);
                SseUtils.sendError(emitter, "AGENT_ERROR", "深度追问智能检索失败，请重试");
            }
        });
        return emitter;
    }

    /**
     * Ask the LLM to decide whether to search the knowledge base.
     * Falls back to answer_directly on any failure.
     */
    AgentDecision decide(String question, List<String> expectedKeywords,
                         String latestQuestion, String contextPreview) {
        try {
            String userPrompt = promptService.render("interview/deep-dive-agent-decide.md", Map.of(
                    "question", question,
                    "expectedKeywords", expectedKeywords != null ? expectedKeywords : List.of(),
                    "latestQuestion", latestQuestion != null ? latestQuestion : "",
                    "contextPreview", contextPreview != null ? contextPreview : ""
            ));
            String systemPrompt = "你是一个决策助手。";

            AiGateway.JsonResult<AgentDecision> result =
                    aiGateway.generateJson(systemPrompt, userPrompt, AgentDecision.class);
            return result.value();
        } catch (Exception e) {
            log.warn("Agent decision failed, falling back to answer_directly: {}", e.getMessage());
            AgentDecision fallback = new AgentDecision();
            fallback.setAction("answer_directly");
            fallback.setReason("决策失败，直接回答");
            return fallback;
        }
    }

    /**
     * Search the Obsidian knowledge base. Returns empty list on any failure.
     */
    List<NoteItem> searchKnowledge(String keyword) {
        try {
            if (!obsidianService.isVaultConfigured()) {
                return List.of();
            }
            return obsidianService.searchNotes(keyword);
        } catch (Exception e) {
            log.warn("Knowledge search failed for keyword '{}': {}", keyword, e.getMessage());
            return List.of();
        }
    }

    /**
     * Format search results into a text block for injection into the prompt.
     */
    String buildSearchContext(List<NoteItem> notes) {
        if (notes == null || notes.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (NoteItem note : notes) {
            sb.append("- ").append(note.getTitle());
            if (note.getTags() != null && !note.getTags().isEmpty()) {
                sb.append(" [").append(String.join(", ", note.getTags())).append("]");
            }
            sb.append(" (").append(note.getId()).append(")\n");
        }
        return sb.toString();
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd backend && ./mvnw test -Dtest="DeepDiveAgentTest" -Dsurefire.useFile=false`
Expected: All tests PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/interviewassistant/agent/ \
        backend/src/test/java/com/interviewassistant/agent/
git commit -m "feat(agent): implement DeepDiveAgent with decide/search/stream flow"
```

---

## Task 4: SseUtils.sendAgentStep

**Files:**
- Modify: `backend/src/main/java/com/interviewassistant/common/SseUtils.java`

- [ ] **Step 1: Add sendAgentStep method to SseUtils**

Add this method to `SseUtils.java` after the existing `sendAiError` method (around line 70):

```java
public static void sendAgentStep(SseEmitter emitter, String keyword, List<String> noteTitles) {
    try {
        Map<String, Object> payload = Map.of(
                "keyword", keyword != null ? keyword : "",
                "notes", noteTitles != null ? noteTitles : List.of()
        );
        emitter.send(SseEmitter.event()
                .name("agent_step")
                .data(OBJECT_MAPPER.writeValueAsString(payload), MediaType.APPLICATION_JSON));
    } catch (Exception e) {
        log.debug("SSE agent_step send failed: {}", AiErrorUtils.compactMessage(e));
    }
}
```

Also add the missing import at the top of the file:
```java
import java.util.List;
```

- [ ] **Step 2: Verify existing tests still pass**

Run: `cd backend && ./mvnw test -Dsurefire.useFile=false`
Expected: All 26 tests PASS (no regression).

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/interviewassistant/common/SseUtils.java
git commit -m "feat(sse): add sendAgentStep for agent_step SSE event"
```

---

## Task 5: Wire DeepDiveAgent into InterviewStreamService

**Files:**
- Modify: `backend/src/main/java/com/interviewassistant/service/InterviewStreamService.java`

- [ ] **Step 1: Add DeepDiveAgent injection and switch streamDeepDive**

Replace the `streamDeepDive` method in `InterviewStreamService.java`. The key change: instead of building the prompt and calling `aiGateway.streamText()` directly, delegate to `DeepDiveAgent.execute()`.

New imports to add:
```java
import com.interviewassistant.agent.DeepDiveAgent;
```

New field and constructor change — add `DeepDiveAgent`:
```java
private final DeepDiveAgent deepDiveAgent;

public InterviewStreamService(InterviewAiService interviewService,
                              PromptService promptService,
                              AiGateway aiGateway,
                              DeepDiveAgent deepDiveAgent,
                              @Qualifier("sseTaskExecutor") Executor executor) {
    this.interviewService = interviewService;
    this.promptService = promptService;
    this.aiGateway = aiGateway;
    this.deepDiveAgent = deepDiveAgent;
    this.executor = executor;
}
```

Replace `streamDeepDive` method:
```java
public SseEmitter streamDeepDive(DeepDiveRequest request) {
    return deepDiveAgent.execute(
            request.getQuestion(), request.getExpectedKeywords(),
            request.getContextType(), request.getContextContent(),
            request.getMessages());
}
```

- [ ] **Step 2: Run all backend tests**

Run: `cd backend && ./mvnw test -Dsurefire.useFile=false`
Expected: All tests PASS. The controller test `InterviewControllerDeepDiveTest` still passes because it mocks `InterviewStreamService` — the internal wiring change is transparent.

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/interviewassistant/service/InterviewStreamService.java
git commit -m "feat(agent): wire DeepDiveAgent into streamDeepDive flow"
```

---

## Task 6: Frontend — Handle agent_step SSE Events

**Files:**
- Modify: `frontend/src/types/deepDive.ts`
- Modify: `frontend/src/api/deepDiveApi.ts`
- Modify: `frontend/src/stores/deepDiveStore.ts`

- [ ] **Step 1: Add AgentStepInfo type to deepDive.ts**

Add to the end of `frontend/src/types/deepDive.ts`:

```typescript
export interface AgentStepInfo {
  keyword: string
  notes: string[]
}
```

- [ ] **Step 2: Update deepDiveApi.ts to use streamPostEvents**

Replace the entire content of `frontend/src/api/deepDiveApi.ts`:

```typescript
import type { DeepDiveRequest } from '../types/deepDive'
import { streamPostEvents } from './streamClient'

export function streamDeepDive(
  request: DeepDiveRequest,
  onChunk: (text: string) => void,
  onError?: (error: string) => void,
  onAgentStep?: (keyword: string, notes: string[]) => void,
): Promise<void> {
  return streamPostEvents(
    '/interview/deep-dive/stream',
    request,
    (event) => {
      if (event.type === 'agent_step') {
        try {
          const parsed = JSON.parse(event.data)
          onAgentStep?.(parsed.keyword ?? '', parsed.notes ?? [])
        } catch {
          // ignore malformed agent_step
        }
        return
      }
      if (event.type === 'progress') return
      onChunk(event.data)
    },
    onError,
  )
}
```

- [ ] **Step 3: Update deepDiveStore.ts to handle agent steps**

In `frontend/src/stores/deepDiveStore.ts`, add an `agentStepInfo` ref and wire the `onAgentStep` callback.

Add import at top:
```typescript
import type { AgentStepInfo } from '../types/deepDive'
```

Add new ref inside the `defineStore` function (after `streamingContent`):
```typescript
const agentStepInfo = ref<AgentStepInfo | null>(null)
```

Update the `api.streamDeepDive` call inside `sendMessage` to pass the `onAgentStep` callback:
```typescript
await api.streamDeepDive(
  {
    question: questionText,
    expectedKeywords,
    contextType: sourceType.value,
    contextContent: contextContent.value,
    messages: messages.value,
  },
  (chunk) => {
    streamingContent.value += chunk
  },
  (err) => {
    streamingContent.value = ''
    messages.value.pop()
    inputText.value = question
    isStreaming.value = false
    console.error('Deep dive streaming error:', err)
  },
  (keyword, notes) => {
    agentStepInfo.value = { keyword, notes }
  },
)
```

In `reset()` function, add:
```typescript
agentStepInfo.value = null
```

Add to the return statement:
```typescript
return {
  isOpen, sourceType, contextContent, messages, inputText,
  isStreaming, streamingContent, agentStepInfo,
  openDeepDive, sendMessage, closeDeepDive, reset,
}
```

- [ ] **Step 4: Verify frontend builds**

Run: `cd frontend && npm run build`
Expected: Build succeeds with no TypeScript errors.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/types/deepDive.ts frontend/src/api/deepDiveApi.ts frontend/src/stores/deepDiveStore.ts
git commit -m "feat(frontend): handle agent_step SSE events in deep dive"
```

---

## Task 7: Frontend — Display Agent Step Info in Deep-Dive Drawer

**Files:**
- Modify: `frontend/src/views/MockInterviewRoomView.vue`

This is the most UI-context-dependent task. The implementer will need to find the deep-dive drawer section and add a small info bar when `agentStepInfo` is non-null.

- [ ] **Step 1: Find the deep-dive drawer in MockInterviewRoomView.vue and add agent step display**

The deep-dive drawer already imports `useDeepDiveStore`. Add `agentStepInfo` to the destructured store. Then, inside the deep-dive message area (where streaming content is shown), add a conditional info bar above the streaming/response content:

Find where `deepDiveStore.streamingContent` or `deepDiveStore.messages` is rendered. Above the assistant's streaming response, add:

```vue
<div v-if="deepDiveStore.agentStepInfo"
     class="mx-4 mb-2 px-3 py-2 bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-lg text-sm text-blue-700 dark:text-blue-300">
  已检索知识库「{{ deepDiveStore.agentStepInfo.keyword }}」：
  {{ deepDiveStore.agentStepInfo.notes.join('、') }}
</div>
```

The exact location depends on the template structure. The implementer should look for the section that renders the assistant's response in the deep-dive drawer and insert this before it.

- [ ] **Step 2: Verify frontend builds**

Run: `cd frontend && npm run build`
Expected: Build succeeds.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/views/MockInterviewRoomView.vue
git commit -m "feat(frontend): display agent step info in deep-dive drawer"
```

---

## Task 8: End-to-End Verification

**Files:** None (verification only)

- [ ] **Step 1: Run all backend tests**

Run: `cd backend && ./mvnw test -Dsurefire.useFile=false`
Expected: All tests PASS (26 existing + DeepDiveAgentTest tests).

- [ ] **Step 2: Run frontend build**

Run: `cd frontend && npm run build`
Expected: Build succeeds.

- [ ] **Step 3: Start backend and frontend dev servers, do a manual smoke test**

Run: `cd backend && mvn spring-boot:run`
Run: `cd frontend && npm run dev`

Manual test:
1. Open the app → go to interview room
2. Generate a question and answer it
3. Open deep-dive on the recommended answer or feedback
4. Ask a follow-up question that would benefit from knowledge base search
5. Verify: either `agent_step` event is received (check browser Network tab SSE) or direct answer works as before
6. Verify: if agent_step is received, the info bar appears showing the keyword and note titles

- [ ] **Step 4: Final commit (if any fixes were needed)**

```bash
git add -A
git commit -m "fix: address e2e verification findings"
```

---

## Task 9: Update OpenSpec

**Files:**
- Modify: `openspec/changes/react-agent-deep-dive/tasks.md`
- Modify: `openspec/changes/react-agent-deep-dive/specs/react-agent/spec.md`

- [ ] **Step 1: Update tasks.md — check off completed items and add new items**

Mark existing items as complete (context compaction was already implemented). Add new items for the agent work:

```markdown
## 4. DeepDiveAgent ReAct 循环

- [x] 4.1 创建 `AgentDecision` DTO（action, keyword, reason）
- [x] 4.2 创建 `deep-dive-agent-decide.md` 决策 prompt 模板
- [x] 4.3 实现 `DeepDiveAgent` 服务（decide → search → stream）
- [x] 4.4 添加 `SseUtils.sendAgentStep()` 支持 `agent_step` 事件
- [x] 4.5 将 `InterviewStreamService.streamDeepDive()` 切换到走 Agent 层
- [x] 4.6 前端处理 `agent_step` SSE 事件并在 UI 显示检索信息
```

- [ ] **Step 2: Update react-agent spec — mark as implemented**

Replace the content of `openspec/changes/react-agent-deep-dive/specs/react-agent/spec.md`:

```markdown
## ADDED Requirements

### Requirement: ReAct Agent 深度追问
系统 SHALL 在深度追问流程中实现 ReAct Agent 循环（最多 1 次工具调用）：
1. Agent 判断是否需要检索知识库（返回结构化 JSON：action=search_notes 或 answer_directly）
2. 若 search_notes：调用 ObsidianService.searchNotes(keyword)，通过 SSE agent_step 事件通知前端检索了哪些笔记
3. 将检索结果注入 prompt 后流式输出最终回答

#### Scenario: Agent 判断需要检索知识库
- **GIVEN** 候选人追问涉及上下文外的知识点
- **WHEN** Agent 决策返回 action=search_notes
- **THEN** 系统搜索 Obsidian 知识库，发送 agent_step SSE 事件，将结果注入最终回答

#### Scenario: Agent 判断直接回答
- **GIVEN** 候选人追问可基于当前上下文充分回答
- **WHEN** Agent 决策返回 action=answer_directly
- **THEN** 系统直接流式输出回答，不触发知识库检索

#### Scenario: 决策失败时优雅降级
- **GIVEN** Agent 决策调用失败（JSON 解析异常、LLM 错误）
- **WHEN** decide() 方法捕获异常
- **THEN** 系统降级为 answer_directly，不中断用户流程
```

- [ ] **Step 3: Commit**

```bash
git add openspec/changes/react-agent-deep-dive/
git commit -m "docs: update OpenSpec tasks and spec for ReAct agent implementation"
```
