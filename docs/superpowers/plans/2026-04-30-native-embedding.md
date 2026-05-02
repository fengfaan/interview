# Native ONNX Embedding Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the HTTP-based smart-embedding sidecar with JVM-native ONNX Runtime inference for bge-micro-v2, keeping Smart Connections index data.

**Architecture:** Load `model_quantized.onnx` directly in the JVM using Microsoft ONNX Runtime Java API. Tokenize input text with a simple Java tokenizer (matching the BGE/BERT WordPiece tokenizer), run ONNX inference, mean-pool + normalize the output. Replace `SmartEmbeddingClient` (HTTP calls) with `OnnxEmbeddingService` (local inference).

**Tech Stack:** ONNX Runtime Java 1.20+, bge-micro-v2 ONNX model, Java 17, Spring Boot 3.3.

---

## File Structure

| Action | File | Responsibility |
|--------|------|----------------|
| Modify | `pom.xml` | Add ONNX Runtime dependency |
| Create | `ai/embedding/OnnxEmbeddingService.java` | Load model, tokenize, run inference |
| Create | `ai/embedding/BertTokenizer.java` | WordPiece tokenizer for BGE models |
| Modify | `service/SmartConnectionsIndexService.java` | Use `OnnxEmbeddingService` instead of `SmartEmbeddingClient` |
| Delete | `service/SmartEmbeddingClient.java` | Remove HTTP sidecar client |
| Delete | `tools/smart-embedding/` | Remove sidecar (no longer needed) |

---

### Task 1: Add ONNX Runtime dependency to pom.xml

**Files:**
- Modify: `backend/pom.xml`

- [ ] **Step 1: Add ONNX Runtime dependency**

Add to `<dependencies>`:

```xml
<dependency>
    <groupId>com.microsoft.onnxruntime</groupId>
    <artifactId>onnxruntime</artifactId>
    <version>1.20.1</version>
</dependency>
```

- [ ] **Step 2: Verify dependency resolves**

Run: `cd /Users/fengfan/interviewAssistant/backend && ./mvnw dependency:resolve -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
cd /Users/fengfan/interviewAssistant/backend && git add pom.xml && git commit -m "build: add ONNX Runtime dependency for native embedding inference"
```

---

### Task 2: Copy ONNX model and tokenizer vocab to backend resources

The model file is at `tools/smart-embedding/node_modules/@huggingface/transformers/.cache/TaylorAI/bge-micro-v2/onnx/model_quantized.onnx`. We also need the tokenizer vocab file.

**Files:**
- Create: `backend/src/main/resources/embedding/model_quantized.onnx` (copy)
- Create: `backend/src/main/resources/embedding/tokenizer.json` (copy)

- [ ] **Step 1: Find tokenizer config**

The `@huggingface/transformers` library caches tokenizer configs. Find and copy the tokenizer.json:

```bash
cd /Users/fengfan/interviewAssistant/backend
mkdir -p src/main/resources/embedding
# Copy ONNX model
cp ../tools/smart-embedding/node_modules/@huggingface/transformers/.cache/TaylorAI/bge-micro-v2/onnx/model_quantized.onnx src/main/resources/embedding/
# Find and copy tokenizer
find ../tools/smart-embedding/node_modules/@huggingface/transformers/.cache/TaylorAI/bge-micro-v2 -name "tokenizer.json" -exec cp {} src/main/resources/embedding/ \;
```

If tokenizer.json is not found locally, download from HuggingFace:
```bash
curl -L -o src/main/resources/embedding/tokenizer.json "https://huggingface.co/TaylorAI/bge-micro-v2/resolve/main/tokenizer.json"
```

- [ ] **Step 2: Verify files exist**

```bash
ls -lh src/main/resources/embedding/
```
Expected: `model_quantized.onnx` (~20-30MB) and `tokenizer.json` (~200KB)

- [ ] **Step 3: Commit**

```bash
cd /Users/fengfan/interviewAssistant/backend && git add src/main/resources/embedding/ && git commit -m "build: add ONNX model and tokenizer for native embedding"
```

---

### Task 3: Create BertTokenizer

**Files:**
- Create: `backend/src/main/java/com/interviewassistant/ai/embedding/BertTokenizer.java`

This is a WordPiece tokenizer matching BGE/BERT behavior. It reads `tokenizer.json` to get the vocabulary, then tokenizes input text into token IDs.

- [ ] **Step 1: Create the tokenizer class**

