## 1. Backend Project Setup

- [x] 1.1 Initialize Spring Boot 3 project with Maven, JDK 17, and directory structure (config, controller, service, prompt, dto, common)
- [x] 1.2 Add spring-ai-zhipuai-spring-boot-starter dependency and configure application.yml with ZhiPu AI (model: glm-4-flash, api-key from env var)
- [x] 1.3 Implement CorsConfig allowing localhost:5173
- [x] 1.4 Implement ApiResponse wrapper class and GlobalExceptionHandler for unified error format { success, error, message }
- [x] 1.5 Implement SseUtils for sending Server-Sent Events (data chunk, error event, [DONE] terminator)

## 2. Backend — Interview API

- [x] 2.1 Create InterviewPrompts with system prompt for "资深技术面试官" role and question generation prompt template
- [x] 2.2 Create interview DTOs: QuestionRequest (direction, level, history), QuestionResponse (questionId, question, expectedKeywords), FeedbackRequest (direction, level, question, answer, expectedKeywords), FeedbackResponse (keywordHits, score), FeedbackStreamRequest
- [x] 2.3 Implement InterviewAiService.question() — call ZhiPu via Spring AI ChatClient with structured output, return question + keywords
- [x] 2.4 Implement InterviewAiService.feedback() — analyze answer against keywords, return hit/miss arrays and score via structured output
- [x] 2.5 Implement InterviewAiService.feedbackStream() — generate streaming Markdown (点评 + 口语化示范 + 深度追问), include followUpQuestion in final event
- [x] 2.6 Implement InterviewController with endpoints: POST /api/interview/question, POST /api/interview/feedback, POST /api/interview/feedback/stream
- [x] 2.7 Add input validation: direction/level enum check, answer max 5000 chars, required field checks

## 3. Backend — Resume API

- [x] 3.1 Create ResumePrompts with system prompt for "技术招聘视角的简历优化顾问" role and analysis/rewrite prompt templates
- [x] 3.2 Create resume DTOs: AnalyzeRequest (jobDescription, resume), AnalyzeResponse (score, dimensions[], suggestions[]), RewriteStreamRequest (jobDescription, resume, suggestion)
- [x] 3.3 Implement ResumeAiService.analyze() — call ZhiPu with structured output for score + dimensions + suggestions
- [x] 3.4 Implement ResumeAiService.rewriteStream() — generate streaming STAR rewrite Markdown (原始描述 + STAR改写范例 + 填写建议)
- [x] 3.5 Implement ResumeController with endpoints: POST /api/resume/analyze, POST /api/resume/rewrite/stream
- [x] 3.6 Add input validation: JD and resume max 20000 chars each, required field checks

## 4. Frontend Project Setup

- [x] 4.1 Initialize Vue 3 + TypeScript + Vite project in frontend/ directory
- [x] 4.2 Install and configure Tailwind CSS with design system tokens (colors: surface/primary/secondary/error, fonts: Manrope/Inter, border-radius)
- [x] 4.3 Install dependencies: vue-router, pinia, marked (for Markdown rendering)
- [x] 4.4 Configure Vue Router: /interview, /resume, / redirects to /interview
- [x] 4.5 Create AppLayout.vue with fixed sidebar (w-64) and scrollable main content area
- [x] 4.6 Create AppSidebar.vue with two navigation items (面试演练室, 简历调优台) and active state highlighting
- [x] 4.7 Create Pinia stores skeleton: interviewStore.ts, resumeStore.ts

## 5. Frontend — Interview Room Page

- [x] 5.1 Create MockInterviewRoomView.vue with three-section layout (top config bar, middle Q&A area, bottom feedback drawer)
- [x] 5.2 Implement InterviewConfigBar — direction dropdown (GO_BACKEND, REACT_FRONTEND, SYSTEM_DESIGN), difficulty buttons (基础八股, 深度原理, 项目实战), Start Simulation button
- [x] 5.3 Implement question display — question bubble on left side of middle area, showing question text and description
- [x] 5.4 Implement answer editor — large textarea on right side of middle area, with Submit Answer and Skip buttons, character count indicator
- [x] 5.5 Implement streamClient utility — streamPost(url, body, onChunk) using fetch + ReadableStream + TextDecoder for SSE
- [x] 5.6 Implement feedback flow — call /feedback JSON endpoint, render keyword hits (green tags for hit, red for miss), then start SSE stream for commentary
- [x] 5.7 Implement feedback drawer — collapsible panel showing: keyword radar, streaming Markdown commentary (rendered with marked), follow-up question card with "Continue Challenge" button
- [x] 5.8 Implement follow-up flow — clicking "Continue Challenge" sets followUpQuestion as new current question, clears answer area, collapses feedback drawer
- [x] 5.9 Implement skip logic — clicking Skip adds current question to history as skipped, calls /question for a new one
- [x] 5.10 Wire up Cmd/Ctrl+Enter keyboard shortcut for answer submission

## 6. Frontend — Resume Optimizer Page

- [x] 6.1 Create ResumeOptimizerView.vue with left-right split layout and bottom STAR workspace
- [x] 6.2 Implement left column — JD textarea (top half) and resume Markdown editor (bottom half), both scrollable with max-length indicators
- [x] 6.3 Implement right column — matching score display (percentage + dimension list with color-coded progress bars), optimization suggestion list
- [x] 6.4 Implement analysis trigger — "Generate Report" button or Cmd/Ctrl+Enter calls POST /api/resume/analyze, renders results in right column
- [x] 6.5 Implement suggestion selection — clicking a suggestion expands bottom STAR workspace and triggers POST /api/resume/rewrite/stream
- [x] 6.6 Implement STAR workspace — bottom panel with original description (left), streaming STAR rewrite (right), Apply/Dismiss buttons
- [x] 6.7 Implement "Apply to Resume" — find sourceText in resume content and replace with STAR rewrite result
- [x] 6.8 Implement "Dismiss" — collapse workspace, mark suggestion as dismissed

## 7. Frontend — LocalStorage & Polish

- [x] 7.1 Create useLocalStorage composable with debounce (500ms) auto-save, keyed by page (ai-career-prep.mock-interview, ai-career-prep.resume-optimizer)
- [x] 7.2 Wire interviewStore to LocalStorage — save/restore direction, level, question, draft, history, feedback state
- [x] 7.3 Wire resumeStore to LocalStorage — save/restore JD, resume content, analysis results, STAR drafts
- [x] 7.4 Implement session cleanup — keep max 3 sessions per page, auto-remove oldest on 4th
- [x] 7.5 Implement "New Simulation" button — archive current state, initialize fresh interview session
- [x] 7.6 Add loading states — spinner/skeleton for API calls, streaming indicator for SSE, disable submit during active requests
- [x] 7.7 Add error handling — display retry-able error banners for API failures, detect SSE disconnect and offer regenerate
