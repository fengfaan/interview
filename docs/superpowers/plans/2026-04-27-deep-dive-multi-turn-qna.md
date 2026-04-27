# Deep Dive Multi-Turn Q&A Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a multi-turn Q&A "Deep Dive" feature to the interview practice room, allowing users to ask follow-up questions about recommended answers or feedback commentary.

**Architecture:** Stateless backend — a new SSE streaming endpoint receives the full conversation history each time. Frontend manages state in a new Pinia store and renders a right-side Drawer component with chat bubbles.

**Tech Stack:** Spring Boot (backend SSE), Vue 3 + Pinia + Tailwind CSS (frontend), existing `streamClient.ts` for SSE.

---

## File Structure

### Backend — New files
- `backend/src/main/java/com/interviewassistant/dto/interview/ChatRole.java` — USER/ASSISTANT enum
- `backend/src/main/java/com/interviewassistant/dto/interview/ChatMessage.java` — role + content DTO
- `backend/src/main/java/com/interviewassistant/dto/interview/DeepDiveContextType.java` — RECOMMENDED_ANSWER/FEEDBACK enum
- `backend/src/main/java/com/interviewassistant/dto/interview/DeepDiveRequest.java` — request DTO
- `backend/prompts/interview/deep-dive.md` — prompt template

### Backend — Modified files
- `backend/src/main/java/com/interviewassistant/service/InterviewAiService.java` — add `buildDeepDivePrompt()` method
- `backend/src/main/java/com/interviewassistant/service/InterviewStreamService.java` — add `streamDeepDive()` method
- `backend/src/main/java/com/interviewassistant/controller/InterviewController.java` — add deep-dive endpoint

### Frontend — New files
- `frontend/src/types/deepDive.ts` — TypeScript types
- `frontend/src/api/deepDiveApi.ts` — API call for deep dive SSE
- `frontend/src/stores/deepDiveStore.ts` — Pinia store
- `frontend/src/components/DeepDiveDrawer.vue` — Drawer chat UI component

### Frontend — Modified files
- `frontend/src/views/MockInterviewRoomView.vue` — add "深度追问" buttons + mount DeepDiveDrawer

---

### Task 1: Backend DTOs and Enum

**Files:**
- Create: `backend/src/main/java/com/interviewassistant/dto/interview/ChatRole.java`
- Create: `backend/src/main/java/com/interviewassistant/dto/interview/ChatMessage.java`
- Create: `backend/src/main/java/com/interviewassistant/dto/interview/DeepDiveContextType.java`
- Create: `backend/src/main/java/com/interviewassistant/dto/interview/DeepDiveRequest.java`

- [ ] **Step 1: Create ChatRole enum**

```java
package com.interviewassistant.dto.interview;

public enum ChatRole {
    USER, ASSISTANT
}
```

- [ ] **Step 2: Create ChatMessage DTO**

```java
package com.interviewassistant.dto.interview;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChatMessage {
    @NotNull(message = "role is required")
    private ChatRole role;

    @NotBlank(message = "content is required")
    private String content;
}
```

- [ ] **Step 3: Create DeepDiveContextType enum**

```java
package com.interviewassistant.dto.interview;

public enum DeepDiveContextType {
    RECOMMENDED_ANSWER, FEEDBACK
}
```

- [ ] **Step 4: Create DeepDiveRequest DTO**

```java
package com.interviewassistant.dto.interview;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class DeepDiveRequest {
    @NotBlank(message = "question is required")
    private String question;

    private List<String> expectedKeywords;

    @NotNull(message = "contextType is required")
    private DeepDiveContextType contextType;

    @NotBlank(message = "contextContent is required")
    private String contextContent;

    @NotEmpty(message = "messages must not be empty")
    private List<ChatMessage> messages;
}
```

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/interviewassistant/dto/interview/ChatRole.java \
        backend/src/main/java/com/interviewassistant/dto/interview/ChatMessage.java \
        backend/src/main/java/com/interviewassistant/dto/interview/DeepDiveContextType.java \
        backend/src/main/java/com/interviewassistant/dto/interview/DeepDiveRequest.java
git commit -m "feat(deep-dive): add backend DTOs for multi-turn Q&A"
```

---

### Task 2: Prompt Template

**Files:**
- Create: `backend/prompts/interview/deep-dive.md`

- [ ] **Step 1: Create the deep-dive prompt template**

```markdown
你是一位经验丰富的技术面试教练，正在帮助候选人深入理解面试题中的知识点。

