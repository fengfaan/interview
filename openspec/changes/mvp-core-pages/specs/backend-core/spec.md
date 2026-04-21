## ADDED Requirements

### Requirement: Spring Boot 3 project with Spring AI
The system SHALL initialize a Spring Boot 3 project with Spring AI and the ZhiPu AI starter dependency.

#### Scenario: Backend starts successfully
- **WHEN** developer runs `mvn spring-boot:run` with a valid `ZHIPU_API_KEY` environment variable
- **THEN** the application starts on port 8080 and can connect to ZhiPu AI

### Requirement: ZhiPu AI model configuration
The system SHALL use ZhiPu AI as the LLM provider. The model name SHALL be configurable via `application.yml` with a default of `glm-4-flash`. The API key SHALL be read from the `ZHIPU_API_KEY` environment variable.

#### Scenario: Model configuration is applied
- **WHEN** application.yml specifies `spring.ai.zhipuai.chat.options.model: glm-4-plus`
- **THEN** all AI calls use the glm-4-plus model

#### Scenario: Missing API key
- **WHEN** the application starts without `ZHIPU_API_KEY` environment variable
- **THEN** the application logs a clear error message about the missing configuration

### Requirement: CORS configuration for development
The system SHALL allow CORS requests from `http://localhost:5173` during development, with GET, POST, and OPTIONS methods allowed.

#### Scenario: Frontend makes cross-origin request
- **WHEN** frontend at `http://localhost:5173` sends a POST request to `http://localhost:8080/api/interview/question`
- **THEN** the request is accepted without CORS errors

### Requirement: Unified error response format
All API error responses SHALL return JSON in the format: `{ "success": false, "error": "ERROR_CODE", "message": "Human-readable description" }`.

#### Scenario: Invalid request body
- **WHEN** client sends a POST request with malformed JSON
- **THEN** the system responds with HTTP 400 and body `{ "success": false, "error": "BAD_REQUEST", "message": "..." }`

#### Scenario: AI service unavailable
- **WHEN** ZhiPu AI call fails or times out (60 seconds)
- **THEN** the system responds with HTTP 503 and body `{ "success": false, "error": "AI_SERVICE_ERROR", "message": "..." }`

### Requirement: SSE utility for streaming responses
The system SHALL provide a utility for sending Server-Sent Events with `text/event-stream` content type. Each event SHALL contain a `data` field with text content. The stream SHALL end with a `data: [DONE]` event.

#### Scenario: SSE stream completes normally
- **WHEN** AI generates a streaming response
- **THEN** each text chunk is sent as `data: <chunk>\n\n`, followed by `data: [DONE]\n\n` when complete

#### Scenario: SSE stream encounters error
- **WHEN** AI call fails mid-stream
- **THEN** an `event: error\ndata: {"error":"AI_SERVICE_ERROR"}\n\n` event is sent and the connection is closed

### Requirement: Input length validation
The system SHALL reject requests where text fields exceed maximum lengths: JD 20000 chars, resume 20000 chars, interview answer 5000 chars.

#### Scenario: Oversized answer submission
- **WHEN** client submits an interview answer longer than 5000 characters
- **THEN** the system responds with HTTP 400 and error code `INPUT_TOO_LONG`