```java
package com.interviewassistant.ai.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.util.*;

public class BertTokenizer {

    private final Map<String, Integer> vocab;
    private final Map<Integer, String> idToToken;
    private final boolean doLowerCase;
    private final int clsTokenId;
    private final int sepTokenId;
    private final int padTokenId;

    public BertTokenizer(String tokenizerPath) {
        this(tokenizerPath, true);
    }

    public BertTokenizer(String tokenizerPath, boolean doLowerCase) {
        this.doLowerCase = doLowerCase;
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream is;
            if (tokenizerPath.startsWith("classpath:")) {
                is = new ClassPathResource(tokenizerPath.substring(10)).getInputStream();
            } else {
                is = new java.io.FileInputStream(tokenizerPath);
            }
            JsonNode root = mapper.readTree(is);

            Map<String, Integer> v = new HashMap<>();
            JsonNode modelNode = root.path("model");
            JsonNode vocabNode = modelNode.path("vocab");
            Iterator<Map.Entry<String, JsonNode>> fields = vocabNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                v.put(entry.getKey(), entry.getValue().asInt());
            }
            this.vocab = Collections.unmodifiableMap(v);

            Map<Integer, String> id2t = new HashMap<>();
            for (Map.Entry<String, Integer> entry : v.entrySet()) {
                id2t.put(entry.getValue(), entry.getKey());
            }
            this.idToToken = Collections.unmodifiableMap(id2t);

            this.clsTokenId = v.getOrDefault("[CLS]", 101);
            this.sepTokenId = v.getOrDefault("[SEP]", 102);
            this.padTokenId = v.getOrDefault("[PAD]", 0);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load tokenizer: " + e.getMessage(), e);
        }
    }

    public record Tokenization(long[] inputIds, long[] attentionMask) {}

    public Tokenization tokenize(String text, int maxLength) {
        String processed = doLowerCase ? text.toLowerCase(Locale.ROOT) : text;
        List<String> tokens = basicTokenize(processed);
        List<String> wordPieces = new ArrayList<>();
        for (String token : tokens) {
            wordPieceTokenize(token, wordPieces);
        }

        // Truncate to maxLength - 2 (CLS + SEP)
        int maxTokens = maxLength - 2;
        if (wordPieces.size() > maxTokens) {
            wordPieces = wordPieces.subList(0, maxTokens);
        }

        // Build input: [CLS] tokens [SEP] [PAD...]
        int seqLen = wordPieces.size() + 2;
        long[] inputIds = new long[seqLen];
        long[] attentionMask = new long[seqLen];

        inputIds[0] = clsTokenId;
        for (int i = 0; i < wordPieces.size(); i++) {
            inputIds[i + 1] = vocab.getOrDefault(wordPieces.get(i), vocab.getOrDefault("[UNK]", 100));
        }
        inputIds[wordPieces.size() + 1] = sepTokenId;
        Arrays.fill(attentionMask, 0, seqLen, 1L);

        return new Tokenization(inputIds, attentionMask);
    }

    private List<String> basicTokenize(String text) {
        // Strip accents, split on whitespace and punctuation
        String cleaned = text.replaceAll("[\\p{M}]", "");
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        for (char c : cleaned.toCharArray()) {
            if (Character.isWhitespace(c)) {
                if (!sb.isEmpty()) {
                    tokens.add(sb.toString());
                    sb.setLength(0);
                }
            } else if (isPunctuation(c)) {
                if (!sb.isEmpty()) {
                    tokens.add(sb.toString());
                    sb.setLength(0);
                }
                tokens.add(String.valueOf(c));
            } else {
                sb.append(c);
            }
        }
        if (!sb.isEmpty()) {
            tokens.add(sb.toString());
        }
        return tokens;
    }

    private void wordPieceTokenize(String word, List<String> output) {
        if (vocab.containsKey(word)) {
            output.add(word);
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < word.length(); ) {
            int end = word.length();
            boolean found = false;
            while (end > i) {
                String sub = (i > 0 ? "##" : "") + word.substring(i, end);
                if (vocab.containsKey(sub)) {
                    sb.append(sub).append(" ");
                    i = end;
                    found = true;
                    break;
                }
                end--;
            }
            if (!found) {
                sb.append("[UNK]").append(" ");
                i++;
            }
        }
        String[] pieces = sb.toString().trim().split(" ");
        Collections.addAll(output, pieces);
    }

    private boolean isPunctuation(char c) {
        int type = Character.getType(c);
        return type == Character.CONNECTOR_PUNCTUATION
                || type == Character.DASH_PUNCTUATION
                || type == Character.END_PUNCTUATION
                || type == Character.FINAL_QUOTE_PUNCTUATION
                || type == Character.INITIAL_QUOTE_PUNCTUATION
                || type == Character.OTHER_PUNCTUATION
                || type == Character.START_PUNCTUATION;
    }

    public int getPadTokenId() {
        return padTokenId;
    }
}
```

