# Web Question Import Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Allow users to import interview questions from web pages via URL capture or manual paste, AI-parse into structured questions, and batch-import into the Obsidian knowledge base.

**Architecture:** Playwright (Java) captures web page text on the backend. A new `ImportController` exposes capture/parse/save endpoints. AI parses text into structured questions using a new prompt template. Frontend gets a new `/import` route with a two-tab UI (URL / paste). Obsidian notes get a `source: web-import` tag and optional `url` frontmatter field.

**Tech Stack:** Playwright Java, Spring Boot, Spring AI, Vue 3, Pinia, TypeScript

---

## File Structure

### Backend — New Files
| File | Responsibility |
|---|---|
| `backend/src/main/java/com/interviewassistant/controller/ImportController.java` | REST endpoints: capture, parse, save |
| `backend/src/main/java/com/interviewassistant/service/BrowserCaptureService.java` | Playwright lifecycle + page text extraction |
| `backend/src/main/java/com/interviewassistant/dto/import_/CaptureRequest.java` | URL input DTO |
| `backend/src/main/java/com/interviewassistant/dto/import_/CaptureResponse.java` | Capture result DTO |
| `backend/src/main/java/com/interviewassistant/dto/import_/ParseRequest.java` | Parse input DTO |
| `backend/src/main/java/com/interviewassistant/dto/import_/ParseResponse.java` | Parse result DTO |
| `backend/src/main/java/com/interviewassistant/dto/import_/ImportSaveRequest.java` | Batch save DTO |
| `backend/src/main/java/com/interviewassistant/dto/import_/ImportSaveResult.java` | Per-item save result DTO |
| `backend/prompts/import/import-parse.md` | AI prompt for parsing web text into questions |

### Backend — Modified Files
| File | Change |
|---|---|
| `backend/pom.xml` | Add Playwright dependency |
| `backend/src/main/java/.../dto/knowledge/CreateNoteRequest.java` | Add optional `url` field |
| `backend/src/main/java/.../service/ObsidianService.java` | Write `url` to frontmatter |

### Frontend — New Files
| File | Responsibility |
|---|---|
| `frontend/src/views/ImportView.vue` | Import page (URL tab + paste tab + results + save) |
| `frontend/src/stores/importStore.ts` | State: URL, captured text, parsed questions, save progress |
| `frontend/src/api/importApi.ts` | API calls: capture, parse, save |
| `frontend/src/types/import.ts` | TypeScript types for import feature |

### Frontend — Modified Files
| File | Change |
|---|---|
| `frontend/src/router/index.ts` | Add `/import` route |
| `frontend/src/components/AppSidebar.vue` | Add nav entry |

---

### Task 1: Add Playwright Dependency

**Files:**
- Modify: `backend/pom.xml`

- [ ] **Step 1: Add Playwright dependency to pom.xml**

Add inside `<dependencies>`:

```xml
<dependency>
    <groupId>com.microsoft.playwright</groupId>
    <artifactId>playwright</artifactId>
    <version>1.49.0</version>
</dependency>
```

- [ ] **Step 2: Verify compilation**

Run: `cd backend && ./mvnw compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add backend/pom.xml
git commit -m "chore: add Playwright Java dependency for web capture"
```

---

### Task 2: Create Import DTOs

**Files:**
- Create: `backend/src/main/java/com/interviewassistant/dto/import_/CaptureRequest.java`
- Create: `backend/src/main/java/com/interviewassistant/dto/import_/CaptureResponse.java`
- Create: `backend/src/main/java/com/interviewassistant/dto/import_/ParseRequest.java`
- Create: `backend/src/main/java/com/interviewassistant/dto/import_/ParseResponse.java`
- Create: `backend/src/main/java/com/interviewassistant/dto/import_/ImportSaveRequest.java`
- Create: `backend/src/main/java/com/interviewassistant/dto/import_/ImportSaveResult.java`

Note: Package uses `import_` because `import` is a Java keyword.

- [ ] **Step 1: Create CaptureRequest**

```java
package com.interviewassistant.dto.import_;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CaptureRequest {
    @NotBlank(message = "URL 不能为空")
    @Pattern(regexp = "^https?://.*", message = "URL 必须以 http:// 或 https:// 开头")
    private String url;
}
```

- [ ] **Step 2: Create CaptureResponse**

```java
package com.interviewassistant.dto.import_;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CaptureResponse {
    private String title;
    private String content;
    private String url;
    private String capturedAt;
}
```

- [ ] **Step 3: Create ParseRequest**

```java
package com.interviewassistant.dto.import_;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ParseRequest {
    @NotBlank(message = "内容不能为空")
    private String content;

    private String direction;
    private String level;
}
```

