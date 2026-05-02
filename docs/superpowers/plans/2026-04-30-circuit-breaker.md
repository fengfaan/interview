# Circuit Breaker & Model Fallback Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add circuit breaker for all models with automatic fallback — OpenRouter free models switch among themselves first, then fall back to ZhiPu glm-4-flash as last resort.

**Architecture:** In-memory `ModelCircuitBreaker` service tracks per-model health state (CLOSED/OPEN/HALF_OPEN). `AiGateway` records call outcomes and checks health before each call. When the current model is OPEN, `FallbackService` picks the next healthy model or falls back to ZhiPu. A new REST endpoint exposes model health status.

**Tech Stack:** Java 17, Spring Boot 3.3, no external dependencies

---

## File Structure

| Action | File | Responsibility |
|--------|------|----------------|
| Create | `dto/settings/ModelHealthEntry.java` | DTO for model health API |
| Create | `ai/circuitbreaker/CircuitState.java` | Enum: CLOSED, OPEN, HALF_OPEN |
| Create | `ai/circuitbreaker/ModelCircuitState.java` | Per-model state record: failureCount, openedAt, state |
| Create | `ai/circuitbreaker/ModelCircuitBreaker.java` | Core circuit breaker logic: record success/failure, state transitions |
| Create | `ai/circuitbreaker/FallbackService.java` | Selects next healthy model, triggers model switch via SettingsService |
| Modify | `ai/gateway/AiGateway.java` | Integrate circuit breaker: record outcomes, check before calls, trigger fallback |
| Modify | `controller/SettingsController.java` | Add GET `/api/settings/model-health` endpoint |
| Create | `ai/circuitbreaker/ModelCircuitBreakerTest.java` | Unit tests for circuit breaker state machine |

---

### Task 1: Circuit State Enum and Per-Model State Record

**Files:**
- Create: `src/main/java/com/interviewassistant/ai/circuitbreaker/CircuitState.java`
- Create: `src/main/java/com/interviewassistant/ai/circuitbreaker/ModelCircuitState.java`

- [ ] **Step 1: Create CircuitState enum**

```java
package com.interviewassistant.ai.circuitbreaker;

public enum CircuitState {
    CLOSED,
    OPEN,
    HALF_OPEN
}
```

- [ ] **Step 2: Create ModelCircuitState class**

```java
package com.interviewassistant.ai.circuitbreaker;

import lombok.Data;

@Data
public class ModelCircuitState {
    private CircuitState state = CircuitState.CLOSED;
    private int failureCount = 0;
    private long openedAt = 0;

    public void recordSuccess() {
        this.failureCount = 0;
        this.state = CircuitState.CLOSED;
    }

    public void recordFailure() {
        this.failureCount++;
    }

    public void open() {
        this.state = CircuitState.OPEN;
        this.openedAt = System.currentTimeMillis();
    }

    public void halfOpen() {
        this.state = CircuitState.HALF_OPEN;
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/interviewassistant/ai/circuitbreaker/CircuitState.java \
        src/main/java/com/interviewassistant/ai/circuitbreaker/ModelCircuitState.java
git commit -m "feat(circuit-breaker): add CircuitState enum and ModelCircuitState"
```

---

### Task 2: ModelCircuitBreaker Service

**Files:**
- Create: `src/main/java/com/interviewassistant/ai/circuitbreaker/ModelCircuitBreaker.java`
- Create: `src/test/java/com/interviewassistant/ai/circuitbreaker/ModelCircuitBreakerTest.java`

- [ ] **Step 1: Write failing tests**

