## ADDED Requirements

### Requirement: Interview state persistence
The system SHALL save interview page state to LocalStorage under key `ai-career-prep.mock-interview`. Saved data SHALL include: direction, level, current question, user draft answer, Q&A history (up to 10 rounds), keyword results, and feedback drawer state.

#### Scenario: State persists across page navigation
- **WHEN** user switches to Resume Optimizer and back to Interview Room
- **THEN** all interview state (current question, draft answer, history) is restored

#### Scenario: State persists across browser refresh
- **WHEN** user refreshes the page during an active interview
- **THEN** the interview resumes with the same question, history, and draft answer

### Requirement: Resume state persistence
The system SHALL save resume page state to LocalStorage under key `ai-career-prep.resume-optimizer`. Saved data SHALL include: JD content, resume content, analysis results (score, dimensions, suggestions), and STAR rewrite drafts.

#### Scenario: State persists across page navigation
- **WHEN** user switches to Interview Room and back to Resume Optimizer
- **THEN** JD content, resume content, and analysis results are restored

### Requirement: Auto-save on input change
The system SHALL debounce and auto-save state to LocalStorage within 500ms of any user input change (typing in textarea, selecting options, etc.).

#### Scenario: Typing in answer textarea
- **WHEN** user types in the answer textarea
- **THEN** state is saved to LocalStorage within 500ms of the last keystroke

### Requirement: Session cleanup policy
The system SHALL keep only the 3 most recent session entries per page in LocalStorage. When a 4th session is created, the oldest SHALL be automatically removed.

#### Scenario: Session limit enforcement
- **WHEN** user starts a 4th new interview simulation
- **THEN** the 1st (oldest) simulation's data is removed from LocalStorage

### Requirement: New session action
Each page SHALL provide a "New Session" or equivalent action that clears current page state and starts fresh, while preserving the old session as a history entry.

#### Scenario: Start new interview session
- **WHEN** user clicks "New Simulation" in the sidebar
- **THEN** current interview state is archived, a fresh state is initialized, and the page is ready for a new session