原始面试题：{{question}}
考察要点：{{expectedKeywords}}

候选人正在查看的上下文（{{contextType}}）：
{{contextContent}}

以下是候选人与你的多轮追问历史：
{{history}}

请基于以上完整上下文，回答候选人最新的问题。要求：

1. 用深入浅出的方式讲解，善用类比和实际例子帮助理解
2. 将讲解内容与原始面试题和考察要点关联起来
3. 如果候选人的理解有偏差，温和地纠正
4. 使用 Markdown 格式输出，可以包含代码示例
5. 回答要有针对性，不要泛泛而谈
```

- [ ] **Step 2: Commit**

```bash
git add backend/prompts/interview/deep-dive.md
git commit -m "feat(deep-dive): add prompt template for multi-turn Q&A"
```

---

### Task 3: Backend Service Layer

**Files:**
- Modify: `backend/src/main/java/com/interviewassistant/service/InterviewAiService.java` — add `buildDeepDivePrompt()` method after `buildRecommendedAnswerPrompt()` (around line 92)
- Modify: `backend/src/main/java/com/interviewassistant/service/InterviewStreamService.java` — add `streamDeepDive()` method after `streamRecommendedAnswer()` (around line 59)

- [ ] **Step 1: Add `buildDeepDivePrompt()` to InterviewAiService**

Add this method after `buildRecommendedAnswerPrompt()` in `InterviewAiService.java`:

```java
public String buildDeepDivePrompt(String question, List<String> expectedKeywords,
                                   DeepDiveContextType contextType, String contextContent,
                                   List<ChatMessage> messages) {
    String history = messages.stream()
            .map(m -> (m.getRole() == ChatRole.USER ? "候选人" : "教练") + "：" + m.getContent())
            .collect(Collectors.joining("\n\n"));

    return promptService.render("interview/deep-dive.md", Map.of(
            "question", question,
            "expectedKeywords", expectedKeywords != null ? expectedKeywords : List.of(),
            "contextType", contextType == DeepDiveContextType.RECOMMENDED_ANSWER ? "推荐答案" : "反馈点评",
            "contextContent", contextContent,
            "history", history
    ));
}
```

- [ ] **Step 2: Add `streamDeepDive()` to InterviewStreamService**

Add this method after `streamRecommendedAnswer()` in `InterviewStreamService.java`:

```java
public SseEmitter streamDeepDive(DeepDiveRequest request) {
    SseEmitter emitter = SseUtils.createShortEmitter();
    String prompt = interviewService.buildDeepDivePrompt(
            request.getQuestion(), request.getExpectedKeywords(),
            request.getContextType(), request.getContextContent(),
            request.getMessages());
    executor.execute(() -> aiGateway.streamText(
            emitter,
            promptService.load("interview/system.md"),
            prompt,
            "深度追问生成失败",
            "启动深度追问失败"
    ));
    return emitter;
}
```

Requires adding these imports to `InterviewStreamService.java`:
```java
import com.interviewassistant.dto.interview.ChatMessage;
import com.interviewassistant.dto.interview.DeepDiveContextType;
import com.interviewassistant.dto.interview.DeepDiveRequest;
```

- [ ] **Step 3: Compile to verify**

Run: `cd backend && ./mvnw compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/interviewassistant/service/InterviewAiService.java \
        backend/src/main/java/com/interviewassistant/service/InterviewStreamService.java
git commit -m "feat(deep-dive): add service layer for multi-turn Q&A"
```

---

### Task 4: Backend Controller Endpoint

**Files:**
- Modify: `backend/src/main/java/com/interviewassistant/controller/InterviewController.java` — add endpoint after `streamRecommendedAnswer()` (around line 100)

- [ ] **Step 1: Add deep-dive endpoint to InterviewController**

Add this endpoint after the `streamRecommendedAnswer` method in `InterviewController.java`:

```java
@PostMapping(value = "/deep-dive/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter streamDeepDive(@Valid @RequestBody DeepDiveRequest request) {
    return interviewStreamService.streamDeepDive(request);
}
```

No additional imports needed — `DeepDiveRequest` is already covered by the `dto.interview.*` wildcard import.

- [ ] **Step 2: Compile to verify**

Run: `cd backend && ./mvnw compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/interviewassistant/controller/InterviewController.java
git commit -m "feat(deep-dive): add POST /api/interview/deep-dive/stream endpoint"
```

---

### Task 5: Frontend Types and API

**Files:**
- Create: `frontend/src/types/deepDive.ts`
- Create: `frontend/src/api/deepDiveApi.ts`

- [ ] **Step 1: Create TypeScript types**

Create `frontend/src/types/deepDive.ts`:

```typescript
export type DeepDiveContextType = 'RECOMMENDED_ANSWER' | 'FEEDBACK'

