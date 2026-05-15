## 1. Backend DTOs

- [x] 1.1 Create `HealthCheckupRequest` DTO with `resume` (@NotBlank, min 50 chars) and optional `jobDescription` fields
- [x] 1.2 Create `HealthCheckupResponse` DTO with `overallScore`, `funnelScores` (ats/hr/hiringManager/risk), `redFlags`, `warnings`, `highlights` (each as list of `Finding`), `annotations` (list of `Annotation`), and `summary`
- [x] 1.3 Create `Finding` DTO with `category`, `title`, `detail` fields
- [x] 1.4 Create `Annotation` DTO with `quote`, `location`, `category` (weak-verb/no-metric/vague/redundant/missing-result/strong), `problem`, `suggestion`, `rewrite` (nullable) fields
- [x] 1.5 Create `FunnelScore` DTO with `score` (0-100) and `detail` fields

## 2. Backend Prompt Template

- [x] 2.1 Create `backend/prompts/resume/health-checkup.md` prompt template with: system role (ATS/HR/hiring manager/risk assessor), funnel evaluation criteria, annotation scanning rules, strict JSON output schema matching the response DTO, and handling for optional JD (skip ATS keyword matching when absent)

## 3. Backend Service & Controller

- [x] 3.1 Add `healthCheckup(HealthCheckupRequest)` method to `ResumeAiService` — validate input, render `health-checkup.md` prompt, call `aiGateway.generateJson`, deserialize to `HealthCheckupResponse`
- [x] 3.2 Add `POST /api/resume/health-checkup` endpoint to `ResumeController` accepting `HealthCheckupRequest`, returning `JsonResult<HealthCheckupResponse>`

## 4. Frontend Types & Store

- [x] 4.1 Add TypeScript types for `HealthCheckupResponse`, `FunnelScore`, `Finding`, `Annotation` to `frontend/src/types/resume.ts`
- [x] 4.2 Add `healthCheckupResult` state and `isHealthChecking` loading state to `resumeStore`
- [x] 4.3 Add `healthCheckup()` action to `resumeStore` calling `POST /api/resume/health-checkup`
- [x] 4.4 Add `applyAnnotationRewrite(annotation)` action to `resumeStore` replacing `annotation.quote` with `annotation.rewrite` in resume text
- [x] 4.5 Add health checkup result persistence to LocalStorage (save/restore in `persist()`/`restore()`)

## 5. Frontend API Layer

- [x] 5.1 Add `healthCheckup(data)` function to `frontend/src/api/resumeApi.ts` calling `POST /api/resume/health-checkup`

## 6. Frontend UI

- [x] 6.1 Add "简历体检" tab to `ResumeOptimizerView.vue` tab list (4th tab after polish)
- [x] 6.2 Build health checkup input area: resume textarea (shared with other tabs), optional JD textarea, "开始体检" button, with input validation
- [x] 6.3 Build overall score display with gauge/chart component (reuse existing SVG circle gauge from match tab)
- [x] 6.4 Build funnel visualization showing 4 layers with pass-through scores and detail text
- [x] 6.5 Build categorized findings section: red flags / warnings / highlights with expandable cards and counts
- [x] 6.6 Build annotation list: each annotation shows quoted original text, category badge, problem description, suggestion, and "Apply rewrite" button (when rewrite is available)
- [x] 6.7 Wire up tab switching, loading states, error handling, and Cmd/Ctrl+Enter shortcut