- [ ] **Step 4: Create ParseResponse**

```java
package com.interviewassistant.dto.import_;

import com.interviewassistant.dto.interview.BatchQuestionItem;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ParseResponse {
    private List<ParsedQuestion> items;

    @Data
    @AllArgsConstructor
    public static class ParsedQuestion {
        private String question;
        private String answer;
        private List<String> keywords;
    }
}
```

- [ ] **Step 5: Create ImportSaveRequest**

```java
package com.interviewassistant.dto.import_;

import com.interviewassistant.dto.import_.ParseResponse.ParsedQuestion;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class ImportSaveRequest {
    @NotEmpty(message = "题目列表不能为空")
    private List<ParsedQuestion> items;
    private String direction;
    private String level;
    private String sourceUrl;
}
```

- [ ] **Step 6: Create ImportSaveResult**

```java
package com.interviewassistant.dto.import_;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ImportSaveResult {
    private String title;
    private boolean success;
    private String error;
}
```

- [ ] **Step 7: Verify compilation**

Run: `cd backend && ./mvnw compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 8: Commit**

```bash
git add backend/src/main/java/com/interviewassistant/dto/import_/
git commit -m "feat(import): add DTOs for web capture, parse, and save"
```

---

### Task 3: Create Prompt Template

**Files:**
- Create: `backend/prompts/import/import-parse.md`

- [ ] **Step 1: Create directory and prompt file**

```
mkdir -p backend/prompts/import
```

Create `backend/prompts/import/import-parse.md`:

```markdown
你是一位资深技术面试官。请从以下网页文本中识别并提取所有面试题。

## 方向
{{direction}}

## 题型
{{questionType}}

## 网页文本内容
{{content}}

## 输出要求
- 只输出 JSON，不要使用 Markdown 代码块，不要解释
- 使用紧凑 JSON 格式：{"items":[{"q":"题目","a":"参考答案","k":["关键词"]}]}
- 如果文本中不包含面试题，返回 {"items":[]}
- 从文本中尽可能完整地提取所有面试题
- q 为题目原文或提炼后的面试提问
- a 为题目对应的参考答案，尽可能从文本中提取原文答案；如果原文没有答案，根据题目和方向生成简明参考答案
- k 为 3-5 个核心关键词
- 所有字段使用中文
- 答案控制在 200-500 字

## 输出字段
- q：面试题
- a：参考答案
- k：关键词
```

- [ ] **Step 2: Commit**

```bash
git add backend/prompts/import/
git commit -m "feat(import): add import-parse prompt template"
```

---

### Task 4: Create BrowserCaptureService

**Files:**
- Create: `backend/src/main/java/com/interviewassistant/service/BrowserCaptureService.java`

- [ ] **Step 1: Create BrowserCaptureService**

```java
package com.interviewassistant.service;

import com.interviewassistant.dto.import_.CaptureResponse;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
public class BrowserCaptureService {

    private static final int MAX_CONTENT_LENGTH = 4000;
    private static final String PAGE_TIMEOUT_MS = "30000";
    private static final List<String> CONTENT_SELECTORS = List.of(
            "article", "main", ".post-body", ".article-content",
            ".content", ".post-content", "#content", "body"
    );

    private volatile com.microsoft.playwright.Playwright playwright;
    private volatile com.microsoft.playwright.Browser browser;

