# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Local AI workbench for developer interview preparation. Desktop-style web app combining mock interviews, rapid batch questions, recommended answers, resume analysis, STAR rewriting, web question import, Obsidian knowledge base integration, prompt style profiles, and prompt visual tuning. Designed for local single-user deployment — no database, no login, no cloud accounts.

Built with Vue 3 + Spring Boot 3 + Spring AI, connecting to LLM providers (ZhiPu GLM default, MiMo, OpenRouter alternative) via OpenAI-compatible API. All UI text and prompts are in Chinese.

## Common Commands

### Backend (Spring Boot / Maven)
```bash
cd backend
./mvnw spring-boot:run                    # Start backend on :8080
./mvnw test                               # Run all tests
./mvnw test -Dtest=ModelCircuitBreakerTest # Run a single test class
./mvnw compile                            # Compile only
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
  ├── AiGateway (unified AI call entry point)
  │     ├── Circuit Breaker → fallback to backup model
  │     └── Spring AI ChatClient
  ├── PromptService ← prompts/*.md templates + style injection
  ├── StyleService  ← prompts/styles/*.json
  ├── DeepDiveAgent ← KnowledgeTools (knowledge base retrieval)
  ├── ConsolidateService ← AI-powered question parsing + dedup
  └── OnnxEmbeddingService (local vector inference, bge-micro-v2)
        │
  LLM Provider (ZhiPu GLM / MiMo / OpenRouter)
```

**Frontend**: Vue 3.4 + TypeScript 5 + Vite 5 + Pinia (7 stores) + Vue Router + Tailwind CSS. State lives in Pinia stores and LocalStorage. `streamClient.ts` handles SSE via fetch+ReadableStream.

**Backend**: Spring Boot 3.3 + Java 17 + Spring AI 1.1. No database — settings persist in `backend/settings.properties`, knowledge notes are Obsidian Markdown files. Lombok is used throughout DTOs and services.

**AI Provider switching**: `AiConfig.java` manages a volatile `ChatClient` that can be refreshed at runtime. Supports `zhipu`, `mimo`, and `openrouter` providers, each with different base URLs and completion paths. Default model is `glm-4-flash`.

**Circuit breaker**: `ModelCircuitBreaker` tracks consecutive failures per model and automatically falls back to a backup model chain. Health status is exposed via `/api/settings/model-health`.

**DeepDive Agent**: Multi-turn follow-up Q&A on interview feedback or recommended answers. The agent autonomously calls `KnowledgeTools` to retrieve relevant notes from the Obsidian vault during conversation.

**Prompt templates**: All AI prompts live in `backend/prompts/` as `.md` files using `{{variableName}}` placeholders. Loaded fresh on each call (no caching). Editable at runtime via the Settings page. Directory configurable via `PROMPT_DIR` env var.

**Style profiles**: Per direction × level (5 directions × 3 levels = 15 combinations) style configurations stored as JSON in `backend/prompts/styles/`. Injected into prompts via `{{styleInstruction}}` placeholder.

**Embedding**: Built-in ONNX inference with `bge-micro-v2` model for semantic search and dedup. No external sidecar needed. Optional external endpoint configurable via `SMART_EMBEDDING_ENDPOINT`.

## Key Design Patterns

- **DTOs by domain**: `dto/interview/`, `dto/resume/`, `dto/settings/`, `dto/knowledge/`, `dto/import_/` — each domain has its own request/response objects
- **Services by domain**: `InterviewAiService` (prompt building + JSON generation), `ResumeAiService`, `SettingsService`, `ObsidianService`, `PromptService`, `ConsolidateService`, `BrowserCaptureService`. Streaming orchestration lives in separate services: `InterviewStreamService`, `ResumeStreamService`, `BatchQuestionStreamService`. All AI calls go through `AiGateway` which wraps Spring AI's `ChatClient`.
- **SSE streaming**: Endpoints suffixed with `/stream` return `text/event-stream`. Backend uses `SseEmitter` + `AiGateway.streamText()` with upstream subscription cancellation on timeout/close. Frontend `streamClient.ts` parses these via `fetch` + `ReadableStream`.
- **Pinia stores**: One per view (`interviewStore`, `resumeStore`, `settingsStore`, `knowledgeStore`, `rapidQuestionStore`, `importStore`) plus `deepDiveStore` for the multi-turn Q&A drawer in the interview room.
- **API layer**: `frontend/src/api/` has one file per backend controller. `streamClient.ts` is the shared SSE client.

## Environment Variables

| Variable | Default | Purpose |
|---|---|---|
| `ZHIPU_API_KEY` | `not-configured` | ZhiPu AI API key (can also set via Settings page) |
| `MIMO_API_KEY` | empty | MiMo API key |
| `OPENROUTER_API_KEY` | empty | OpenRouter API key |
| `OPENROUTER_PROXY` | empty | OpenRouter HTTP proxy (e.g. `http://127.0.0.1:7890`) |
| `SETTINGS_FILE` | auto-resolved | Path to settings properties file |
| `PROMPT_DIR` | `prompts` | Path to prompt templates directory |
| `AI_SYNC_TIMEOUT_MS` | `120000` | Synchronous AI request timeout |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:8080` | Allowed frontend origins |
| `SMART_EMBEDDING_ENDPOINT` | empty (uses built-in ONNX) | External vector service endpoint |

## Frontend Routes

| Path | View | Feature |
|---|---|---|
| `/interview` | `MockInterviewRoomView.vue` | Mock interview room |
| `/rapid` | `RapidQuestionView.vue` | Rapid batch questions |
| `/import` | `ImportView.vue` | Web question import (browser capture + AI parse) |
| `/resume` | `ResumeOptimizerView.vue` | Resume optimizer |
| `/knowledge` | `KnowledgeBaseView.vue` | Obsidian knowledge browser |
| `/settings` | `SettingsView.vue` | API key, model, vault, prompt management |

## Backend API Prefix

All endpoints are under `/api/`. Key groups: `/api/interview/*`, `/api/resume/*`, `/api/settings/*`, `/api/knowledge/*`.