export type ChatRole = 'USER' | 'ASSISTANT'

export interface ChatMessage {
  role: ChatRole
  content: string
}

export interface DeepDiveRequest {
  question: string
  expectedKeywords: string[]
  contextType: DeepDiveContextType
  contextContent: string
  messages: ChatMessage[]
}
```

- [ ] **Step 2: Create API function**

Create `frontend/src/api/deepDiveApi.ts`:

```typescript
import type { DeepDiveRequest } from '../types/deepDive'
import { streamPost } from './streamClient'

export function streamDeepDive(
  request: DeepDiveRequest,
  onChunk: (text: string) => void,
  onError?: (error: string) => void,
): Promise<void> {
  return streamPost('/interview/deep-dive/stream', request, onChunk, onError)
}
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/types/deepDive.ts frontend/src/api/deepDiveApi.ts
git commit -m "feat(deep-dive): add frontend types and API for multi-turn Q&A"
```

---

### Task 6: Frontend Deep Dive Store

**Files:**
- Create: `frontend/src/stores/deepDiveStore.ts`

- [ ] **Step 1: Create deepDiveStore**

Create `frontend/src/stores/deepDiveStore.ts`:

```typescript
import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { DeepDiveContextType, ChatMessage } from '../types/deepDive'
import * as api from '../api/deepDiveApi'

