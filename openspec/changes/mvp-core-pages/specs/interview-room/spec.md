## ADDED Requirements

### Requirement: Generate interview question
The system SHALL provide `POST /api/interview/question` that generates a question based on direction, difficulty level, and conversation history. The response SHALL include `questionId`, `question` text, and `expectedKeywords` array.

#### Scenario: First question in a new session
- **WHEN** client posts `{ "direction": "GO_BACKEND", "level": "DEEP_PRINCIPLE", "history": [] }`
- **THEN** system responds with a question relevant to Go backend at deep-principle level, along with 4-6 expected keywords

#### Scenario: Question based on conversation history
- **WHEN** client posts with history containing 2 previous Q&A pairs
- **THEN** system generates a question that builds on or extends previous topics, avoiding repetition

### Requirement: Submit answer and get keyword analysis
The system SHALL provide `POST /api/interview/feedback` that accepts direction, level, question, answer, and expectedKeywords, returning structured JSON with `keywordHits` (hit/miss arrays) and a `score` (0-100).

#### Scenario: Partial keyword match
- **WHEN** user's answer contains 2 of 4 expected keywords
- **THEN** response includes `keywordHits.hit` with the 2 matched keywords, `keywordHits.miss` with the 2 missing ones, and a proportional score

#### Scenario: Empty answer
- **WHEN** user submits an empty or whitespace-only answer
- **THEN** system responds with HTTP 400 and error code `EMPTY_ANSWER`

### Requirement: Stream answer feedback
The system SHALL provide `POST /api/interview/feedback/stream` that accepts the same input as the feedback endpoint and returns an SSE stream of Markdown text containing: commentary (## 点评), demo answer (## 口语化示范, max 300 chars), and follow-up question (## 深度追问). The response SHALL also include a `followUpQuestion` field for the next question.

#### Scenario: Successful streaming feedback
- **WHEN** client submits a valid answer via the stream endpoint
- **THEN** system streams Markdown sections in order: 点评 → 口语化示范 → 深度追问, then sends `[DONE]`

#### Scenario: Follow-up question is contextually relevant
- **WHEN** user answered about Go garbage collection
- **THEN** the follow-up question relates to the user's specific answer content, not a random new topic

### Requirement: Skip current question
The system SHALL allow skipping the current question by calling the generate question endpoint with the current question added to history marked as `skipped: true`.

#### Scenario: Skip and get new question
- **WHEN** client posts to `/api/interview/question` with history including the current question as skipped
- **THEN** system generates a new question without repeating the skipped one

### Requirement: Interview session state in frontend
The frontend SHALL manage interview state including: current direction, current level, session active status, current question, user draft answer, Q&A history, feedback drawer expanded state, keyword hit results, AI demo answer, and follow-up question.

#### Scenario: Submit answer triggers feedback flow
- **WHEN** user clicks "Submit Answer" (or presses Cmd/Ctrl+Enter)
- **THEN** frontend sends answer to feedback JSON endpoint, renders keyword radar, then opens feedback drawer and starts SSE stream for commentary

#### Scenario: Follow-up question becomes next question
- **WHEN** user clicks "Continue Challenge" after seeing feedback
- **THEN** the followUpQuestion from feedback becomes the new current question, answer area is cleared, and feedback drawer collapses

### Requirement: Three-section page layout
The interview page SHALL use a top-middle-bottom layout: top config bar (direction selector, difficulty selector, start button), middle area (question bubble left + answer editor right), bottom feedback drawer (collapsed by default, auto-expands after submission).

#### Scenario: Config bar controls session
- **WHEN** user selects a direction and clicks "Start Simulation"
- **THEN** a new session begins, the first question is generated, and config controls are disabled during active session

#### Scenario: Feedback drawer interaction
- **WHEN** feedback is received after answer submission
- **THEN** the bottom drawer auto-expands to show keyword hits, streaming commentary, and the follow-up question card

### Requirement: Keyboard shortcut for answer submission
The system SHALL support `Cmd/Ctrl + Enter` to submit the current answer in the interview room.

#### Scenario: Keyboard submit
- **WHEN** user presses Cmd+Enter (Mac) or Ctrl+Enter (Windows/Linux) while the answer textarea is focused
- **THEN** the answer is submitted (equivalent to clicking "Submit Answer")
