## ADDED Requirements

### Requirement: User can trigger comprehensive resume health checkup
The system SHALL accept a resume text input and an optional JD text input, and return a structured multi-dimensional health checkup report simulating the real-world resume screening funnel.

#### Scenario: Successful health checkup with JD
- **WHEN** user submits resume text (≥50 meaningful characters) and JD text to `POST /api/resume/health-checkup`
- **THEN** system returns a JSON response containing: overall score (0-100), funnel layer scores (ats, hr, hiringManager, risk), categorized findings (redFlags, warnings, highlights), and line-by-line annotations with quoted text, problem description, and actionable suggestions

#### Scenario: Successful health checkup without JD
- **WHEN** user submits resume text (≥50 meaningful characters) without JD text
- **THEN** system returns a health checkup report with ATS keyword matching dimension marked as skipped, and all other dimensions evaluated normally

#### Scenario: Resume too short for analysis
- **WHEN** user submits resume text with fewer than 50 meaningful characters
- **THEN** system returns HTTP 400 with message indicating resume content is too short

#### Scenario: Resume lacks recognizable content
- **WHEN** user submits text that contains no resume-related signals (project, experience, skill, education keywords)
- **THEN** system returns HTTP 400 with message indicating the text does not appear to be resume content

### Requirement: Health checkup evaluates ATS pass-through probability
The system SHALL evaluate the resume's likelihood of passing ATS screening by checking: keyword coverage against JD (when JD provided), standard section header usage, date format consistency, and presence of parseable structure. Score range 0-100.

#### Scenario: Resume with good ATS signals and JD keywords
- **WHEN** resume contains standard section headers (工作经历/项目经历/教育背景/专业技能), consistent date formats, and JD keywords appear in the resume text
- **THEN** ATS score SHALL be ≥ 70

#### Scenario: Resume missing standard sections
- **WHEN** resume uses non-standard section headers or has no clear section structure
- **THEN** ATS score SHALL be < 50 and a warning SHALL be generated about section header formatting

### Requirement: Health checkup evaluates HR first-scan pass rate
The system SHALL evaluate whether a recruiter can understand the candidate's positioning within 10 seconds by checking: clear career positioning statement, visible company names and titles, reasonable job-hopping frequency, no unexplained gaps, and appropriate resume length for experience level. Score range 0-100.

#### Scenario: Resume with clear positioning and stable history
- **WHEN** resume has a clear professional title/positioning, shows stable employment (avg ≥ 1.5 years per role), and most recent role is prominent
- **THEN** HR score SHALL be ≥ 70

#### Scenario: Resume with frequent job changes
- **WHEN** resume shows 3+ company changes within 2 years
- **THEN** a red flag SHALL be generated about job-hopping frequency, and HR score SHALL be reduced

#### Scenario: Resume with unexplained employment gaps
- **WHEN** resume timeline has gaps ≥ 3 months between roles with no explanation
- **THEN** a red flag SHALL be generated about the unexplained gap

### Requirement: Health checkup evaluates hiring manager content quality
The system SHALL evaluate resume content depth by checking: quantification density (percentage of experience items with measurable results), action verb strength (ratio of strong verbs to weak verbs), outcome-orientation ratio (results described vs tasks listed), technical depth signals (specific architectural decisions and reasoning), and business value signals (measurable business impact). Score range 0-100.

#### Scenario: Resume with high quantification density
- **WHEN** ≥ 60% of experience bullet points contain specific numbers or metrics
- **THEN** hiring manager score SHALL receive a highlight annotation about strong quantification

#### Scenario: Resume with low quantification density
- **WHEN** < 20% of experience bullet points contain numbers or metrics
- **THEN** a warning SHALL be generated for each unquantified experience item, with suggestions for what metrics to add

#### Scenario: Resume with weak action verbs
- **WHEN** experience descriptions use weak verbs (参与/协助/负责/了解/熟悉) without demonstrating specific contribution
- **THEN** each weak verb usage SHALL be annotated with a suggestion to replace with a strong verb and specify the candidate's actual role

### Requirement: Health checkup detects risk signals
The system SHALL scan for risk signals including: excessive job-hopping (avg tenure < 1 year), unexplained career gaps ≥ 3 months, title/experience mismatch, skill list inflation (> 20 listed technologies), stagnation (same title/description for 5+ years with no growth), and potential overqualification/underqualification for target role (when JD provided).

#### Scenario: Resume with skill inflation
- **WHEN** resume lists more than 20 distinct technologies in a skills section
- **THEN** a warning SHALL be generated suggesting to focus on top 8-12 core technologies relevant to the target role

#### Scenario: Resume with career stagnation
- **WHEN** a candidate holds the same title with similar responsibilities across multiple years with no progression
- **THEN** a warning SHALL be generated about lack of growth trajectory

#### Scenario: No risk signals detected
- **WHEN** resume has none of the defined risk signals
- **THEN** risk section SHALL contain empty red flags list and a highlight noting clean risk profile

### Requirement: Health checkup provides line-by-line annotations
The system SHALL annotate specific lines or passages of the resume with: quoted original text (verbatim), problem category (weak-verb / no-metric / vague / redundant / missing-result / strong), specific suggestion, and optional rewrite text that can be applied to replace the original.

#### Scenario: Annotating a weak experience description
- **WHEN** a resume line reads "参与了用户系统的开发和维护"
- **THEN** the annotation SHALL quote the original text, identify it as weak-verb with no-metric, and suggest a rewrite with strong verb and placeholder metrics

#### Scenario: Annotating a strong experience description
- **WHEN** a resume line contains specific metrics, strong verbs, and clear outcomes
- **THEN** the annotation SHALL mark it as "strong" and include it as a highlight in the report

#### Scenario: User applies annotation rewrite
- **WHEN** user clicks "Apply" on an annotation that has a rewrite
- **THEN** the system SHALL replace the quoted original text in the resume textarea with the rewrite text

### Requirement: Frontend displays health checkup report with funnel visualization
The system SHALL display the health checkup report with: an overall score gauge, a funnel diagram showing pass-through rates at each screening layer, categorized finding cards (red flags / warnings / highlights with counts), and expandable annotation list grouped by resume section or chronologically.

#### Scenario: Displaying complete health checkup results
- **WHEN** health checkup API returns successfully
- **THEN** frontend SHALL render the funnel scores, findings categorized by severity, and all annotations with apply buttons

#### Scenario: Health checkup in progress
- **WHEN** health checkup API call is in progress
- **THEN** frontend SHALL show a loading state with indication that analysis is running

### Requirement: Health checkup result persists in browser
The system SHALL persist the health checkup result, the input resume text, and the optional JD text to LocalStorage so that the user can review results after page refresh.

#### Scenario: User refreshes page after checkup
- **WHEN** user completes a health checkup and then refreshes the browser
- **THEN** the resume text, JD text (if provided), and full checkup result SHALL be restored from LocalStorage
