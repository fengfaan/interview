## ADDED Requirements

### Requirement: Resume matching analysis
The system SHALL provide `POST /api/resume/analyze` that accepts `jobDescription` and `resume` text, returning a JSON response with `score` (0-100), `dimensions` array (each with name, score, reason), and `suggestions` array (each with id, priority, title, reason, sourceText).

#### Scenario: Successful analysis
- **WHEN** client posts valid JD and resume content
- **THEN** system returns a matching score, dimension-level breakdown, and actionable suggestions with specific resume text references

#### Scenario: Analysis identifies gaps
- **WHEN** resume lacks skills highlighted in the JD
- **THEN** suggestions include HIGH priority items identifying missing keywords and weak descriptions

### Requirement: STAR rewrite streaming
The system SHALL provide `POST /api/resume/rewrite/stream` that accepts `jobDescription`, `resume`, and a `suggestion` object, returning an SSE stream of Markdown containing: original description (### 原始描述), STAR rewrite example (### STAR 改写范例), and fill-in guidance (### 填写建议) with placeholder tokens like `[XX技术]`.

#### Scenario: Successful STAR rewrite
- **WHEN** client submits a suggestion targeting "负责订单系统开发"
- **THEN** system streams a STAR framework rewrite with placeholders for the user to fill in real data

#### Scenario: Rewrite references original text
- **WHEN** the suggestion's sourceText is "Assisted with project management"
- **THEN** the rewrite output shows the original text followed by a STAR-structured replacement

### Requirement: Left-right split layout
The resume page SHALL use a left-right split layout. Left column: JD input area (top) + resume editor (bottom). Right column: matching score display + dimension breakdown + optimization suggestion list.

#### Scenario: Split view layout
- **WHEN** user navigates to the resume optimizer page
- **THEN** left column shows JD and resume editors side by side or stacked, right column shows analysis results

### Requirement: Bottom STAR reconstruction workspace
The page SHALL have a bottom workspace area that expands when a suggestion is selected. It SHALL show: original description (left), STAR rewrite with placeholders (right), and "Apply to Resume" / "Dismiss" buttons.

#### Scenario: Select a suggestion
- **WHEN** user clicks a suggestion card in the right column
- **THEN** the bottom workspace expands, triggers the STAR rewrite stream, and displays the result

#### Scenario: Apply STAR rewrite to resume
- **WHEN** user clicks "Apply to Resume"
- **THEN** the original `sourceText` in the resume editor is replaced with the STAR rewrite content, and a re-analysis is NOT automatically triggered

#### Scenario: Dismiss suggestion
- **WHEN** user clicks "Dismiss"
- **THEN** the bottom workspace collapses and the suggestion is marked as dismissed in the UI

### Requirement: Matching score display
The system SHALL display the overall matching score as a percentage, with dimension-level scores shown as a list. Each dimension SHALL show name, score bar (color-coded: red <40, yellow 40-70, green >70), and reason.

#### Scenario: Dimension score visualization
- **WHEN** analysis returns dimensions with scores 90, 40, and 65
- **THEN** the first shows green, second shows yellow, third shows yellow, each with a progress bar proportional to score

### Requirement: Resume page keyboard shortcut
The system SHALL support `Cmd/Ctrl + Enter` to trigger analysis or submit the current optimization action.

#### Scenario: Trigger analysis
- **WHEN** user presses Cmd/Ctrl+Enter while JD and resume editors have content
- **THEN** matching analysis is triggered
