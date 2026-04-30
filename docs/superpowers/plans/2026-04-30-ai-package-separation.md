# AI Package Separation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move all AI-related classes into `com.interviewassistant.ai` package with sub-packages, separating AI concerns from business logic.

**Architecture:** Pure package refactoring — move 9 Java files from `config/`, `service/`, `common/`, `agent/`, `prompt/` into `ai/config/`, `ai/gateway/`, `ai/prompt/`, `ai/agent/`, `ai/exception/`, `ai/util/`. Update all imports in 12 consumer files. No logic changes.

**Tech Stack:** Java 17, Spring Boot 3.3, Maven

---

## File Structure

### Files to move (9 source + 1 test)

| Current Location | New Location |
|-----------------|--------------|
| `config/AiConfig.java` | `ai/config/AiConfig.java` |
| `service/AiGateway.java` | `ai/gateway/AiGateway.java` |
| `service/PromptService.java` | `ai/prompt/PromptService.java` |
| `prompt/InterviewLabels.java` | `ai/prompt/InterviewLabels.java` |
| `agent/DeepDiveAgent.java` | `ai/agent/DeepDiveAgent.java` |
| `agent/KnowledgeTools.java` | `ai/agent/KnowledgeTools.java` |
| `common/AiErrorUtils.java` | `ai/util/AiErrorUtils.java` |
| `common/JsonOutputUtils.java` | `ai/util/JsonOutputUtils.java` |
| `common/AiResponseFormatException.java` | `ai/exception/AiResponseFormatException.java` |
| `common/PromptLoadException.java` | `ai/exception/PromptLoadException.java` |

### Consumer files needing import updates (12 source + 5 test)

**Source:**
- `controller/SettingsController.java` — imports `AiConfig`, `AiGateway`, `PromptService`
- `controller/InterviewController.java` — imports `AiErrorUtils`
- `service/SettingsService.java` — imports `AiConfig`
- `service/InterviewAiService.java` — imports `AiErrorUtils`, `JsonOutputUtils`, `InterviewLabels`
- `service/InterviewStreamService.java` — imports `DeepDiveAgent`
- `service/BatchQuestionStreamService.java` — imports `AiErrorUtils`
- `service/AiGateway.java` (self) — imports `AiConfig`, `AiErrorUtils`, `AiResponseFormatException`, `JsonOutputUtils`
- `service/PromptService.java` (self) — imports `PromptLoadException`
- `ai/agent/DeepDiveAgent.java` (moved) — imports `AiGateway`, `PromptService`
- `ai/agent/KnowledgeTools.java` (moved) — imports `ObsidianService`, `NoteItem`
- `ai/gateway/AiGateway.java` (moved) — imports `AiConfig`, `SseUtils`, `AiErrorUtils`, `AiResponseFormatException`, `JsonOutputUtils`

**Test:**
- `test/agent/DeepDiveAgentTest.java` → `test/ai/agent/DeepDiveAgentTest.java` — imports `AiGateway`, `PromptService`, `ObsidianService`, `KnowledgeTools`
- `test/service/InterviewAiServiceBatchTest.java` — imports `InterviewLabels`
- `test/service/InterviewAiServiceDeepDiveTest.java` — imports `AiErrorUtils` (indirect via `InterviewAiService`)
- `test/controller/InterviewControllerBatchTest.java` — may reference moved types
- `test/controller/InterviewControllerDeepDiveTest.java` — may reference moved types

---

### Task 1: Move AI utility and exception classes

Move the leaf classes with no cross-dependencies within the AI group: `AiErrorUtils`, `JsonOutputUtils`, `AiResponseFormatException`, `PromptLoadException`.

**Files:**
- Move: `common/AiErrorUtils.java` → `ai/util/AiErrorUtils.java`
- Move: `common/JsonOutputUtils.java` → `ai/util/JsonOutputUtils.java`
- Move: `common/AiResponseFormatException.java` → `ai/exception/AiResponseFormatException.java`
- Move: `common/PromptLoadException.java` → `ai/exception/PromptLoadException.java`

- [ ] **Step 1: Create target directories and move files**

```bash
cd /Users/fengfan/interviewAssistant/backend
mkdir -p src/main/java/com/interviewassistant/ai/util
mkdir -p src/main/java/com/interviewassistant/ai/exception
```

For each file, move it and update its `package` declaration:

- `common/AiErrorUtils.java` → `ai/util/AiErrorUtils.java`: change `package com.interviewassistant.common` → `package com.interviewassistant.ai.util`
- `common/JsonOutputUtils.java` → `ai/util/JsonOutputUtils.java`: change `package com.interviewassistant.common` → `package com.interviewassistant.ai.util`
- `common/AiResponseFormatException.java` → `ai/exception/AiResponseFormatException.java`: change `package com.interviewassistant.common` → `package com.interviewassistant.ai.exception`
- `common/PromptLoadException.java` → `ai/exception/PromptLoadException.java`: change `package com.interviewassistant.common` → `package com.interviewassistant.ai.exception`

- [ ] **Step 2: Update imports in consumer files**

