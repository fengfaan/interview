## ADDED Requirements

### Requirement: User can upload PDF resume file
The system SHALL accept a PDF file via `POST /api/resume/import-file` with `multipart/form-data`, parse the file to extract text content, and return the extracted text along with file metadata.

#### Scenario: Successful PDF import
- **WHEN** user uploads a text-based PDF file (≤5MB) to `POST /api/resume/import-file`
- **THEN** system returns HTTP 200 with JSON body containing `text` (extracted plain text), `fileName` (original filename), and `pageCount` (number of pages)

#### Scenario: Scanned PDF with no text layer
- **WHEN** user uploads a scanned/image-based PDF that contains no extractable text
- **THEN** system returns HTTP 200 with `text` field as empty string and includes a `warning` field indicating the PDF appears to be a scanned document

#### Scenario: PDF file too large
- **WHEN** user uploads a PDF file larger than 5MB
- **THEN** system returns HTTP 413 with message indicating file size exceeds the limit

### Requirement: User can upload DOCX resume file
The system SHALL accept a DOCX file via the same `POST /api/resume/import-file` endpoint, parse the file to extract text content preserving paragraph structure, and return the extracted text.

#### Scenario: Successful DOCX import
- **WHEN** user uploads a DOCX file (≤5MB) to `POST /api/resume/import-file`
- **THEN** system returns HTTP 200 with JSON body containing `text` (extracted plain text with paragraph breaks), `fileName`, and `pageCount`

#### Scenario: DOCX with tables
- **WHEN** user uploads a DOCX file containing tables (e.g., skill comparison tables)
- **THEN** extracted text SHALL include table content serialized as line-based text with tab-separated columns

### Requirement: Unsupported file format rejected
The system SHALL reject files that are not PDF or DOCX format.

#### Scenario: Upload unsupported format
- **WHEN** user uploads a file with extension other than .pdf or .docx (e.g., .txt, .png, .rtf)
- **THEN** system returns HTTP 400 with message indicating only PDF and DOCX formats are supported

### Requirement: Extracted text is cleaned
The system SHALL perform basic text cleaning on extracted content before returning it.

#### Scenario: PDF with excessive blank lines
- **WHEN** extracted PDF text contains 3 or more consecutive blank lines
- **THEN** system SHALL collapse them to at most 2 blank lines in the returned text

#### Scenario: PDF with page number footers
- **WHEN** extracted PDF text contains lines matching common page number patterns (e.g., "第 1 页", "Page 1 of 3", standalone numbers)
- **THEN** system SHALL remove those lines from the returned text

### Requirement: Frontend provides file upload UI in resume input area
The ResumeOptimizerView SHALL display a file upload area near the resume textarea, supporting both click-to-select and drag-and-drop interactions for PDF and DOCX files.

#### Scenario: Click to upload
- **WHEN** user clicks the upload area and selects a PDF or DOCX file
- **THEN** the file is uploaded to the backend, parsed text is returned, and the resume textarea is populated with the extracted text

#### Scenario: Drag and drop upload
- **WHEN** user drags a PDF or DOCX file onto the upload area
- **THEN** the file is uploaded to the backend, parsed text is returned, and the resume textarea is populated with the extracted text

#### Scenario: Drop unsupported file type
- **WHEN** user drags a non-PDF/DOCX file onto the upload area
- **THEN** the UI displays an error message indicating only PDF and DOCX files are supported, and no upload occurs

### Requirement: Import response format
The `POST /api/resume/import-file` response SHALL conform to a JSON structure with specific fields.

#### Scenario: Response structure
- **WHEN** a valid file is successfully imported
- **THEN** the response SHALL be wrapped in the standard `ApiResponse` envelope, with `data` containing: `text` (string, the extracted text), `fileName` (string, original filename), `pageCount` (integer, number of pages), and optional `warning` (string, present only when text extraction had issues)