- [ ] **Step 2: Compile**

Run: `cd /Users/fengfan/interviewAssistant/backend && ./mvnw compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
cd /Users/fengfan/interviewAssistant/backend && git add src/main/java/com/interviewassistant/ai/embedding/ && git commit -m "feat(embedding): add BertTokenizer for WordPiece tokenization"
```

---

### Task 4: Create OnnxEmbeddingService

**Files:**
- Create: `backend/src/main/java/com/interviewassistant/ai/embedding/OnnxEmbeddingService.java`

This service loads the ONNX model at startup and provides an `embed(String text)` method that returns `double[]`.

- [ ] **Step 1: Create the service**

```java
package com.interviewassistant.ai.embedding;

import ai.onnxruntime.*;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Map;

@Slf4j
@Service
public class OnnxEmbeddingService {

    private static final String MODEL_RESOURCE = "embedding/model_quantized.onnx";
    private static final String TOKENIZER_RESOURCE = "embedding/tokenizer.json";
    private static final int MAX_SEQ_LENGTH = 512;
    private static final String EXPECTED_MODEL = "TaylorAI/bge-micro-v2";
    private static final int EXPECTED_DIMS = 384;

    private OrtEnvironment env;
    private OrtSession session;
    private BertTokenizer tokenizer;

    @PostConstruct
    void init() {
        try {
            this.env = OrtEnvironment.getEnvironment();

            // Extract model to temp file (ONNX Runtime needs a file path)
            Path modelPath = extractResourceToTemp(MODEL_RESOURCE, "embedding-model", ".onnx");
            SessionOptions opts = new SessionOptions();
            opts.setOptimizationLevel(SessionOptions.OptLevel.ALL_OPT);
            this.session = new OrtSession(env, modelPath.toString(), opts);

            this.tokenizer = new BertTokenizer("classpath:" + TOKENIZER_RESOURCE);

            log.info("ONNX embedding model loaded: dims={}, model={}", EXPECTED_DIMS, EXPECTED_MODEL);
        } catch (Exception e) {
            log.error("Failed to load ONNX embedding model: {}", e.getMessage());
            throw new RuntimeException("ONNX embedding model 加载失败: " + e.getMessage(), e);
        }
    }

    @PreDestroy
    void close() {
        try {
            if (session != null) session.close();
            if (env != null) env.close();
        } catch (Exception e) {
            log.warn("Error closing ONNX session: {}", e.getMessage());
        }
    }

    public record EmbeddingResult(String model, double[] vector) {}

    public EmbeddingResult embed(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("查询文本不能为空");
        }

        try {
            BertTokenizer.Tokenization tok = tokenizer.tokenize(text, MAX_SEQ_LENGTH);

            long[] shape = {1, tok.inputIds().length};
            OnnxTensor inputIdsTensor = OnnxTensor.createTensor(env, new long[][]{tok.inputIds()});
            OnnxTensor attentionMaskTensor = OnnxTensor.createTensor(env, new long[][]{tok.attentionMask()});

            Map<String, OnnxTensor> inputs = Map.of(
                    "input_ids", inputIdsTensor,
                    "attention_mask", attentionMaskTensor
            );

            try (OrtSession.Result result = session.run(inputs)) {
                float[][][] output = (float[][][]) result.get(0).getValue();
                double[] pooled = meanPool(output[0], tok.attentionMask());
                double[] normalized = normalize(pooled);
                return new EmbeddingResult(EXPECTED_MODEL, round(normalized));
            }
        } catch (OrtException e) {
            throw new RuntimeException("ONNX inference 失败: " + e.getMessage(), e);
        }
    }

    private double[] meanPool(float[][] tokenVectors, long[] attentionMask) {
        int dims = tokenVectors[0].length;
        double[] result = new double[dims];
        double count = 0;
        for (int i = 0; i < tokenVectors.length; i++) {
            if (attentionMask[i] == 1) {
                for (int j = 0; j < dims; j++) {
                    result[j] += tokenVectors[i][j];
                }
                count++;
            }
        }
        if (count > 0) {
            for (int j = 0; j < dims; j++) {
                result[j] /= count;
            }
        }
        return result;
    }

    private double[] normalize(double[] vector) {
        double norm = 0;
        for (double v : vector) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        if (norm == 0) return vector;
        double[] result = new double[vector.length];
        for (int i = 0; i < vector.length; i++) {
            result[i] = vector[i] / norm;
        }
        return result;
    }

    private double[] round(double[] vector) {
        double[] result = new double[vector.length];
        for (int i = 0; i < vector.length; i++) {
            result[i] = Math.round(vector[i] * 1e8) / 1e8;
        }
        return result;
    }

    private Path extractResourceToTemp(String resourcePath, String prefix, String suffix) throws Exception {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        try (InputStream is = resource.getInputStream()) {
            Path tempFile = Files.createTempFile(prefix, suffix);
            Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
            tempFile.toFile().deleteOnExit();
            return tempFile;
        }
    }
}
```