export const useDeepDiveStore = defineStore('deepDive', () => {
  const isOpen = ref(false)
  const sourceType = ref<DeepDiveContextType>('RECOMMENDED_ANSWER')
  const contextContent = ref('')
  const messages = ref<ChatMessage[]>([])
  const inputText = ref('')
  const isStreaming = ref(false)
  const streamingContent = ref('')

  function openDeepDive(type: DeepDiveContextType, content: string) {
    sourceType.value = type
    contextContent.value = content
    messages.value = []
    inputText.value = ''
    streamingContent.value = ''
    isStreaming.value = false
    isOpen.value = true
  }

  async function sendMessage(question: string, questionText: string, expectedKeywords: string[]) {
    if (!question.trim() || isStreaming.value) return

    const userMessage: ChatMessage = { role: 'USER', content: question }
    messages.value.push(userMessage)
    inputText.value = ''
    isStreaming.value = true
    streamingContent.value = ''

    try {
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
      )

      if (streamingContent.value) {
        messages.value.push({ role: 'ASSISTANT', content: streamingContent.value })
      }
      streamingContent.value = ''
    } catch (e: any) {
      messages.value.pop()
      inputText.value = question
    } finally {
      isStreaming.value = false
    }
  }

  function closeDeepDive() {
    isOpen.value = false
  }

  function reset() {
    isOpen.value = false
    messages.value = []
    inputText.value = ''
    contextContent.value = ''
    streamingContent.value = ''
    isStreaming.value = false
  }

  return {
    isOpen, sourceType, contextContent, messages, inputText,
    isStreaming, streamingContent,
    openDeepDive, sendMessage, closeDeepDive, reset,
  }
})
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/stores/deepDiveStore.ts
git commit -m "feat(deep-dive): add Pinia store for multi-turn Q&A state"
```

---

### Task 7: DeepDiveDrawer Component

**Files:**
- Create: `frontend/src/components/DeepDiveDrawer.vue`

- [ ] **Step 1: Create the Drawer component**

Create `frontend/src/components/DeepDiveDrawer.vue`:

```vue
<template>
  <Transition name="drawer">
    <div
      v-if="store.isOpen"
      class="fixed inset-0 z-50 flex justify-end"
    >
      <div class="absolute inset-0 bg-black/30" @click="store.closeDeepDive()"></div>
      <div class="relative w-full max-w-lg bg-surface-container-lowest flex flex-col shadow-2xl">
        <!-- Header -->
        <div class="flex items-center justify-between px-5 py-4 border-b border-outline-variant/20 bg-surface-container-low">
          <div class="flex items-center gap-2 min-w-0">
            <span class="material-symbols-outlined text-primary text-xl">forum</span>
            <h3 class="font-headline font-bold text-on-surface text-sm truncate">深度追问</h3>
          </div>
          <button
            @click="store.closeDeepDive()"
            class="text-on-surface-variant hover:text-on-surface p-1 rounded-lg hover:bg-surface-container-highest transition-colors"
          >
            <span class="material-symbols-outlined text-xl">close</span>
          </button>
        </div>

        <!-- Context Summary -->
        <div class="px-5 py-3 bg-surface-container-low border-b border-outline-variant/10">
          <p class="text-xs text-on-surface-variant font-label mb-1">
            当前题目 · {{ store.sourceType === 'RECOMMENDED_ANSWER' ? '基于推荐答案' : '基于反馈点评' }}
          </p>
          <p class="text-sm text-on-surface line-clamp-2">{{ questionText }}</p>
        </div>

        <!-- Chat Messages -->
        <div ref="chatContainer" class="flex-1 overflow-y-auto px-5 py-4 space-y-4">
          <div
            v-for="(msg, i) in store.messages"
            :key="i"
            class="flex"
            :class="msg.role === 'USER' ? 'justify-end' : 'justify-start'"
          >
            <div
              class="max-w-[85%] rounded-xl px-4 py-3 text-sm"
              :class="msg.role === 'USER'
                ? 'bg-primary text-on-primary rounded-br-sm'
                : 'bg-surface-container-high text-on-surface rounded-bl-sm'"
            >
              <div v-if="msg.role === 'ASSISTANT'" class="markdown-body" v-html="renderMarkdown(msg.content)"></div>
              <div v-else>{{ msg.content }}</div>
            </div>
          </div>

          <!-- Streaming message -->
          <div v-if="store.isStreaming" class="flex justify-start">
            <div class="max-w-[85%] bg-surface-container-high text-on-surface rounded-xl rounded-bl-sm px-4 py-3 text-sm">
              <div v-if="store.streamingContent" class="markdown-body" v-html="renderMarkdown(store.streamingContent)"></div>
              <div v-else class="flex items-center gap-2 text-on-surface-variant">
                <span class="material-symbols-outlined animate-spin text-sm">progress_activity</span>
                思考中...
              </div>
            </div>
          </div>
        </div>

        <!-- Input Area -->
        <div class="px-5 py-4 border-t border-outline-variant/20 bg-surface-container-low">
          <div class="flex items-end gap-3">
            <textarea
              v-model="store.inputText"
              :disabled="store.isStreaming"
              rows="2"
              class="flex-1 bg-surface-container-highest text-on-surface rounded-xl px-4 py-3 text-sm resize-none focus:ring-0 outline-none placeholder-on-surface-variant/50"
              placeholder="输入追问..."
              @keydown="handleKeydown"
            ></textarea>
            <button
              @click="handleSend"
              :disabled="store.isStreaming || !store.inputText.trim()"
              class="bg-primary text-on-primary rounded-xl p-3 hover:opacity-90 transition-opacity disabled:opacity-40 flex-shrink-0"
            >
              <span class="material-symbols-outlined text-xl">send</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { ref, nextTick, watch } from 'vue'
import { useDeepDiveStore } from '../stores/deepDiveStore'
import { useInterviewStore } from '../stores/interviewStore'
import { renderMarkdown } from '../utils/markdown'

const store = useDeepDiveStore()
const interviewStore = useInterviewStore()
const chatContainer = ref<HTMLElement>()

const questionText = computed(() => interviewStore.currentQuestion?.question ?? '')
const expectedKeywords = computed(() => interviewStore.currentQuestion?.expectedKeywords ?? [])

function handleSend() {
  if (!store.inputText.trim() || store.isStreaming) return
  store.sendMessage(store.inputText, questionText.value, expectedKeywords.value)
}

function handleKeydown(e: KeyboardEvent) {
  if ((e.metaKey || e.ctrlKey) && e.key === 'Enter') {
    e.preventDefault()
    handleSend()
  }
}

