# MiMo Provider Integration Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Xiaomi MiMo (Token Plan) as a third AI provider alongside Zhipu and OpenRouter.

**Architecture:** MiMo exposes an OpenAI-compatible API at `https://token-plan-cn.xiaomimimo.com/v1` with standard `/chat/completions` path. It plugs into the existing provider architecture identically to zhipu/openrouter — no new abstractions needed. The backend stores per-provider API keys (`api-key.mimo` in settings.properties), and the frontend auto-renders a new provider tab from `PROVIDER_PRESETS`.

**Tech Stack:** Spring AI (OpenAI-compatible), Vue 3 + Pinia, Tailwind CSS

---

## File Structure

| File | Action | Responsibility |
|------|--------|----------------|
| `backend/src/main/java/com/interviewassistant/ai/config/AiConfig.java` | Modify | Add mimo constants, whitelist entry, URL/path/model mappings |
| `backend/src/main/java/com/interviewassistant/service/SettingsService.java` | Modify | Add mimo model list, env key mapping |
| `backend/src/main/java/com/interviewassistant/ai/circuitbreaker/FallbackService.java` | Modify | Add mimo to fallback chain |
| `backend/src/main/resources/application.yml` | Modify | Add `MIMO_API_KEY` env var |
| `frontend/src/stores/settingsStore.ts` | Modify | Add mimo entry to `PROVIDER_PRESETS` |

---

### Task 1: Add MiMo provider constants and whitelist to AiConfig

**Files:**
- Modify: `backend/src/main/java/com/interviewassistant/ai/config/AiConfig.java`

- [ ] **Step 1: Add MiMo constants**

Add after the existing `OPENROUTER_DEFAULT_MODEL` line (line 39):

```java
public static final String PROVIDER_MIMO = "mimo";
private static final String MIMO_BASE_URL = "https://token-plan-cn.xiaomimimo.com/v1";
private static final String MIMO_COMPLETIONS_PATH = "/chat/completions";
public static final String MIMO_DEFAULT_MODEL = "mimo-v2-flash";
```

- [ ] **Step 2: Add mimo to normalizeProvider whitelist**

In `normalizeProvider()` (line 158), change the switch expression from:

```java
case PROVIDER_ZHIPU, PROVIDER_OPENROUTER -> normalized;
```

to:

```java
case PROVIDER_ZHIPU, PROVIDER_OPENROUTER, PROVIDER_MIMO -> normalized;
```

- [ ] **Step 3: Update defaultModelFor to handle mimo**

In `defaultModelFor()` (line 164), change from:

```java
return PROVIDER_OPENROUTER.equals(normalizeProvider(provider))
        ? OPENROUTER_DEFAULT_MODEL
        : DEFAULT_MODEL;
```

to:

```java
String p = normalizeProvider(provider);
if (PROVIDER_OPENROUTER.equals(p)) return OPENROUTER_DEFAULT_MODEL;
if (PROVIDER_MIMO.equals(p)) return MIMO_DEFAULT_MODEL;
return DEFAULT_MODEL;
```

- [ ] **Step 4: Update baseUrlFor to handle mimo**

In `baseUrlFor()` (line 170), change from:

```java
return PROVIDER_OPENROUTER.equals(provider) ? OPENROUTER_BASE_URL : ZHIPU_BASE_URL;
```

to:

```java
if (PROVIDER_OPENROUTER.equals(provider)) return OPENROUTER_BASE_URL;
if (PROVIDER_MIMO.equals(provider)) return MIMO_BASE_URL;
return ZHIPU_BASE_URL;
```

- [ ] **Step 5: Update completionsPathFor to handle mimo**

In `completionsPathFor()` (line 174), change from:

```java
return PROVIDER_OPENROUTER.equals(provider) ? OPENROUTER_COMPLETIONS_PATH : ZHIPU_COMPLETIONS_PATH;
```

to:

```java
if (PROVIDER_OPENROUTER.equals(provider)) return OPENROUTER_COMPLETIONS_PATH;
if (PROVIDER_MIMO.equals(provider)) return MIMO_COMPLETIONS_PATH;
return ZHIPU_COMPLETIONS_PATH;
```