```java
package com.interviewassistant.ai.circuitbreaker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModelCircuitBreakerTest {

    private static final int FAILURE_THRESHOLD = 3;
    private static final long OPEN_DURATION_MS = 30_000;
    private ModelCircuitBreaker breaker;

    @BeforeEach
    void setUp() {
        breaker = new ModelCircuitBreaker(FAILURE_THRESHOLD, OPEN_DURATION_MS);
    }

    @Test
    void initialStateIsClosed() {
        assertEquals(CircuitState.CLOSED, breaker.getState("model-a"));
        assertEquals(0, breaker.getFailureCount("model-a"));
    }

    @Test
    void failureCountIncrementsOnRecordFailure() {
        breaker.recordFailure("model-a");
        assertEquals(1, breaker.getFailureCount("model-a"));
        assertEquals(CircuitState.CLOSED, breaker.getState("model-a"));
    }

    @Test
    void opensAfterThresholdFailures() {
        for (int i = 0; i < FAILURE_THRESHOLD; i++) {
            breaker.recordFailure("model-a");
        }
        assertEquals(CircuitState.OPEN, breaker.getState("model-a"));
        assertTrue(breaker.getOpenedAt("model-a") > 0);
    }

    @Test
    void isOpenReturnsFalseWhenClosed() {
        assertFalse(breaker.isOpen("model-a"));
    }

    @Test
    void isOpenReturnsTrueWhenOpen() {
        for (int i = 0; i < FAILURE_THRESHOLD; i++) {
            breaker.recordFailure("model-a");
        }
        assertTrue(breaker.isOpen("model-a"));
    }

    @Test
    void transitionsToHalfOpenAfterCoolDown() {
        // Use a breaker with 0ms cooldown for testing
        ModelCircuitBreaker fastBreaker = new ModelCircuitBreaker(FAILURE_THRESHOLD, 0);
        for (int i = 0; i < FAILURE_THRESHOLD; i++) {
            fastBreaker.recordFailure("model-a");
        }
        assertEquals(CircuitState.OPEN, fastBreaker.getState("model-a"));

        // After cooldown (0ms), isOpen triggers transition to HALF_OPEN
        assertFalse(fastBreaker.isOpen("model-a"));
        assertEquals(CircuitState.HALF_OPEN, fastBreaker.getState("model-a"));
    }

    @Test
    void halfOpenGoesToClosedOnSuccess() {
        ModelCircuitBreaker fastBreaker = new ModelCircuitBreaker(FAILURE_THRESHOLD, 0);
        for (int i = 0; i < FAILURE_THRESHOLD; i++) {
            fastBreaker.recordFailure("model-a");
        }
        fastBreaker.isOpen("model-a"); // triggers HALF_OPEN
        assertEquals(CircuitState.HALF_OPEN, fastBreaker.getState("model-a"));

        fastBreaker.recordSuccess("model-a");
        assertEquals(CircuitState.CLOSED, fastBreaker.getState("model-a"));
        assertEquals(0, fastBreaker.getFailureCount("model-a"));
    }

    @Test
    void halfOpenGoesBackToOpenOnFailure() {
        ModelCircuitBreaker fastBreaker = new ModelCircuitBreaker(FAILURE_THRESHOLD, 0);
        for (int i = 0; i < FAILURE_THRESHOLD; i++) {
            fastBreaker.recordFailure("model-a");
        }
        fastBreaker.isOpen("model-a"); // triggers HALF_OPEN

        fastBreaker.recordFailure("model-a");
        assertEquals(CircuitState.OPEN, fastBreaker.getState("model-a"));
    }

    @Test
    void recordSuccessResetsClosedState() {
        breaker.recordFailure("model-a");
        breaker.recordFailure("model-a");
        breaker.recordSuccess("model-a");
        assertEquals(CircuitState.CLOSED, breaker.getState("model-a"));
        assertEquals(0, breaker.getFailureCount("model-a"));
    }

    @Test
    void differentModelsAreIndependent() {
        breaker.recordFailure("model-a");
        breaker.recordFailure("model-a");
        assertEquals(2, breaker.getFailureCount("model-a"));
        assertEquals(0, breaker.getFailureCount("model-b"));
        assertEquals(CircuitState.CLOSED, breaker.getState("model-b"));
    }

    @Test
    void resetClearsAllState() {
        breaker.recordFailure("model-a");
        breaker.recordFailure("model-b");
        breaker.reset("model-a");
        assertEquals(0, breaker.getFailureCount("model-a"));
        assertEquals(CircuitState.CLOSED, breaker.getState("model-a"));
        assertEquals(1, breaker.getFailureCount("model-b"));
    }

    @Test
    void isOpenStaysOpenWithinCooldown() {
        for (int i = 0; i < FAILURE_THRESHOLD; i++) {
            breaker.recordFailure("model-a");
        }
        // Still within 30s cooldown
        assertTrue(breaker.isOpen("model-a"));
        assertEquals(CircuitState.OPEN, breaker.getState("model-a"));
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd /Users/fengfan/interviewAssistant/backend && ./mvnw test -pl . -Dtest=ModelCircuitBreakerTest -Dsurefire.failIfNoSpecifiedTests=false 2>&1 | tail -20`
Expected: Compilation errors (classes don't exist yet)

- [ ] **Step 3: Implement ModelCircuitBreaker**

```java
package com.interviewassistant.ai.circuitbreaker;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ModelCircuitBreaker {

    private static final int DEFAULT_FAILURE_THRESHOLD = 3;
    private static final long DEFAULT_OPEN_DURATION_MS = 30_000;

    private final int failureThreshold;
    private final long openDurationMs;
    private final ConcurrentHashMap<String, ModelCircuitState> states = new ConcurrentHashMap<>();

    public ModelCircuitBreaker() {
        this(DEFAULT_FAILURE_THRESHOLD, DEFAULT_OPEN_DURATION_MS);
    }

    public ModelCircuitBreaker(int failureThreshold, long openDurationMs) {
        this.failureThreshold = failureThreshold;
        this.openDurationMs = openDurationMs;
    }

    private ModelCircuitState getOrCreate(String model) {
        return states.computeIfAbsent(model, k -> new ModelCircuitState());
    }

    public CircuitState getState(String model) {
        return getOrCreate(model).getState();
    }

    public int getFailureCount(String model) {
        return getOrCreate(model).getFailureCount();
    }

    public long getOpenedAt(String model) {
        return getOrCreate(model).getOpenedAt();
    }

    public boolean isOpen(String model) {
        ModelCircuitState state = getOrCreate(model);
        if (state.getState() == CircuitState.CLOSED) {
            return false;
        }
        if (state.getState() == CircuitState.OPEN) {
            long elapsed = System.currentTimeMillis() - state.getOpenedAt();
            if (elapsed >= openDurationMs) {
                state.halfOpen();
                log.info("Model [{}] circuit transitioned to HALF_OPEN after {}ms cooldown", model, elapsed);
                return false;
            }
            return true;
        }
        // HALF_OPEN: allow one probe request
        return false;
    }

    public void recordSuccess(String model) {
        ModelCircuitState state = getOrCreate(model);
        CircuitState previous = state.getState();
        state.recordSuccess();
        if (previous != CircuitState.CLOSED) {
            log.info("Model [{}] circuit CLOSED after successful call (was {})", model, previous);
        }
    }

    public void recordFailure(String model) {
        ModelCircuitState state = getOrCreate(model);
        if (state.getState() == CircuitState.HALF_OPEN) {
            state.open();
            log.warn("Model [{}] HALF_OPEN probe failed, back to OPEN", model);
            return;
        }
        state.recordFailure();
        if (state.getFailureCount() >= failureThreshold && state.getState() != CircuitState.OPEN) {
            state.open();
            log.warn("Model [{}] circuit OPEN after {} consecutive failures", model, failureThreshold);
        }
    }

    public void reset(String model) {
        states.remove(model);
    }

    public List<ModelHealthEntry> getHealthForAll(List<String> models) {
        return models.stream().map(m -> {
            ModelCircuitState s = states.get(m);
            if (s == null) {
                return new ModelHealthEntry(m, CircuitState.CLOSED, 0, null);
            }
            String openedAtIso = s.getOpenedAt() > 0
                    ? Instant.ofEpochMilli(s.getOpenedAt()).toString()
                    : null;
            return new ModelHealthEntry(m, s.getState(), s.getFailureCount(), openedAtIso);
        }).collect(Collectors.toList());
    }

    public record ModelHealthEntry(String model, CircuitState state, int failureCount, String openedAt) {
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd /Users/fengfan/interviewAssistant/backend && ./mvnw test -pl . -Dtest=ModelCircuitBreakerTest 2>&1 | tail -20`
Expected: All 12 tests PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/interviewassistant/ai/circuitbreaker/ModelCircuitBreaker.java \
        src/test/java/com/interviewassistant/ai/circuitbreaker/ModelCircuitBreakerTest.java
git commit -m "feat(circuit-breaker): add ModelCircuitBreaker with state machine and tests"
```

---

### Task 3: FallbackService

**Files:**
- Create: `src/main/java/com/interviewassistant/ai/circuitbreaker/FallbackService.java`

- [ ] **Step 1: Implement FallbackService**

```java
package com.interviewassistant.ai.circuitbreaker;

import com.interviewassistant.ai.config.AiConfig;
import com.interviewassistant.service.SettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FallbackService {

    private final ModelCircuitBreaker circuitBreaker;
    private final SettingsService settingsService;

    /**
     * Find a healthy alternative model and switch to it.
     * Priority: other OpenRouter free models -> ZhiPu glm-4-flash.
     *
     * @return the model name switched to, or null if all models are down
     */
    public String fallback(String currentModel) {
        String currentProvider = settingsService.getCurrentProvider();

        if (AiConfig.PROVIDER_OPENROUTER.equals(currentProvider)) {
            String candidate = findHealthyOpenRouterModel(currentModel);
            if (candidate != null) {
                switchModel(AiConfig.PROVIDER_OPENROUTER, candidate);
                return candidate;
            }
            // All OpenRouter models OPEN -> fall back to ZhiPu
            log.warn("All OpenRouter free models OPEN, falling back to ZhiPu {}", AiConfig.DEFAULT_MODEL);
            if (!circuitBreaker.isOpen(AiConfig.DEFAULT_MODEL)) {
                switchModel(AiConfig.PROVIDER_ZHIPU, AiConfig.DEFAULT_MODEL);
                return AiConfig.DEFAULT_MODEL;
            }
        } else {
            // ZhiPu model failed -> try OpenRouter free models
            String candidate = findHealthyOpenRouterModel(null);
            if (candidate != null) {
                switchModel(AiConfig.PROVIDER_OPENROUTER, candidate);
                return candidate;
            }
        }

        log.error("All models are OPEN, no fallback available");
        return null;
    }

    private String findHealthyOpenRouterModel(String excludeModel) {
        List<String> models = settingsService.getModelOptions(AiConfig.PROVIDER_OPENROUTER);
        for (String model : models) {
            if (model.equals(excludeModel)) {
                continue;
            }
            if (!circuitBreaker.isOpen(model)) {
                return model;
            }
        }
        return null;
    }

    private void switchModel(String provider, String model) {
        log.warn("Circuit breaker triggered, switching model: provider={}, model={}", provider, model);
        settingsService.saveModel(provider, model);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/interviewassistant/ai/circuitbreaker/FallbackService.java
git commit -m "feat(circuit-breaker): add FallbackService for model switching"
```

---

### Task 4: Integrate Circuit Breaker into AiGateway

**Files:**
- Modify: `src/main/java/com/interviewassistant/ai/gateway/AiGateway.java`

- [ ] **Step 1: Add circuit breaker fields and imports to AiGateway**

Add imports at the top of AiGateway.java (after existing imports):

```java
import com.interviewassistant.ai.circuitbreaker.ModelCircuitBreaker;
import com.interviewassistant.ai.circuitbreaker.FallbackService;
```

Add fields (after `private final ObjectMapper objectMapper;`):

```java
private final ModelCircuitBreaker circuitBreaker;
private final FallbackService fallbackService;
```

Note: `@RequiredArgsConstructor` from Lombok will auto-include these new final fields in the constructor.

- [ ] **Step 2: Add checkAndFallback helper method**

Add this method after the `callWithTimeout` method (around line 172):

```java
private void ensureModelAvailable() {
    String currentModel = aiConfig.getCurrentModel();
    if (circuitBreaker.isOpen(currentModel)) {
        String fallback = fallbackService.fallback(currentModel);
        if (fallback == null) {
            throw new RuntimeException("所有 AI 模型均不可用，请稍后重试");
        }
    }
}

private void recordSuccess() {
    circuitBreaker.recordSuccess(aiConfig.getCurrentModel());
}

private boolean isRetryableError(Throwable error) {
    return AiErrorUtils.isRateLimit(error) || AiErrorUtils.isNetworkError(error)
            || is5xxError(error);
}

private boolean is5xxError(Throwable error) {
    Throwable current = error;
    while (current != null) {
        String message = current.getMessage();
        if (message != null) {
            String normalized = message.toLowerCase();
            if (normalized.contains("500") || normalized.contains("502")
                    || normalized.contains("503") || normalized.contains("504")) {
                return true;
            }
        }
        current = current.getCause();
    }
    return false;
}

private void recordIfRetryable(Throwable error) {
    if (isRetryableError(error)) {
        circuitBreaker.recordFailure(aiConfig.getCurrentModel());
    }
}
```

- [ ] **Step 3: Wrap generateJson with circuit breaker logic**

Replace the existing `generateJson` method (lines 45-83) with:

```java
public <T> JsonResult<T> generateJson(String systemPrompt, String userPrompt, Class<T> responseType) {
    ensureModelAvailable();
    var converter = new BeanOutputConverter<>(responseType);
    AiResponseFormatException lastFormatError = null;
    for (int attempt = 1; attempt <= JSON_GENERATION_ATTEMPTS; attempt++) {
        try {
            ChatResponse response = callWithTimeout(() -> aiConfig.getCurrentChatClient().prompt()
                            .system(systemPrompt)
                            .user(userPrompt + "\n\n" + converter.getFormat())
                            .call()
                            .chatResponse(),
                    "结构化 AI 生成");
            String rawText = response.getResult().getOutput().getText();
            String json = JsonOutputUtils.extractJson(rawText);
            if (json.isBlank()) {
                throw new AiResponseFormatException(
                        "AI_EMPTY_RESPONSE",
                        "AI 返回了空内容，请稍后重试，或在设置中切换到更稳定的模型。"
                );
            }
            T value = objectMapper.readValue(json, responseType);
            String actualModel = response.getMetadata() != null ? response.getMetadata().getModel() : null;
            recordSuccess();
            return new JsonResult<>(value, actualModel);
        } catch (AiResponseFormatException e) {
            lastFormatError = e;
        } catch (Exception e) {
            recordIfRetryable(e);
            lastFormatError = new AiResponseFormatException(
                    "AI_RESPONSE_FORMAT_ERROR",
                    "AI 返回内容格式不符合要求，请重试；如果频繁出现，请切换模型或优化提示词。",
                    e
            );
        }

        if (attempt < JSON_GENERATION_ATTEMPTS) {
            log.warn("AI JSON response parse failed, retrying. attempt={}/{}, reason={}",
                    attempt, JSON_GENERATION_ATTEMPTS, lastFormatError.getMessage());
        }
    }
    throw lastFormatError;
}
```

- [ ] **Step 4: Wrap generateText methods with circuit breaker logic**

Replace the existing `generateText(String)` method (lines 85-86) with:

```java
public String generateText(String userPrompt) {
    ensureModelAvailable();
    try {
        String result = callWithTimeout(() -> aiConfig.getCurrentChatClient().prompt()
                        .user(userPrompt)
                        .call()
                        .content(),
                "文本 AI 生成");
        recordSuccess();
        return result;
    } catch (Exception e) {
        recordIfRetryable(e);
        throw e;
    }
}
```

Replace the existing `generateText(String, String)` method (lines 93-99) with:

```java
public String generateText(String systemPrompt, String userPrompt) {
    ensureModelAvailable();
    try {
        String result = callWithTimeout(() -> aiConfig.getCurrentChatClient().prompt()
                        .system(systemPrompt)
                        .user(userPrompt)
                        .call()
                        .content(),
                "文本 AI 生成");
        recordSuccess();
        return result;
    } catch (Exception e) {
        recordIfRetryable(e);
        throw e;
    }
}
```

- [ ] **Step 5: Wrap callWithTools with circuit breaker logic**

Replace the existing `callWithTools` method (lines 102-111) with:

```java
public ChatResponse callWithTools(List<Message> messages, ToolCallback... toolCallbacks) {
    ensureModelAvailable();
    try {
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(aiConfig.getCurrentModel())
                .temperature(0.7)
                .toolCallbacks(toolCallbacks)
                .internalToolExecutionEnabled(false)
                .build();
        Prompt prompt = new Prompt(messages, options);
        ChatResponse response = callWithTimeout(() -> aiConfig.getCurrentChatModel().call(prompt), "工具调用 AI 生成");
        recordSuccess();
        return response;
    } catch (Exception e) {
        recordIfRetryable(e);
        throw e;
    }
}
```

- [ ] **Step 6: Wrap streamText with circuit breaker logic**

Replace the existing `streamText` method (lines 113-154) with:

```java
public void streamText(SseEmitter emitter, String systemPrompt, String userPrompt,
                       String failureMessage, String startupFailureMessage) {
    ensureModelAvailable();
    try {
        AtomicBoolean closed = new AtomicBoolean(false);
        Disposable subscription = aiConfig.getCurrentChatClient().prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .stream()
                .content()
                .subscribe(
                        chunk -> {
                            if (!closed.get()) {
                                SseUtils.sendChunk(emitter, chunk);
                            }
                        },
                        error -> {
                            recordIfRetryable(error);
                            if (!closed.get()) {
                                sendStreamError(emitter, error, failureMessage);
                            } else {
                                log.debug("AI stream failed after SSE closed: {}", AiErrorUtils.compactMessage(error));
                            }
                        },
                        () -> {
                            recordSuccess();
                            if (!closed.get()) {
                                SseUtils.sendDone(emitter);
                            }
                        }
                );
        Runnable cancelUpstream = () -> {
            closed.set(true);
            subscription.dispose();
        };
        emitter.onTimeout(cancelUpstream);
        emitter.onCompletion(cancelUpstream);
        emitter.onError(error -> {
            log.debug("SSE stream closed before AI stream completed: {}", AiErrorUtils.compactMessage(error));
            cancelUpstream.run();
        });
    } catch (Exception e) {
        recordIfRetryable(e);
        sendStreamStartupError(emitter, e, startupFailureMessage);
    }
}
```

- [ ] **Step 7: Compile to verify no errors**

Run: `cd /Users/fengfan/interviewAssistant/backend && ./mvnw compile -q 2>&1 | tail -20`
Expected: BUILD SUCCESS

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/interviewassistant/ai/gateway/AiGateway.java
git commit -m "feat(circuit-breaker): integrate circuit breaker into AiGateway for all call patterns"
```

---

### Task 5: Model Health API Endpoint

**Files:**
- Create: `src/main/java/com/interviewassistant/dto/settings/ModelHealthEntry.java`
- Modify: `src/main/java/com/interviewassistant/controller/SettingsController.java`

- [ ] **Step 1: Create ModelHealthResponse DTO**

```java
package com.interviewassistant.dto.settings;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ModelHealthEntry {
    private String model;
    private String state;
    private int failureCount;
    private String openedAt;
}
```

- [ ] **Step 2: Add health endpoint to SettingsController**

Add import at top:

```java
import com.interviewassistant.ai.circuitbreaker.ModelCircuitBreaker;
import com.interviewassistant.dto.settings.ModelHealthEntry;
```

Add field:

```java
private final ModelCircuitBreaker circuitBreaker;
```

Add endpoint method (after the existing `saveModel` method around line 68):

```java
@GetMapping("/model-health")
public ApiResponse<List<ModelHealthEntry>> getModelHealth() {
    String provider = settingsService.getCurrentProvider();
    java.util.List<String> models = new java.util.ArrayList<>(settingsService.getModelOptions(provider));
    // Also include the other provider's models
    String otherProvider = AiConfig.PROVIDER_OPENROUTER.equals(provider)
            ? AiConfig.PROVIDER_ZHIPU : AiConfig.PROVIDER_OPENROUTER;
    models.addAll(settingsService.getModelOptions(otherProvider));
    List<ModelHealthEntry> health = circuitBreaker.getHealthForAll(models).stream()
            .map(e -> new ModelHealthEntry(e.model(), e.state().name(), e.failureCount(), e.openedAt()))
            .collect(java.util.stream.Collectors.toList());
    return ApiResponse.ok(health);
}
```

- [ ] **Step 3: Compile to verify**

Run: `cd /Users/fengfan/interviewAssistant/backend && ./mvnw compile -q 2>&1 | tail -20`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/interviewassistant/dto/settings/ModelHealthEntry.java \
        src/main/java/com/interviewassistant/controller/SettingsController.java
git commit -m "feat(circuit-breaker): add GET /api/settings/model-health endpoint"
```

---

### Task 6: Run All Tests and Verify

**Files:** No new files

- [ ] **Step 1: Run full test suite**

Run: `cd /Users/fengfan/interviewAssistant/backend && ./mvnw test 2>&1 | tail -30`
Expected: All tests pass

- [ ] **Step 2: Start application and verify health endpoint**

Run: `cd /Users/fengfan/interviewAssistant/backend && ./mvnw spring-boot:run`

Then in another terminal: `curl -s http://localhost:8080/api/settings/model-health | python3 -m json.tool`

Expected: JSON array with all models showing `"state": "CLOSED"`, `"failureCount": 0`

- [ ] **Step 3: Final commit if any fixes needed**

```bash
git add -A
git commit -m "fix(circuit-breaker): fix integration issues from testing"
```