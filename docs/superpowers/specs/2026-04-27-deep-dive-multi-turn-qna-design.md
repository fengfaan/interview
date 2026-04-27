# Deep Dive Multi-Turn Q&A — Design Spec

## Problem

In the interview practice room, when a user encounters concepts they don't fully understand in the recommended answer or AI feedback, there is no way to ask follow-up questions. The user must move on with incomplete understanding.

## Solution

Add a "Deep Dive" multi-turn Q&A feature: a right-side Drawer panel that opens from either the recommended answer or feedback commentary section. The user can freely ask questions about any concept, and the AI responds with context-aware explanations using the full conversation history.

## User Flow

1. User reads recommended answer or feedback commentary
2. Clicks "深度追问" button
3. Right-side Drawer slides open with the current question as context
4. User types a follow-up question, AI streams a response
5. Repeat — AI remembers full conversation history within the session
6. Close Drawer; conversation history preserved until session resets

## Data Model

### New DTOs

**`DeepDiveRequest`** (`dto/interview/DeepDiveRequest.java`):
- `question` (String) — original interview question
- `expectedKeywords` (List\<String\>) — expected keywords
- `contextType` (DeepDiveContextType) — RECOMMENDED_ANSWER or FEEDBACK
- `contextContent` (String) — the recommended answer or feedback text
- `messages` (List\<ChatMessage\>) — conversation history

**`ChatMessage`** (`dto/interview/ChatMessage.java`):
- `role` (ChatRole enum: USER, ASSISTANT)
- `content` (String)

**`DeepDiveContextType`** (enum):
- `RECOMMENDED_ANSWER`
- `FEEDBACK`

## API

### `POST /api/interview/deep-dive/stream`

- Request body: `DeepDiveRequest` (validated)
- Response: `text/event-stream` — same SSE format as existing endpoints (data chunks + `[DONE]`)
- Streaming via `AiGateway.streamText()` and `SseUtils`

Stateless design: the frontend sends full conversation history with each request. No server-side session storage. Consistent with existing architecture.

## Prompt Template

New file: `backend/prompts/interview/deep-dive.md`

Variables: `{{question}}`, `{{expectedKeywords}}`, `{{contextType}}`, `{{contextContent}}`, `{{history}}`

The prompt sets the AI role as an interview coach explaining concepts. It instructs the AI to:
- Use analogies and examples
- Connect explanations back to the original interview question
- Output Markdown format
- Reference keywords when relevant

## Backend Changes

### New files
- `dto/interview/DeepDiveRequest.java` — request DTO
- `dto/interview/ChatMessage.java` — chat message DTO
- `dto/interview/DeepDiveContextType.java` — context type enum
- `prompts/interview/deep-dive.md` — prompt template

### Modified files
- `InterviewController.java` — add `POST /api/interview/deep-dive/stream` endpoint
- `InterviewAiService.java` — add `buildDeepDivePrompt()` method
- `InterviewStreamService.java` — add `streamDeepDive()` method

## Frontend Changes

### New files
- `stores/deepDiveStore.ts` — Pinia store for deep dive state
- `components/DeepDiveDrawer.vue` — Drawer component with chat UI
- `api/interviewApi.ts` — add `streamDeepDive()` API call

### Modified files
- `MockInterviewRoomView.vue` — add "深度追问" buttons to recommended answer panel and feedback panel, mount DeepDiveDrawer component

### DeepDiveStore State
- `isOpen` (boolean) — Drawer visibility
- `sourceType` (DeepDiveContextType) — which panel opened the drawer
- `questionContext` — original question + keywords (read from interviewStore)
- `contextContent` (string) — recommended answer or feedback text
- `messages` (ChatMessage[]) — conversation history
- `inputText` (string) — current input
- `isStreaming` (boolean) — streaming state

### DeepDiveStore Actions
- `openDeepDive(sourceType, contextContent)` — inject context, open drawer
- `sendMessage()` — build request with full history, call SSE endpoint, append AI response
- `closeDeepDive()` — close drawer, preserve history
- `reset()` — clear all state

### DeepDiveDrawer Layout
1. **Top bar**: current question summary (read-only), close button
2. **Chat area**: scrollable message list with user/AI bubbles, AI messages rendered as Markdown
3. **Input area**: textarea + send button, Cmd+Enter to send