- [ ] **Step 6: No changes needed to requestFactoryFor or refreshClient**

MiMo uses the standard `HttpComponentsClientHttpRequestFactory` (same as zhipu) — no proxy, no special headers. The existing `requestFactoryFor()` falls through to the zhipu branch for non-openrouter providers, which is correct for mimo.

The `refreshClient()` method only has openrouter-specific branches (`if (PROVIDER_OPENROUTER.equals(...))`). Mimo will use the default path, which builds a standard `OpenAiApi` + `OpenAiChatModel` + `ChatClient` without extra headers or retry template. No changes needed.

- [ ] **Step 7: Verify compilation**

Run: `cd backend && ./mvnw compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 8: Commit**

```bash
git add backend/src/main/java/com/interviewassistant/ai/config/AiConfig.java
git commit -m "feat(mimo): add MiMo provider constants and whitelist to AiConfig"
```

---

### Task 2: Add MiMo model options and env key to SettingsService

**Files:**
- Modify: `backend/src/main/java/com/interviewassistant/service/SettingsService.java`

- [ ] **Step 1: Add MIMO_MODEL_OPTIONS list**

Add after the `OPENROUTER_MODEL_OPTIONS` list (after line 58):

```java
private static final List<String> MIMO_MODEL_OPTIONS = List.of(
        "mimo-v2-flash",
        "mimo-v2.5",
        "mimo-v2-omni",
        "mimo-v2.5-pro",
        "mimo-v2-pro"
);
```

- [ ] **Step 2: Update getModelOptions to return mimo models**

In `getModelOptions(String provider)` (line 136), change from:

```java
return AiConfig.PROVIDER_OPENROUTER.equals(AiConfig.normalizeProvider(provider))
        ? OPENROUTER_MODEL_OPTIONS
        : ZHIPU_MODEL_OPTIONS;