watch(
  () => [store.messages.length, store.isStreaming, store.streamingContent],
  () => nextTick(() => {
    if (chatContainer.value) {
      chatContainer.value.scrollTop = chatContainer.value.scrollHeight
    }
  }),
)
</script>

<style scoped>
.drawer-enter-active,
.drawer-leave-active {
  transition: transform 0.3s ease;
}
.drawer-enter-from,
.drawer-leave-to {
  transform: translateX(100%);
}
</style>
```

Note: This file needs `import { computed } from 'vue'` — add `computed` to the existing vue import line:
```typescript
import { ref, computed, nextTick, watch } from 'vue'
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/DeepDiveDrawer.vue
git commit -m "feat(deep-dive): add DeepDiveDrawer chat component"
```

---

### Task 8: Integrate into MockInterviewRoomView

**Files:**
- Modify: `frontend/src/views/MockInterviewRoomView.vue`

- [ ] **Step 1: Add imports**

In the `<script setup>` section, add these imports after the existing imports (around line 301):

```typescript
import { useDeepDiveStore } from '../stores/deepDiveStore'
import DeepDiveDrawer from '../components/DeepDiveDrawer.vue'
```

And add the store reference after `const knowledgeStore = useKnowledgeStore()` (line 306):

```typescript
const deepDiveStore = useDeepDiveStore()
```

- [ ] **Step 2: Add "深度追问" button in Recommended Answer Panel**

In the `<template>` section, inside the Recommended Answer Panel's header button group (after the "保存到知识库" button, around line 180), add:

```html
<button
  v-if="store.recommendedAnswer && !store.isAnswerStreaming"
  @click="deepDiveStore.openDeepDive('RECOMMENDED_ANSWER', store.recommendedAnswer)"
  class="text-sm font-medium text-on-surface-variant hover:bg-surface-container-high px-3 py-2 rounded-lg transition-colors flex items-center gap-1"
>
  <span class="material-symbols-outlined text-base">forum</span>
  深度追问
</button>
```

- [ ] **Step 3: Add "深度追问" button in Feedback Drawer**

In the Feedback Drawer's action button area (inside the "继续挑战？" card, after the "继续挑战" button, around line 289), add:

```html
<button
  @click="deepDiveStore.openDeepDive('FEEDBACK', store.commentary)"
  class="bg-surface-container-high hover:bg-surface-container-highest text-on-surface font-label font-medium py-2.5 px-5 rounded-lg transition-colors flex items-center gap-2"
>
  <span class="material-symbols-outlined text-base">forum</span>
  深度追问
</button>
```

- [ ] **Step 4: Mount DeepDiveDrawer component**

Add the DeepDiveDrawer component at the very end of the template, just before the closing `</div>` of the root element (around line 295, before the last `</div>`):

```html
<DeepDiveDrawer />
```

- [ ] **Step 5: Verify frontend builds**

Run: `cd frontend && npm run build`
Expected: Build succeeds with no TypeScript errors

- [ ] **Step 6: Commit**

```bash
git add frontend/src/views/MockInterviewRoomView.vue
git commit -m "feat(deep-dive): integrate deep dive buttons into interview room"
```

---

### Task 9: End-to-End Verification

- [ ] **Step 1: Start backend**

Run: `cd backend && mvn spring-boot:run`
Wait for: "Started Application in" log line

- [ ] **Step 2: Start frontend dev server**

Run: `cd frontend && npm run dev`
Wait for: "Local: http://localhost:5173/"

- [ ] **Step 3: Manual test — Recommended Answer path**

1. Open http://localhost:5173/interview
2. Click "开始模拟"
3. Click "背题答案" — wait for answer to stream
4. Click "深度追问" button in the answer panel
5. Verify right-side Drawer opens
6. Type a follow-up question, press Cmd+Enter
7. Verify AI response streams in a chat bubble
8. Ask another question, verify history is maintained
9. Close Drawer, re-open — verify previous messages preserved

- [ ] **Step 4: Manual test — Feedback path**

1. Submit an answer to the question
2. Wait for feedback to stream
3. Click "深度追问" button in the feedback drawer
4. Verify Drawer opens with feedback as context
5. Ask follow-up questions about the feedback

- [ ] **Step 5: Verify no regressions**

1. Normal question generation still works
2. Recommended answer generation still works
3. Answer submission and feedback still works
4. Skip question still works
5. Continue challenge still works
