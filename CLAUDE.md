# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Local AI workbench for developer interview preparation. Desktop-style web app combining mock interviews, recommended answers, resume analysis, STAR rewriting, Obsidian knowledge base integration, and prompt visual tuning. Designed for local single-user deployment — no database, no login, no cloud accounts.

Built with Vue 3 + Spring Boot 3 + Spring AI, connecting to LLM providers (ZhiPu GLM default, OpenRouter alternative) via OpenAI-compatible API. All UI text and prompts are in Chinese.

## Common Commands

### Backend (Spring Boot / Maven)
```bash
cd backend
mvn spring-boot:run          # Start backend on :8080
./mvnw test                   # Run tests
./mvnw compile                # Compile only
```

### Frontend (Vue 3 / Vite)
```bash
cd frontend
npm install                   # Install dependencies
npm run dev                   # Dev server on :5173 (proxies /api → :8080)
npm run build                 # Type-check + production build (vue-tsc && vite build)
```

No linter is configured. The build command includes TypeScript type-checking via `vue-tsc`.

## Architecture

```
Browser (Vue 3 SPA :5173)
  ├── REST JSON → /api/* (Vite proxies to :8080)
  └── SSE streaming → fetch + ReadableStream
        │
Spring Boot API (:8080)
  ├── Spring AI ChatClient
  └── LLM Provider (ZhiPu GLM / OpenRouter)
```

**Frontend**: Vue 3.4 + TypeScript 5 + Vite 5 + Pinia (6 stores) + Vue Router + Tailwind CSS. State lives in Pinia stores and LocalStorage. `streamClient.ts` handles SSE via fetch+ReadableStream.

**Backend**: Spring Boot 3.3 + Java 17 + Spring AI 1.1. No database — settings persist in `backend/settings.properties`, knowledge notes are Obsidian Markdown files. Lombok is used throughout DTOs and services.

**AI Provider switching**: `AiConfig.java` manages a volatile `ChatClient` that can be refreshed at runtime. Supports `zhipu` and `openrouter` providers, each with different base URLs and completion paths. Default model is `glm-4-flash`.

**Prompt templates**: All AI prompts live in `backend/prompts/` as `.md` files using `{{variableName}}` placeholders. Loaded fresh on each call (no caching). Editable at runtime via the Settings page. Directory configurable via `PROMPT_DIR` env var.

## Key Design Patterns

- **DTOs by domain**: `dto/interview/`, `dto/resume/`, `dto/settings/`, `dto/knowledge/` — each domain has its own request/response objects
- **Services by domain**: `InterviewAiService` (prompt building + JSON generation), `ResumeAiService`, `SettingsService`, `ObsidianService`, `PromptService`. Streaming orchestration lives in separate services: `InterviewStreamService`, `ResumeStreamService`, `BatchQuestionStreamService`. All AI calls go through `AiGateway` which wraps Spring AI's `ChatClient`.
- **SSE streaming**: Endpoints suffixed with `/stream` return `text/event-stream`. Backend uses `SseEmitter` + `AiGateway.streamText()` with upstream subscription cancellation on timeout/close. Frontend `streamClient.ts` parses these via `fetch` + `ReadableStream`.
- **Pinia stores**: One per view (`interviewStore`, `resumeStore`, `settingsStore`, `knowledgeStore`, `rapidQuestionStore`) plus `deepDiveStore` for the multi-turn Q&A drawer in the interview room.
- **API layer**: `frontend/src/api/` has one file per backend controller. `streamClient.ts` is the shared SSE client.

## Environment Variables

| Variable | Default | Purpose |
|---|---|---|
| `ZHIPU_API_KEY` | `not-configured` | ZhiPu AI API key (can also set via Settings page) |
| `SETTINGS_FILE` | `backend/settings.properties` | Path to settings properties file |
| `PROMPT_DIR` | `prompts` | Path to prompt templates directory |

## Frontend Routes

| Path | View | Feature |
|---|---|---|
| `/interview` | `MockInterviewRoomView.vue` | Mock interview room |
| `/rapid` | `RapidQuestionView.vue` | Rapid batch questions |
| `/resume` | `ResumeOptimizerView.vue` | Resume optimizer |
| `/knowledge` | `KnowledgeBaseView.vue` | Obsidian knowledge browser |
| `/settings` | `SettingsView.vue` | API key, model, vault, prompt management |

## Backend API Prefix

All endpoints are under `/api/`. Key groups: `/api/interview/*`, `/api/resume/*`, `/api/settings/*`, `/api/knowledge/*`.
