## MODIFIED Requirements

### Requirement: User can analyze resume structure without JD
The system SHALL accept a resume text input (without requiring a JD) and return a structural assessment report covering module completeness, ordering logic, content density, and format compliance. The backend API endpoint `POST /api/resume/structure-analysis` SHALL remain available and unchanged for backward compatibility. The frontend structure check tab SHALL remain accessible but be positioned as a lightweight alternative to the comprehensive health checkup.

#### Scenario: Successful structure analysis
- **WHEN** user submits resume text (≥50 meaningful characters containing resume signals) to `POST /api/resume/structure-analysis`
- **THEN** system returns a JSON response containing: overall structure score (0-100), list of module check items each with name/status(pass|warn|fail)/detail, list of structural issues each with severity(critical|warning|info)/description/suggestion, and a one-sentence fatal problem summary

#### Scenario: Resume too short for analysis
- **WHEN** user submits resume text with fewer than 50 meaningful characters
- **THEN** system returns HTTP 400 with message indicating resume content is too short

#### Scenario: Resume lacks recognizable content
- **WHEN** user submits text that contains no resume-related signals (project, experience, skill, education keywords)
- **THEN** system returns HTTP 400 with message indicating the text does not appear to be resume content