```

to:

```java
String p = AiConfig.normalizeProvider(provider);
if (AiConfig.PROVIDER_OPENROUTER.equals(p)) return OPENROUTER_MODEL_OPTIONS;
if (AiConfig.PROVIDER_MIMO.equals(p)) return MIMO_MODEL_OPTIONS;
return ZHIPU_MODEL_OPTIONS;
```

- [ ] **Step 3: Add mimo case to envKeyForProvider**

In `envKeyForProvider()` (line 300), change from:

```java
return switch (provider) {
    case AiConfig.PROVIDER_OPENROUTER -> "OPENROUTER_API_KEY";
    case AiConfig.PROVIDER_ZHIPU -> "spring.ai.openai.api-key";
    default -> "spring.ai.openai.api-key";
};
```

to:

```java
return switch (provider) {
    case AiConfig.PROVIDER_OPENROUTER -> "OPENROUTER_API_KEY";
    case AiConfig.PROVIDER_MIMO -> "app.mimo.api-key";
    case AiConfig.PROVIDER_ZHIPU -> "spring.ai.openai.api-key";
    default -> "spring.ai.openai.api-key";
};
```

- [ ] **Step 4: Verify compilation**

Run: `cd backend && ./mvnw compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/interviewassistant/service/SettingsService.java
git commit -m "feat(mimo): add MiMo model options and env key mapping to SettingsService"
```

---

### Task 3: Add MiMo to fallback chain in FallbackService

**Files:**
- Modify: `backend/src/main/java/com/interviewassistant/ai/circuitbreaker/FallbackService.java`

- [ ] **Step 1: Add mimo fallback logic**

In `fallback()` method (line 25), add a mimo branch. The full method becomes:

```java
public String fallback(String currentModel) {
    String currentProvider = settingsService.getCurrentProvider();

    if (AiConfig.PROVIDER_OPENROUTER.equals(currentProvider)) {
        String candidate = findHealthyOpenRouterModel(currentModel);
        if (candidate != null) {
            switchModel(AiConfig.PROVIDER_OPENROUTER, candidate);
            return candidate;
        }
        // All OpenRouter models OPEN -> try MiMo
        String mimoDefault = AiConfig.MIMO_DEFAULT_MODEL;
        if (!circuitBreaker.isOpen(mimoDefault)) {
            switchModel(AiConfig.PROVIDER_MIMO, mimoDefault);
            return mimoDefault;
        }
        // MiMo also OPEN -> fall back to ZhiPu
        log.warn("All OpenRouter and MiMo models OPEN, falling back to ZhiPu {}", AiConfig.DEFAULT_MODEL);
        if (!circuitBreaker.isOpen(AiConfig.DEFAULT_MODEL)) {
            switchModel(AiConfig.PROVIDER_ZHIPU, AiConfig.DEFAULT_MODEL);
            return AiConfig.DEFAULT_MODEL;
        }
    } else if (AiConfig.PROVIDER_MIMO.equals(currentProvider)) {
        // MiMo model failed -> try other MiMo models
        String candidate = findHealthyModel(AiConfig.PROVIDER_MIMO, currentModel);
        if (candidate != null) {
            switchModel(AiConfig.PROVIDER_MIMO, candidate);
            return candidate;
        }
        // All MiMo models OPEN -> fall back to ZhiPu
        log.warn("All MiMo models OPEN, falling back to ZhiPu {}", AiConfig.DEFAULT_MODEL);
        if (!circuitBreaker.isOpen(AiConfig.DEFAULT_MODEL)) {
            switchModel(AiConfig.PROVIDER_ZHIPU, AiConfig.DEFAULT_MODEL);
            return AiConfig.DEFAULT_MODEL;
        }
    } else {
        // ZhiPu model failed -> try MiMo first, then OpenRouter
        String mimoDefault = AiConfig.MIMO_DEFAULT_MODEL;
        if (!circuitBreaker.isOpen(mimoDefault)) {
            switchModel(AiConfig.PROVIDER_MIMO, mimoDefault);
            return mimoDefault;
        }
        String candidate = findHealthyOpenRouterModel(null);
        if (candidate != null) {
            switchModel(AiConfig.PROVIDER_OPENROUTER, candidate);
            return candidate;
        }
    }

    log.error("All models are OPEN, no fallback available");
    return null;
}
```

- [ ] **Step 2: Add generic findHealthyModel helper**

Add a new private method alongside `findHealthyOpenRouterModel`:

```java
private String findHealthyModel(String provider, String excludeModel) {
    List<String> models = settingsService.getModelOptions(provider);
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
```

- [ ] **Step 3: Verify compilation**

Run: `cd backend && ./mvnw compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Run existing circuit breaker tests**

Run: `cd backend && ./mvnw test -pl . -Dtest=ModelCircuitBreakerTest -q`
Expected: All tests pass (no test changes needed — MiMo doesn't change circuit breaker behavior)

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/interviewassistant/ai/circuitbreaker/FallbackService.java
git commit -m "feat(mimo): add MiMo to circuit breaker fallback chain"
```

---

### Task 4: Add MIMO_API_KEY environment variable to application.yml

**Files:**
- Modify: `backend/src/main/resources/application.yml`

- [ ] **Step 1: Add mimo env var reference**

Add after the `openrouter` section (after line 14):

```yaml
  mimo:
    api-key: ${MIMO_API_KEY:not-configured}
```

The `app:` section becomes:

```yaml
app:
  ai:
    sync-timeout-ms: ${AI_SYNC_TIMEOUT_MS:120000}
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:5173,http://localhost:8080}
  prompts:
    directory: ${PROMPT_DIR:prompts}
  settings:
    file: ${SETTINGS_FILE:}
  openrouter:
    proxy: ${OPENROUTER_PROXY:}
  mimo:
    api-key: ${MIMO_API_KEY:not-configured}
  smart-embedding:
    endpoint: ${SMART_EMBEDDING_ENDPOINT:http://127.0.0.1:8765/embed}
```

- [ ] **Step 2: Update envKeyForProvider in SettingsService to use this property**

The `envKeyForProvider` from Task 2 returns `"MIMO_API_KEY"` for mimo, but the Spring environment property path should be `app.mimo.api-key`. Update the mimo case in `envKeyForProvider()`:

```java
case AiConfig.PROVIDER_MIMO -> "app.mimo.api-key";
```

- [ ] **Step 3: Verify compilation**

Run: `cd backend && ./mvnw compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/resources/application.yml backend/src/main/java/com/interviewassistant/service/SettingsService.java
git commit -m "feat(mimo): add MIMO_API_KEY env var and application.yml config"
```

---

### Task 5: Add MiMo provider to frontend settingsStore

**Files:**
- Modify: `frontend/src/stores/settingsStore.ts`

- [ ] **Step 1: Add mimo entry to PROVIDER_PRESETS**

In `PROVIDER_PRESETS` (line 6), add after the `openrouter` entry:

```typescript
  mimo: {
    label: '小米 MiMo',
    docsUrl: 'https://platform.xiaomimimo.com/docs/zh-CN/quickstart/first-api-call',
    docsLabel: '获取 API Key',
    modelPlaceholder: '例如 mimo-v2-flash',
    defaultModel: 'mimo-v2-flash',
    apiKeyHelp: 'Token Plan 订阅模式，API Key 格式 tp-xxxxx，Base URL: token-plan-cn.xiaomimimo.com',
  },
```

The full `PROVIDER_PRESETS` object becomes:

```typescript
const PROVIDER_PRESETS = {
  zhipu: {
    label: '智谱 AI',
    docsUrl: 'https://open.bigmodel.cn/',
    docsLabel: '获取 API Key',
    modelPlaceholder: '例如 glm-4-flash',
    defaultModel: 'glm-4-flash',
    apiKeyHelp: '适合继续使用智谱的 OpenAI 兼容接口。',
  },
  openrouter: {
    label: 'OpenRouter',
    docsUrl: 'https://openrouter.ai/docs/quick-start',
    docsLabel: '查看接入文档',
    modelPlaceholder: '例如 openrouter/free 或 qwen/qwen3-coder:free',
    defaultModel: 'openrouter/free',
    apiKeyHelp: '可直接使用 OpenAI 兼容接口；免费模型更适合低频测试和原型验证。',
  },
  mimo: {
    label: '小米 MiMo',
    docsUrl: 'https://platform.xiaomimimo.com/docs/zh-CN/quickstart/first-api-call',
    docsLabel: '获取 API Key',
    modelPlaceholder: '例如 mimo-v2-flash',
    defaultModel: 'mimo-v2-flash',
    apiKeyHelp: 'Token Plan 订阅模式，API Key 格式 tp-xxxxx，Base URL: token-plan-cn.xiaomimimo.com',
  },
} as const
```

No other changes needed — the UI auto-renders from `PROVIDER_PRESETS` keys via `providerOptions` computed property, and `selectProvider` validates against `PROVIDER_PRESETS`.

- [ ] **Step 2: Verify frontend builds**

Run: `cd frontend && npm run build`
Expected: Build succeeds with no TypeScript errors

- [ ] **Step 3: Commit**

```bash
git add frontend/src/stores/settingsStore.ts
git commit -m "feat(mimo): add MiMo provider preset to frontend settings store"
```

---

### Task 6: End-to-end verification

**Files:** None (verification only)

- [ ] **Step 1: Full backend compilation and tests**

Run: `cd backend && ./mvnw test -q`
Expected: All tests pass

- [ ] **Step 2: Frontend build**

Run: `cd frontend && npm run build`
Expected: Build succeeds

- [ ] **Step 3: Manual smoke test**

1. Start backend: `cd backend && mvn spring-boot:run`
2. Start frontend: `cd frontend && npm run dev`
3. Open Settings page in browser
4. Verify "小米 MiMo" button appears in the provider selector
5. Click it, verify model dropdown shows: mimo-v2-flash, mimo-v2.5, mimo-v2-omni, mimo-v2.5-pro, mimo-v2-pro
6. Enter a test API key (format `tp-xxxxx`), click save
7. Select a model, click save
8. Verify the settings persist after page refresh

---

## Self-Review Checklist

- [x] **Spec coverage:** Every requirement from the brainstorming discussion has a task
- [x] **Placeholder scan:** No TBD, TODO, or vague steps — all code is shown inline
- [x] **Type consistency:** All method names, constants, and property names are consistent across tasks
- [x] **No unnecessary changes:** AiGateway, SettingsController, DTOs, SettingsView.vue, settingsApi.ts all remain untouched — they are already provider-agnostic
