## ADDED Requirements

### Requirement: User can polish resume experience with multiple versions
The system SHALL accept a resume experience paragraph (and optional JD) and return a streamed multi-version rewrite via SSE, including a technically-focused version and a business-results-focused version.

#### Scenario: Successful multi-version polish without JD
- **WHEN** user submits a resume experience paragraph to `POST /api/resume/polish/stream` without a JD
- **THEN** system streams a response containing at least 2 rewrite versions (技术深度版 and 业务结果版), each applying STAR framework, strong action verbs, and data-oriented language

#### Scenario: Successful polish with JD keywords
- **WHEN** user submits a resume experience paragraph together with a JD to `POST /api/resume/polish/stream`
- **THEN** system streams rewrite versions that naturally incorporate relevant JD keywords into the polished text

#### Scenario: Experience text too short
- **WHEN** user submits experience text with fewer than 20 meaningful characters
- **THEN** system returns HTTP 400 with message indicating the text is too short for meaningful polishing

### Requirement: Polish applies STAR framework
Each polished version SHALL restructure the input text to follow the STAR framework (Situation, Task, Action, Result) where applicable. Vague descriptions like "负责了某个系统的开发" SHALL be transformed into structured statements with clear context, actions, and outcomes.

#### Scenario: Vague description transformed
- **WHEN** input contains a vague responsibility description without specific actions or results
- **THEN** output SHALL contain a STAR-structured rewrite with placeholders (e.g., `[具体指标]`, `[提升百分比]`) for data the user needs to fill in

### Requirement: Polish strengthens action verbs
Each polished version SHALL replace weak verbs (做、参与、帮忙、负责) with high-impact action verbs (主导、重构、从0到1搭建、优化) appropriate to the context.

#### Scenario: Weak verb replacement
- **WHEN** input contains "参与了系统开发"
- **THEN** output SHALL use a stronger verb such as "主导" or "负责设计并实现" depending on context

### Requirement: Polish generates data-fill annotations
When the AI detects that the input lacks quantitative data support, the polished output SHALL include inline annotations prompting the user to fill in specific metrics (e.g., "这里提到提升了效率，具体提升了百分之多少？").

#### Scenario: Missing quantitative data
- **WHEN** input mentions an improvement or achievement without specific numbers
- **THEN** output SHALL include at least one annotation asking the user to provide the specific metric

### Requirement: Polish stream format
The SSE stream SHALL output Markdown-formatted text with each version separated by `### ` level-3 headings (e.g., `### 版本一：技术深度版`, `### 版本二：业务结果版`). A final section `### 数据补充建议` SHALL list all annotation prompts for missing data.

#### Scenario: Stream output structure
- **WHEN** a polish request completes streaming
- **THEN** the accumulated text SHALL contain at least two `### 版本` headings and one `### 数据补充建议` heading

### Requirement: Polish request accepts optional JD
The `PolishStreamRequest` SHALL have a required `sourceText` field and optional `jobDescription` field. When `jobDescription` is provided, the system SHALL incorporate relevant keywords; when absent, the system SHALL perform standalone polishing.

#### Scenario: Polish without JD
- **WHEN** request omits `jobDescription`
- **THEN** system SHALL perform polishing without keyword injection and SHALL NOT return an error