- [ ] **Step 2: Compile**

Run: `cd /Users/fengfan/interviewAssistant/backend && ./mvnw compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
cd /Users/fengfan/interviewAssistant/backend && git add src/main/java/com/interviewassistant/ai/embedding/OnnxEmbeddingService.java && git commit -m "feat(embedding): add OnnxEmbeddingService for native ONNX inference"
```

---

### Task 5: Wire OnnxEmbeddingService into SmartConnectionsIndexService

**Files:**
- Modify: `backend/src/main/java/com/interviewassistant/service/SmartConnectionsIndexService.java`
- Delete: `backend/src/main/java/com/interviewassistant/service/SmartEmbeddingClient.java`

- [ ] **Step 1: Replace SmartEmbeddingClient with OnnxEmbeddingService**

In `SmartConnectionsIndexService.java`:

Change import:
```java
// Remove
import com.interviewassistant.service.SmartEmbeddingClient;
// Add
import com.interviewassistant.ai.embedding.OnnxEmbeddingService;
```

Change field:
```java
// Remove
private final SmartEmbeddingClient smartEmbeddingClient;
// Add
private final OnnxEmbeddingService onnxEmbeddingService;
```

Change `search()` method (line ~96-108):

```java
public List<SmartConnectionSearchResult> search(String query, Integer limit, boolean includeBlocks) {
    if (query == null || query.isBlank()) {
        return List.of();
    }
    Path dataDir = dataDir();
    if (dataDir == null || !Files.isDirectory(dataDir)) {
        return List.of();
    }

    OnnxEmbeddingService.EmbeddingResult embedding = onnxEmbeddingService.embed(query);
    int safeLimit = Math.max(1, Math.min(limit == null ? DEFAULT_LIMIT : limit, MAX_LIMIT));
    return searchByVector(embedding.model(), embedding.vector(), safeLimit, includeBlocks);
}
```

Note: `EmbeddingResult` is now `OnnxEmbeddingService.EmbeddingResult` with fields `model()` and `vector()`. The `searchByVector` method signature stays the same.

- [ ] **Step 2: Delete SmartEmbeddingClient**

```bash
rm src/main/java/com/interviewassistant/service/SmartEmbeddingClient.java
```

- [ ] **Step 3: Compile and run tests**

Run: `cd /Users/fengfan/interviewAssistant/backend && ./mvnw test -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
cd /Users/fengfan/interviewAssistant/backend && git add -A && git commit -m "refactor: replace SmartEmbeddingClient HTTP sidecar with native ONNX inference

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 6: Smoke test and verify

- [ ] **Step 1: Start backend**

Run: `cd /Users/fengfan/interviewAssistant/backend && mvn spring-boot:run`
Expected: Log shows "ONNX embedding model loaded: dims=384, model=TaylorAI/bge-micro-v2"

- [ ] **Step 2: Test vector search endpoint**

```bash
curl -s "http://localhost:8080/api/knowledge/smart-connections/search?query=B%2B%E6%A0%91&limit=3" | python3 -m json.tool
```
Expected: Returns JSON with search results (or empty array if no index data)

- [ ] **Step 3: Final commit if any fixes needed**

```bash
git add -A && git commit -m "fix: address issues from ONNX embedding smoke test"
```