Update these import statements in existing files:

**`service/AiGateway.java`:**
```
com.interviewassistant.common.AiErrorUtils → com.interviewassistant.ai.util.AiErrorUtils
com.interviewassistant.common.AiResponseFormatException → com.interviewassistant.ai.exception.AiResponseFormatException
com.interviewassistant.common.JsonOutputUtils → com.interviewassistant.ai.util.JsonOutputUtils
```

**`service/PromptService.java`:**
```
com.interviewassistant.common.PromptLoadException → com.interviewassistant.ai.exception.PromptLoadException
```

**`service/InterviewAiService.java`:**
```
com.interviewassistant.common.AiErrorUtils → com.interviewassistant.ai.util.AiErrorUtils
com.interviewassistant.common.JsonOutputUtils → com.interviewassistant.ai.util.JsonOutputUtils
```

**`service/BatchQuestionStreamService.java`:**
```
com.interviewassistant.common.AiErrorUtils → com.interviewassistant.ai.util.AiErrorUtils
```

**`controller/InterviewController.java`:**
```
com.interviewassistant.common.AiErrorUtils → com.interviewassistant.ai.util.AiErrorUtils
```

- [ ] **Step 3: Compile to verify**

Run: `cd /Users/fengfan/interviewAssistant/backend && ./mvnw compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
cd /Users/fengfan/interviewAssistant/backend && git add -A && git commit -m "refactor: move AI utility and exception classes to ai.util and ai.exception packages"
```

---

### Task 2: Move AiConfig, AiGateway, PromptService, InterviewLabels

Move the core AI infrastructure classes.

**Files:**
- Move: `config/AiConfig.java` → `ai/config/AiConfig.java`
- Move: `service/AiGateway.java` → `ai/gateway/AiGateway.java`
- Move: `service/PromptService.java` → `ai/prompt/PromptService.java`
- Move: `prompt/InterviewLabels.java` → `ai/prompt/InterviewLabels.java`

- [ ] **Step 1: Create target directories and move files**

```bash
cd /Users/fengfan/interviewAssistant/backend
mkdir -p src/main/java/com/interviewassistant/ai/config
mkdir -p src/main/java/com/interviewassistant/ai/gateway
mkdir -p src/main/java/com/interviewassistant/ai/prompt
```

For each file, move it and update its `package` declaration:

- `config/AiConfig.java` → `ai/config/AiConfig.java`: change `package com.interviewassistant.config` → `package com.interviewassistant.ai.config`
- `service/AiGateway.java` → `ai/gateway/AiGateway.java`: change `package com.interviewassistant.service` → `package com.interviewassistant.ai.gateway`
- `service/PromptService.java` → `ai/prompt/PromptService.java`: change `package com.interviewassistant.service` → `package com.interviewassistant.ai.prompt`
- `prompt/InterviewLabels.java` → `ai/prompt/InterviewLabels.java`: change `package com.interviewassistant.prompt` → `package com.interviewassistant.ai.prompt`

- [ ] **Step 2: Update imports in consumer files**

**`ai/gateway/AiGateway.java` (just moved):**
```
com.interviewassistant.config.AiConfig → com.interviewassistant.ai.config.AiConfig
```

**`ai/prompt/PromptService.java` (just moved):**
```
com.interviewassistant.ai.exception.PromptLoadException — already correct (updated in Task 1)
```

**`controller/SettingsController.java`:**
```
com.interviewassistant.config.AiConfig → com.interviewassistant.ai.config.AiConfig
com.interviewassistant.service.AiGateway → com.interviewassistant.ai.gateway.AiGateway
com.interviewassistant.service.PromptService → com.interviewassistant.ai.prompt.PromptService
```

**`service/SettingsService.java`:**
```
com.interviewassistant.config.AiConfig → com.interviewassistant.ai.config.AiConfig
```

**`service/InterviewAiService.java`:**
```
com.interviewassistant.prompt.InterviewLabels → com.interviewassistant.ai.prompt.InterviewLabels
```

**`service/InterviewStreamService.java`:**
```
com.interviewassistant.agent.DeepDiveAgent → com.interviewassistant.ai.agent.DeepDiveAgent (will be moved in Task 3, but update now)
```

Note: `InterviewStreamService.java` imports `DeepDiveAgent` from the old `agent` package. Update the import now to the new `ai.agent` package — it will be correct after Task 3.

- [ ] **Step 3: Compile to verify**

Run: `cd /Users/fengfan/interviewAssistant/backend && ./mvnw compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
cd /Users/fengfan/interviewAssistant/backend && git add -A && git commit -m "refactor: move AiConfig, AiGateway, PromptService, InterviewLabels to ai package"
```

---

### Task 3: Move DeepDiveAgent and KnowledgeTools

Move the agent classes to `ai/agent/`.

**Files:**
- Move: `agent/DeepDiveAgent.java` → `ai/agent/DeepDiveAgent.java`
- Move: `agent/KnowledgeTools.java` → `ai/agent/KnowledgeTools.java`

- [ ] **Step 1: Create target directory and move files**

