## ADDED Requirements

### Requirement: User can analyze resume structure without JD
The system SHALL accept a resume text input (without requiring a JD) and return a structural assessment report covering module completeness, ordering logic, content density, and format compliance.

#### Scenario: Successful structure analysis
- **WHEN** user submits resume text (≥50 meaningful characters containing resume signals) to `POST /api/resume/structure-analysis`
- **THEN** system returns a JSON response containing: overall structure score (0-100), list of module check items each with name/status(pass|warn|fail)/detail, list of structural issues each with severity(critical|warning|info)/description/suggestion, and a one-sentence fatal problem summary

#### Scenario: Resume too short for analysis
- **WHEN** user submits resume text with fewer than 50 meaningful characters
- **THEN** system returns HTTP 400 with message indicating resume content is too short

#### Scenario: Resume lacks recognizable content
- **WHEN** user submits text that contains no resume-related signals (project, experience, skill, education keywords)
- **THEN** system returns HTTP 400 with message indicating the text does not appear to be resume content

### Requirement: Structure analysis checks module completeness
The system SHALL check whether the resume contains the following key modules: personal info/contact, education background, work/project experience, and professional skills. Each missing module SHALL be flagged as a `fail` status module check item.

#### Scenario: Resume missing contact information
- **WHEN** resume text does not contain recognizable contact information (phone, email)
- **THEN** the module check for "联系方式" SHALL have status `fail` with detail explaining the omission

#### Scenario: Resume has all key modules
- **WHEN** resume text contains personal info, education, work experience, and skills sections
- **THEN** all module check items SHALL have status `pass`

### Requirement: Structure analysis checks ordering and logic
The system SHALL evaluate whether experiences are presented in reverse chronological order (most recent first) and whether work experience is positioned before education for non-fresh-graduates.

#### Scenario: Experience not in reverse chronological order
- **WHEN** resume lists older experiences before newer ones
- **THEN** a structural issue with severity `warning` SHALL be returned advising reverse chronological ordering

### Requirement: Structure analysis checks content density
The system SHALL identify content density imbalances such as overly long self-evaluations, non-core projects taking excessive space, or key experiences described too briefly.

#### Scenario: Self-evaluation section too long
- **WHEN** self-evaluation or summary section occupies disproportionate space relative to project experience
- **THEN** a structural issue with severity `warning` SHALL be returned noting the imbalance

### Requirement: Structure analysis response format
The system SHALL return the analysis as a JSON object conforming to `StructureAnalysisResponse` with fields: `structureScore` (int 0-100), `moduleChecks` (list of ModuleCheck), `issues` (list of StructuralIssue), and `summary` (string, one-sentence fatal problem).

#### Scenario: Response structure validation
- **WHEN** a valid resume is analyzed
- **THEN** the response SHALL contain all four top-level fields with correct types, moduleChecks SHALL have at least 4 items, and summary SHALL be non-empty