    public CaptureResponse capture(String url) {
        try {
            ensureBrowser();
            com.microsoft.playwright.BrowserContext context = browser.newContext(
                    new com.microsoft.playwright.Browser.NewContextOptions()
                            .setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
                                    "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            );
            com.microsoft.playwright.Page page = context.newPage();

            page.navigate(url, new com.microsoft.playwright.Page.NavigateOptions()
                    .setTimeout(Double.parseDouble(PAGE_TIMEOUT_MS)));
            page.waitForLoadState(com.microsoft.playwright.LoadState.NETWORKIDLE,
                    new com.microsoft.playwright.Page.WaitForLoadStateOptions()
                            .setTimeout(Double.parseDouble(PAGE_TIMEOUT_MS)));

            String title = page.title();
            String content = extractContent(page);

            context.close();

            return new CaptureResponse(title, content, url, Instant.now().toString());
        } catch (Exception e) {
            throw new RuntimeException("网页抓取失败: " + e.getMessage(), e);
        }
    }

    private String extractContent(com.microsoft.playwright.Page page) {
        for (String selector : CONTENT_SELECTORS) {
            try {
                String text = page.locator(selector).first().innerText();
                if (text != null && text.length() > 100) {
                    return truncate(text.trim());
                }
            } catch (Exception ignored) {
            }
        }
        String bodyText = page.innerText("body");
        return truncate(bodyText != null ? bodyText.trim() : "");
    }

    private String truncate(String text) {
        if (text.length() <= MAX_CONTENT_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_CONTENT_LENGTH) + "\n...（内容过长，已截断）";
    }

    private synchronized void ensureBrowser() {
        if (browser == null) {
            log.info("Initializing Playwright browser...");
            playwright = com.microsoft.playwright.Playwright.create();
            browser = playwright.chromium().launch(
                    new com.microsoft.playwright.BrowserType.LaunchOptions().setHeadless(true)
            );
            log.info("Playwright browser initialized");
        }
    }

    @PreDestroy
    public synchronized void destroy() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd backend && ./mvnw compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/interviewassistant/service/BrowserCaptureService.java
git commit -m "feat(import): add BrowserCaptureService with Playwright"
```

---

### Task 5: Add Parse Method to InterviewAiService

**Files:**
- Modify: `backend/src/main/java/com/interviewassistant/service/InterviewAiService.java`

- [ ] **Step 1: Add parseWebQuestions method**

Add this method to `InterviewAiService` (after `buildBatchAnswerPrompt`):

```java
public List<ParseResponse.ParsedQuestion> parseWebQuestions(String direction, String level, String content) {
    String directionLabel = direction != null ? direction : "综合";
    String levelLabel = level != null ? InterviewLabels.questionTypeLabel(
            InterviewLevel.valueOf(level)) : "基础";

    String userMessage = promptService.render("import/import-parse.md", Map.ofEntries(
            Map.entry("direction", directionLabel),
            Map.entry("questionType", levelLabel),
            Map.entry("content", content)
    ));

    String response = aiGateway.generateText(
            promptService.load("interview/system.md"), userMessage);

    return parseImportResponse(response);
}
```

- [ ] **Step 2: Add parseImportResponse method**

```java
private List<ParseResponse.ParsedQuestion> parseImportResponse(String response) {
    try {
        String json = com.interviewassistant.ai.util.JsonOutputUtils.extractJson(response);
        com.fasterxml.jackson.databind.JsonNode root = objectMapper.reader()
                .with(com.fasterxml.jackson.core.json.JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature())
                .readTree(json);
        com.fasterxml.jackson.databind.JsonNode items = root.path("items");
        if (!items.isArray()) {
            return java.util.Collections.emptyList();
        }
        java.util.List<ParseResponse.ParsedQuestion> result = new java.util.ArrayList<>();
        for (com.fasterxml.jackson.databind.JsonNode item : items) {
            String question = textValue(item, "q", "question");
            if (question == null) continue;
            String answer = textValue(item, "a", "answer");
            result.add(new ParseResponse.ParsedQuestion(question, answer, listValue(item, "k", "keywords")));
        }
        return result;
    } catch (Exception e) {
        throw new IllegalArgumentException("网页面试题解析失败: " + e.getMessage(), e);
    }
}
```

- [ ] **Step 3: Add import for ParseResponse**

Add at the top of the file imports:

```java
import com.interviewassistant.dto.import_.ParseResponse;
```

- [ ] **Step 4: Verify compilation**

Run: `cd backend && ./mvnw compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/interviewassistant/service/InterviewAiService.java
git commit -m "feat(import): add parseWebQuestions method to InterviewAiService"
```

---

### Task 6: Add url Field to CreateNoteRequest + ObsidianService

**Files:**
- Modify: `backend/src/main/java/com/interviewassistant/dto/knowledge/CreateNoteRequest.java`
- Modify: `backend/src/main/java/com/interviewassistant/service/ObsidianService.java`

- [ ] **Step 1: Add url field to CreateNoteRequest**

Add after `private Boolean force;`:

```java
private String url;
```

- [ ] **Step 2: Write url to frontmatter in ObsidianService**

In `ObsidianService.buildFrontmatter()`, after the `questionId` block (after line `sb.append("questionId: ").append(...)`), add:

```java
if (request.getUrl() != null && !request.getUrl().isBlank()) {
    sb.append("url: ").append(yamlDoubleQuote(request.getUrl())).append("\n");
}
```

- [ ] **Step 3: Verify compilation**

Run: `cd backend && ./mvnw compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/interviewassistant/dto/knowledge/CreateNoteRequest.java \
       backend/src/main/java/com/interviewassistant/service/ObsidianService.java
git commit -m "feat(import): add url field to CreateNoteRequest and frontmatter"
```

---

### Task 7: Create ImportController

**Files:**
- Create: `backend/src/main/java/com/interviewassistant/controller/ImportController.java`

- [ ] **Step 1: Create ImportController**

```java
package com.interviewassistant.controller;

import com.interviewassistant.common.ApiResponse;
import com.interviewassistant.dto.import_.CaptureRequest;
import com.interviewassistant.dto.import_.CaptureResponse;
import com.interviewassistant.dto.import_.ImportSaveRequest;
import com.interviewassistant.dto.import_.ImportSaveResult;
import com.interviewassistant.dto.import_.ParseRequest;
import com.interviewassistant.dto.import_.ParseResponse;
import com.interviewassistant.dto.knowledge.CreateNoteRequest;
import com.interviewassistant.service.BrowserCaptureService;
import com.interviewassistant.service.InterviewAiService;
import com.interviewassistant.service.ObsidianService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
public class ImportController {

    private final BrowserCaptureService captureService;
    private final InterviewAiService interviewService;
    private final ObsidianService obsidianService;

    @PostMapping("/capture")
    public ApiResponse<CaptureResponse> capture(@Valid @RequestBody CaptureRequest request) {
        CaptureResponse response = captureService.capture(request.getUrl());
        return ApiResponse.ok(response);
    }

    @PostMapping("/parse")
    public ApiResponse<ParseResponse> parse(@Valid @RequestBody ParseRequest request) {
        List<ParseResponse.ParsedQuestion> items = interviewService.parseWebQuestions(
                request.getDirection(), request.getLevel(), request.getContent());
        return ApiResponse.ok(new ParseResponse(items));
    }

    @PostMapping("/save")
    public ApiResponse<List<ImportSaveResult>> save(@Valid @RequestBody ImportSaveRequest request) {
        List<ImportSaveResult> results = new ArrayList<>();
        for (ParseResponse.ParsedQuestion item : request.getItems()) {
            try {
                CreateNoteRequest noteRequest = new CreateNoteRequest();
                noteRequest.setTitle(item.getQuestion());
                noteRequest.setDirection(request.getDirection());
                noteRequest.setContent("## 题目\n\n" + item.getQuestion()
                        + "\n\n## 参考答案\n\n" + (item.getAnswer() != null ? item.getAnswer() : "暂无答案")
                        + "\n\n## 考察要点\n\n" + String.join("、", item.getKeywords()));
                noteRequest.setTags(item.getKeywords());
                noteRequest.setSource("web-import");
                noteRequest.setUrl(request.getSourceUrl());

                obsidianService.saveNote(noteRequest);
                results.add(new ImportSaveResult(item.getQuestion(), true, null));
            } catch (Exception e) {
                log.warn("Failed to save imported question: {}", e.getMessage());
                results.add(new ImportSaveResult(item.getQuestion(), false, e.getMessage()));
            }
        }
        return ApiResponse.ok(results);
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd backend && ./mvnw compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/interviewassistant/controller/ImportController.java
git commit -m "feat(import): add ImportController with capture/parse/save endpoints"
```

---

### Task 8: Create Frontend Types

**Files:**
- Create: `frontend/src/types/import.ts`

- [ ] **Step 1: Create import types**

```typescript
export interface CaptureRequest {
  url: string
}

export interface CaptureResponse {
  title: string
  content: string
  url: string
  capturedAt: string
}

export interface ParsedQuestion {
  question: string
  answer: string | null
  keywords: string[]
}

export interface ParseRequest {
  content: string
  direction: string
  level: string
}

export interface ParseResponse {
  items: ParsedQuestion[]
}

export interface ImportSaveRequest {
  items: ParsedQuestion[]
  direction: string
  level: string
  sourceUrl: string
}

export interface ImportSaveResult {
  title: string
  success: boolean
  error: string | null
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/types/import.ts
git commit -m "feat(import): add frontend types for import feature"
```

---

### Task 9: Create Frontend API Layer

**Files:**
- Create: `frontend/src/api/importApi.ts`

- [ ] **Step 1: Create importApi.ts**

```typescript
import type {
  CaptureRequest,
  CaptureResponse,
  ParseRequest,
  ParseResponse,
  ImportSaveRequest,
  ImportSaveResult,
} from '../types/import'

const API_BASE = '/api/import'

export async function capturePage(request: CaptureRequest): Promise<CaptureResponse> {
  const res = await fetch(API_BASE + '/capture', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })
  const json = await res.json()
  if (!json.success) throw new Error(json.message || '网页抓取失败')
  return json.data
}

export async function parseQuestions(request: ParseRequest): Promise<ParseResponse> {
  const res = await fetch(API_BASE + '/parse', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })
  const json = await res.json()
  if (!json.success) throw new Error(json.message || '解析失败')
  return json.data
}

export async function saveImported(request: ImportSaveRequest): Promise<ImportSaveResult[]> {
  const res = await fetch(API_BASE + '/save', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })
  const json = await res.json()
  if (!json.success) throw new Error(json.message || '导入失败')
  return json.data
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/api/importApi.ts
git commit -m "feat(import): add frontend API layer for import"
```

---

### Task 10: Create Import Store

**Files:**
- Create: `frontend/src/stores/importStore.ts`

- [ ] **Step 1: Create importStore.ts**

```typescript
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { capturePage, parseQuestions, saveImported } from '../api/importApi'
import type { ParsedQuestion, ImportSaveResult } from '../types/import'
import { DIRECTIONS, LEVELS } from '../types/interview'

export const useImportStore = defineStore('import', () => {
  // Input state
  const inputMode = ref<'url' | 'paste'>('url')
  const url = ref('')
  const pastedContent = ref('')
  const direction = ref('GO_BACKEND')
  const level = ref('BASIC')

  // Capture state
  const capturedTitle = ref('')
  const capturedContent = ref('')
  const isCapturing = ref(false)
  const captureError = ref('')

  // Parse state
  const parsedQuestions = ref<ParsedQuestion[]>([])
  const isParsing = ref(false)
  const parseError = ref('')

  // Edit state
  const selectedIds = ref<Set<number>>(new Set())
  const editingIndex = ref<number | null>(null)
  const editDraft = ref<ParsedQuestion | null>(null)

  // Save state
  const isSaving = ref(false)
  const saveResults = ref<ImportSaveResult[]>([])
  const savedCount = computed(() => saveResults.value.filter(r => r.success).length)

  // Content to parse (from URL capture or manual paste)
  const contentToParse = computed(() =>
    inputMode.value === 'url' ? capturedContent.value : pastedContent.value
  )

  async function doCapture() {
    captureError.value = ''
    isCapturing.value = true
    capturedTitle.value = ''
    capturedContent.value = ''
    try {
      const result = await capturePage({ url: url.value })
      capturedTitle.value = result.title
      capturedContent.value = result.content
    } catch (e: any) {
      captureError.value = e.message || '网页抓取失败'
    } finally {
      isCapturing.value = false
    }
  }

  async function doParse() {
    if (!contentToParse.value.trim()) return
    parseError.value = ''
    isParsing.value = true
    parsedQuestions.value = []
    selectedIds.value = new Set()
    saveResults.value = []
    try {
      const result = await parseQuestions({
        content: contentToParse.value,
        direction: direction.value,
        level: level.value,
      })
      parsedQuestions.value = result.items
    } catch (e: any) {
      parseError.value = e.message || '解析失败'
    } finally {
      isParsing.value = false
    }
  }

  function toggleSelect(index: number) {
    const s = new Set(selectedIds.value)
    if (s.has(index)) {
      s.delete(index)
    } else {
      s.add(index)
    }
    selectedIds.value = s
  }

  function selectAll() {
    selectedIds.value = new Set(parsedQuestions.value.map((_, i) => i))
  }

  function deselectAll() {
    selectedIds.value = new Set()
  }

  function removeQuestion(index: number) {
    const updated = [...parsedQuestions.value]
    updated.splice(index, 1)
    parsedQuestions.value = updated
    const s = new Set(selectedIds.value)
    s.delete(index)
    selectedIds.value = new Set([...s].filter(i => i < updated.length))
  }

  function startEdit(index: number) {
    editingIndex.value = index
    editDraft.value = { ...parsedQuestions.value[index] }
  }

  function cancelEdit() {
    editingIndex.value = null
    editDraft.value = null
  }

  function saveEdit() {
    if (editingIndex.value !== null && editDraft.value) {
      parsedQuestions.value[editingIndex.value] = { ...editDraft.value }
      editingIndex.value = null
      editDraft.value = null
    }
  }

  async function doSave() {
    const items = [...selectedIds.value]
      .sort((a, b) => a - b)
      .map(i => parsedQuestions.value[i])
    if (items.length === 0) return

    isSaving.value = true
    saveResults.value = []
    try {
      const results = await saveImported({
        items,
        direction: direction.value,
        level: level.value,
        sourceUrl: inputMode.value === 'url' ? url.value : '',
      })
      saveResults.value = results
    } catch (e: any) {
      parseError.value = e.message || '导入失败'
    } finally {
      isSaving.value = false
    }
  }

  function reset() {
    url.value = ''
    pastedContent.value = ''
    capturedTitle.value = ''
    capturedContent.value = ''
    captureError.value = ''
    parsedQuestions.value = []
    parseError.value = ''
    selectedIds.value = new Set()
    editingIndex.value = null
    editDraft.value = null
    isSaving.value = false
    saveResults.value = []
  }

  return {
    inputMode, url, pastedContent, direction, level,
    capturedTitle, capturedContent, isCapturing, captureError,
    parsedQuestions, isParsing, parseError,
    selectedIds, editingIndex, editDraft,
    isSaving, saveResults, savedCount, contentToParse,
    doCapture, doParse, toggleSelect, selectAll, deselectAll,
    removeQuestion, startEdit, cancelEdit, saveEdit, doSave, reset,
  }
})
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/stores/importStore.ts
git commit -m "feat(import): add Pinia store for import page"
```

---

### Task 11: Create ImportView

**Files:**
- Create: `frontend/src/views/ImportView.vue`

- [ ] **Step 1: Create ImportView.vue**

```vue
<template>
  <div class="min-h-screen flex flex-col">
    <header class="sticky top-0 z-10 bg-surface-container-low/80 backdrop-blur-xl border-b border-outline-variant/20 px-8 py-4">
      <div class="flex items-center justify-between">
        <h2 class="text-xl font-extrabold text-on-surface font-headline flex items-center gap-2">
          <span class="material-symbols-outlined text-primary">cloud_download</span>
          网页抓题
        </h2>
        <button
          v-if="store.parsedQuestions.length"
          @click="store.reset()"
          class="bg-surface-container-high hover:bg-surface-container-highest text-on-surface font-medium rounded-lg px-4 py-2 transition-colors flex items-center gap-2 text-sm"
        >
          <span class="material-symbols-outlined text-base">refresh</span>
          重新开始
        </button>
      </div>
    </header>

    <!-- Input Area -->
    <div class="px-8 py-6">
      <div class="max-w-4xl mx-auto">
        <!-- Tab switch -->
        <div class="flex gap-2 mb-5">
          <button
            @click="store.inputMode = 'url'"
            class="px-4 py-2 rounded-lg text-sm font-medium transition-colors"
            :class="store.inputMode === 'url' ? 'bg-primary text-on-primary' : 'bg-surface-container-high text-on-surface-variant'"
          >URL 抓取</button>
          <button
            @click="store.inputMode = 'paste'"
            class="px-4 py-2 rounded-lg text-sm font-medium transition-colors"
            :class="store.inputMode === 'paste' ? 'bg-primary text-on-primary' : 'bg-surface-container-high text-on-surface-variant'"
          >粘贴文本</button>
        </div>

        <!-- URL mode -->
        <div v-if="store.inputMode === 'url'" class="space-y-3">
          <div class="flex gap-3">
            <input
              v-model="store.url"
              type="url"
              placeholder="输入面试题网页 URL，如 https://juejin.cn/post/..."
              class="flex-1 bg-surface-container-lowest text-on-surface border border-outline-variant/30 rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-primary outline-none"
              @keyup.enter="store.doCapture()"
            />
            <button
              @click="store.doCapture()"
              :disabled="store.isCapturing || !store.url.trim()"
              class="bg-primary text-on-primary font-medium rounded-xl px-6 py-3 shadow-sm hover:opacity-90 transition-opacity disabled:opacity-50 flex items-center gap-2 text-sm"
            >
              <span v-if="store.isCapturing" class="material-symbols-outlined animate-spin text-base">progress_activity</span>
              <span v-else class="material-symbols-outlined text-base">search</span>
              抓取
            </button>
          </div>
          <div v-if="store.captureError" class="bg-error-container text-on-error-container px-4 py-3 rounded-xl text-sm">
            {{ store.captureError }}
          </div>
          <div v-if="store.capturedContent && !store.isCapturing" class="bg-surface-container-lowest rounded-xl p-4">
            <div class="flex items-center justify-between mb-2">
              <span class="text-sm font-semibold text-on-surface">{{ store.capturedTitle }}</span>
              <button @click="showRaw = !showRaw" class="text-xs text-primary underline">
                {{ showRaw ? '收起' : '查看原文' }}
              </button>
            </div>
            <div v-if="showRaw" class="text-xs text-on-surface-variant max-h-40 overflow-y-auto whitespace-pre-wrap">{{ store.capturedContent }}</div>
          </div>
        </div>

        <!-- Paste mode -->
        <div v-if="store.inputMode === 'paste'">
          <textarea
            v-model="store.pastedContent"
            class="w-full min-h-[200px] bg-surface-container-lowest text-on-surface border border-outline-variant/30 rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-primary outline-none resize-y"
            placeholder="粘贴面试题网页内容..."
          ></textarea>
        </div>

        <!-- Direction & Level -->
        <div class="flex gap-6 mt-5">
          <div>
            <label class="text-sm font-label text-on-surface-variant mb-2 block">方向</label>
            <div class="flex flex-wrap gap-2">
              <button
                v-for="d in DIRECTIONS" :key="d.value"
                @click="store.direction = d.value"
                class="px-3 py-2 rounded-lg text-sm font-medium transition-colors"
                :class="store.direction === d.value ? 'bg-primary text-on-primary' : 'bg-surface-container-high text-on-surface-variant'"
              >{{ d.label }}</button>
            </div>
          </div>
          <div>
            <label class="text-sm font-label text-on-surface-variant mb-2 block">级别</label>
            <div class="flex gap-2">
              <button
                v-for="l in LEVELS" :key="l.value"
                @click="store.level = l.value"
                class="px-3 py-2 rounded-lg text-sm font-medium transition-colors"
                :class="store.level === l.value ? 'bg-primary text-on-primary' : 'bg-surface-container-high text-on-surface-variant'"
              >{{ l.label }}</button>
            </div>
          </div>
        </div>

        <!-- Parse button -->
        <div class="mt-5">
          <button
            @click="store.doParse()"
            :disabled="store.isParsing || !store.contentToParse.trim()"
            class="bg-gradient-to-r from-primary to-primary-container text-on-primary font-label font-medium rounded-xl py-3 px-8 shadow-md hover:opacity-90 transition-opacity disabled:opacity-50 flex items-center gap-2"
          >
            <span v-if="store.isParsing" class="material-symbols-outlined animate-spin text-base">progress_activity</span>
            <span v-else class="material-symbols-outlined text-base">psychology</span>
            {{ store.isParsing ? '解析中...' : '解析面试题' }}
          </button>
        </div>
        <div v-if="store.parseError" class="bg-error-container text-on-error-container px-4 py-3 rounded-xl text-sm mt-3">
          {{ store.parseError }}
        </div>
      </div>
    </div>

    <!-- Parsed Results -->
    <div v-if="store.parsedQuestions.length" class="flex-1 px-8 pb-8">
      <div class="max-w-4xl mx-auto">
        <div class="flex items-center justify-between mb-4">
          <span class="text-sm text-on-surface-variant">共解析 {{ store.parsedQuestions.length }} 题，已选 {{ store.selectedIds.size }} 题</span>
          <div class="flex gap-2">
            <button @click="store.selectAll()" class="text-xs text-primary hover:underline">全选</button>
            <button @click="store.deselectAll()" class="text-xs text-on-surface-variant hover:underline">取消全选</button>
          </div>
        </div>

        <div class="space-y-3">
          <div
            v-for="(q, index) in store.parsedQuestions" :key="index"
            class="bg-surface-container-lowest rounded-xl shadow-sm overflow-hidden"
          >
            <!-- Editing mode -->
            <div v-if="store.editingIndex === index" class="px-6 py-4 space-y-3">
              <input v-model="store.editDraft!.question" class="w-full bg-surface-container-low text-on-surface border border-outline-variant/30 rounded-lg px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary" />
              <textarea v-model="store.editDraft!.answer" class="w-full min-h-[100px] bg-surface-container-low text-on-surface border border-outline-variant/30 rounded-lg px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary resize-y"></textarea>
              <div class="flex gap-2 justify-end">
                <button @click="store.cancelEdit()" class="text-sm text-on-surface-variant px-3 py-1.5 rounded-lg hover:bg-surface-container-high">取消</button>
                <button @click="store.saveEdit()" class="text-sm bg-primary text-on-primary px-4 py-1.5 rounded-lg">保存</button>
              </div>
            </div>
            <!-- Display mode -->
            <div v-else class="px-6 py-4 flex items-start gap-3">
              <input
                type="checkbox"
                :checked="store.selectedIds.has(index)"
                @change="store.toggleSelect(index)"
                class="mt-1 w-4 h-4 accent-primary"
              />
              <div class="flex-1 min-w-0">
                <div class="font-medium text-on-surface text-sm">{{ q.question }}</div>
                <div v-if="q.keywords.length" class="flex flex-wrap gap-1.5 mt-2">
                  <span v-for="kw in q.keywords" :key="kw" class="bg-primary-fixed/40 text-on-primary-fixed px-2 py-0.5 rounded text-xs">{{ kw }}</span>
                </div>
                <div v-if="q.answer" class="text-xs text-on-surface-variant mt-2 line-clamp-2">{{ q.answer }}</div>
              </div>
              <div class="flex items-center gap-1">
                <button @click="store.startEdit(index)" class="text-on-surface-variant hover:text-on-surface p-1.5 rounded transition-colors" title="编辑">
                  <span class="material-symbols-outlined text-base">edit</span>
                </button>
                <button @click="store.removeQuestion(index)" class="text-on-surface-variant hover:text-error p-1.5 rounded transition-colors" title="删除">
                  <span class="material-symbols-outlined text-base">delete</span>
                </button>
              </div>
            </div>
            <!-- Save result indicator -->
            <div v-if="getSaveResult(index)" class="px-6 py-2 text-xs"
                 :class="getSaveResult(index)!.success ? 'bg-primary-container text-on-primary-container' : 'bg-error-container text-on-error-container'">
              {{ getSaveResult(index)!.success ? '已保存' : '保存失败: ' + getSaveResult(index)!.error }}
            </div>
          </div>
        </div>

        <!-- Import button -->
        <div class="mt-6 flex items-center gap-4">
          <button
            @click="store.doSave()"
            :disabled="store.isSaving || store.selectedIds.size === 0"
            class="bg-gradient-to-r from-primary to-primary-container text-on-primary font-label font-medium rounded-xl py-3 px-8 shadow-md hover:opacity-90 transition-opacity disabled:opacity-50 flex items-center gap-2"
          >
            <span v-if="store.isSaving" class="material-symbols-outlined animate-spin text-base">progress_activity</span>
            <span v-else class="material-symbols-outlined text-base">bookmark_add</span>
            {{ store.isSaving ? '导入中...' : `导入 ${store.selectedIds.size} 题到知识库` }}
          </button>
          <span v-if="store.saveResults.length" class="text-sm text-on-surface-variant">
            {{ store.savedCount }} / {{ store.saveResults.length }} 题保存成功
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useImportStore } from '../stores/importStore'
import { DIRECTIONS, LEVELS } from '../types/interview'
import type { ImportSaveResult } from '../types/import'

const store = useImportStore()
const showRaw = ref(false)

function getSaveResult(index: number): ImportSaveResult | undefined {
  return store.saveResults[index]
}
</script>
```

- [ ] **Step 2: Verify build**

Run: `cd frontend && npm run build`
Expected: build succeeds

- [ ] **Step 3: Commit**

```bash
git add frontend/src/views/ImportView.vue
git commit -m "feat(import): add ImportView with URL capture and paste modes"
```

---

### Task 12: Add Route and Navigation

**Files:**
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/components/AppSidebar.vue`

- [ ] **Step 1: Add /import route to router**

In `frontend/src/router/index.ts`, add after the `rapid` route entry (after line 24):

```typescript
        {
          path: 'import',
          name: 'import',
          component: () => import('../views/ImportView.vue'),
        },
```

- [ ] **Step 2: Add nav entry to sidebar**

In `frontend/src/components/AppSidebar.vue`, add after the 快速刷题 `<li>` (after line 42 `</li>`):

```html
        <li>
          <RouterLink
            to="/import"
            class="flex items-center gap-3 px-4 py-3 rounded-lg transition-all duration-300"
            :class="isActive('/import') ? 'bg-white/10 text-white font-bold' : 'text-neutral-400 hover:text-white hover:bg-white/5'"
          >
            <span class="material-symbols-outlined">cloud_download</span>
            网页抓题
          </RouterLink>
        </li>
```

- [ ] **Step 3: Verify build**

Run: `cd frontend && npm run build`
Expected: build succeeds

- [ ] **Step 4: Commit**

```bash
git add frontend/src/router/index.ts frontend/src/components/AppSidebar.vue
git commit -m "feat(import): add /import route and sidebar nav entry"
```

---

### Task 13: Add Prompt Description

**Files:**
- Modify: `backend/src/main/java/com/interviewassistant/ai/prompt/PromptService.java`

- [ ] **Step 1: Add description for import-parse.md**

In `PromptService.PROMPT_DESCRIPTIONS` map, add:

```java
entry("import/import-parse.md", "从网页文本中 AI 解析面试题"),
```

- [ ] **Step 2: Verify compilation**

Run: `cd backend && ./mvnw compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/interviewassistant/ai/prompt/PromptService.java
git commit -m "feat(import): add prompt description for import-parse template"
```

---

### Task 14: Final Build Verification

- [ ] **Step 1: Backend compile**

Run: `cd backend && ./mvnw compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 2: Frontend build**

Run: `cd frontend && npm run build`
Expected: build succeeds with no errors

- [ ] **Step 3: Push all commits**

```bash
git push
```