```bash
cd /Users/fengfan/interviewAssistant/backend
mkdir -p src/main/java/com/interviewassistant/ai/agent
```

For each file, move it and update its `package` declaration:

- `agent/DeepDiveAgent.java` → `ai/agent/DeepDiveAgent.java`: change `package com.interviewassistant.agent` → `package com.interviewassistant.ai.agent`
- `agent/KnowledgeTools.java` → `ai/agent/KnowledgeTools.java`: change `package com.interviewassistant.agent` → `package com.interviewassistant.ai.agent`

- [ ] **Step 2: Update imports in moved files**

**`ai/agent/DeepDiveAgent.java`:**
```
com.interviewassistant.service.AiGateway → com.interviewassistant.ai.gateway.AiGateway
com.interviewassistant.service.PromptService → com.interviewassistant.ai.prompt.PromptService
com.interviewassistant.service.InterviewAiService → com.interviewassistant.service.InterviewAiService (unchanged)
```

Note: `DeepDiveAgent` also imports `ObsidianService` and `SseUtils` — these stay in `service` and `common` respectively, no change needed.

**`ai/agent/KnowledgeTools.java`:**
```
com.interviewassistant.service.ObsidianService → unchanged
com.interviewassistant.dto.knowledge.NoteItem → unchanged
```

No internal AI imports to update in `KnowledgeTools`.

- [ ] **Step 3: Compile to verify**

Run: `cd /Users/fengfan/interviewAssistant/backend && ./mvnw compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
cd /Users/fengfan/interviewAssistant/backend && git add -A && git commit -m "refactor: move DeepDiveAgent and KnowledgeTools to ai.agent package"
```

---

### Task 4: Move and update test files

Move the `DeepDiveAgentTest` and update imports in all other test files.

**Files:**
- Move: `test/agent/DeepDiveAgentTest.java` → `test/ai/agent/DeepDiveAgentTest.java`
- Update imports: `test/service/InterviewAiServiceBatchTest.java`
- Update imports: `test/service/InterviewAiServiceDeepDiveTest.java`
- Update imports: `test/controller/InterviewControllerBatchTest.java`
- Update imports: `test/controller/InterviewControllerDeepDiveTest.java`

- [ ] **Step 1: Move DeepDiveAgentTest**

```bash
cd /Users/fengfan/interviewAssistant/backend
mkdir -p src/test/java/com/interviewassistant/ai/agent
```

Move `test/agent/DeepDiveAgentTest.java` → `test/ai/agent/DeepDiveAgentTest.java`.

Update package: `package com.interviewassistant.agent` → `package com.interviewassistant.ai.agent`

Update imports in `DeepDiveAgentTest.java`:
```
com.interviewassistant.service.AiGateway → com.interviewassistant.ai.gateway.AiGateway
com.interviewassistant.service.PromptService → com.interviewassistant.ai.prompt.PromptService
com.interviewassistant.agent.KnowledgeTools → com.interviewassistant.ai.agent.KnowledgeTools
com.interviewassistant.agent.DeepDiveAgent → com.interviewassistant.ai.agent.DeepDiveAgent (if referenced by fully qualified name)
```

- [ ] **Step 2: Update imports in other test files**

Check each test file for references to moved classes and update:

**`test/service/InterviewAiServiceBatchTest.java`:**
```
com.interviewassistant.prompt.InterviewLabels → com.interviewassistant.ai.prompt.InterviewLabels
```
(Also check for `AiGateway`, `PromptService` imports and update if present)

**`test/service/InterviewAiServiceDeepDiveTest.java`:**
Check for and update any imports of `AiGateway`, `PromptService`, `InterviewLabels`, `AiErrorUtils`.

**`test/controller/InterviewControllerBatchTest.java`:**
Check for and update any imports of `AiGateway`, `AiConfig`.

**`test/controller/InterviewControllerDeepDiveTest.java`:**
Check for and update any imports of `DeepDiveAgent`.

- [ ] **Step 3: Run full test suite**

Run: `cd /Users/fengfan/interviewAssistant/backend && ./mvnw test -q`
Expected: BUILD SUCCESS, all tests pass

- [ ] **Step 4: Commit**

```bash
cd /Users/fengfan/interviewAssistant/backend && git add -A && git commit -m "refactor: update test imports and move DeepDiveAgentTest to ai.agent package"
```

---

### Task 5: Verify and clean up

- [ ] **Step 1: Delete empty old directories**

```bash
cd /Users/fengfan/interviewAssistant/backend
rmdir src/main/java/com/interviewassistant/agent 2>/dev/null || true
rmdir src/main/java/com/interviewassistant/prompt 2>/dev/null || true
rmdir src/test/java/com/interviewassistant/agent 2>/dev/null || true
```

- [ ] **Step 2: Final full test suite**

Run: `cd /Users/fengfan/interviewAssistant/backend && ./mvnw test -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Final commit**

```bash
cd /Users/fengfan/interviewAssistant/backend && git add -A && git commit -m "refactor: clean up empty directories after AI package separation"
```
