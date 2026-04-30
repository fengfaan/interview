package com.interviewassistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewassistant.ai.embedding.OnnxEmbeddingService;
import com.interviewassistant.dto.knowledge.SmartConnectionSearchResult;
import com.interviewassistant.dto.knowledge.SmartConnectionsIndexStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmartConnectionsIndexService {

    private final SettingsService settingsService;
    private final ObjectMapper objectMapper;
    private final OnnxEmbeddingService onnxEmbeddingService;

    private static final String SMART_ENV_DIR = ".smart-env";
    private static final String MULTI_DIR = "multi";
    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;

    public SmartConnectionsIndexStatus status() {
        Path dataDir = dataDir();
        if (dataDir == null) {
            return new SmartConnectionsIndexStatus(false, null, List.of(), 0, 0, 0, Map.of(),
                    "Obsidian Vault 未配置");
        }
        if (!Files.isDirectory(dataDir)) {
            return new SmartConnectionsIndexStatus(false, dataDir.toString(), List.of(), 0, 0, 0, Map.of(),
                    "未找到 Smart Connections .smart-env 数据目录");
        }

        List<IndexEntry> entries = loadEntries(dataDir);
        Set<String> modelKeys = new TreeSet<>();
        Map<Integer, Integer> dimensions = new TreeMap<>();
        int sourceCount = 0;
        int blockCount = 0;
        for (IndexEntry entry : entries) {
            modelKeys.add(entry.modelKey());
            dimensions.merge(entry.vector().length, 1, Integer::sum);
            if ("source".equals(entry.type())) {
                sourceCount++;
            } else if ("block".equals(entry.type())) {
                blockCount++;
            }
        }

        return new SmartConnectionsIndexStatus(true, dataDir.toString(), new ArrayList<>(modelKeys),
                sourceCount, blockCount, entries.size(), dimensions, "Smart Connections 索引可读取");
    }

    public List<SmartConnectionSearchResult> findSimilarToNote(String noteId, Integer limit, boolean includeBlocks) {
        if (noteId == null || noteId.isBlank()) {
            return List.of();
        }
        Path dataDir = dataDir();
        if (dataDir == null || !Files.isDirectory(dataDir)) {
            return List.of();
        }

        String normalizedNoteId = normalizePath(noteId);
        List<IndexEntry> entries = loadEntries(dataDir);
        Optional<IndexEntry> target = entries.stream()
                .filter(entry -> "source".equals(entry.type()))
                .filter(entry -> normalizedNoteId.equals(normalizePath(entry.path()))
                        || normalizedNoteId.equals(normalizePath(entry.sourcePath())))
                .findFirst();
        if (target.isEmpty()) {
            return List.of();
        }

        IndexEntry targetEntry = target.get();
        int safeLimit = Math.max(1, Math.min(limit == null ? DEFAULT_LIMIT : limit, MAX_LIMIT));
        return entries.stream()
                .filter(entry -> includeBlocks || "source".equals(entry.type()))
                .filter(entry -> !entry.key().equals(targetEntry.key()))
                .filter(entry -> entry.modelKey().equals(targetEntry.modelKey()))
                .filter(entry -> entry.vector().length == targetEntry.vector().length)
                .map(entry -> toResult(entry, cosineSimilarity(targetEntry.vector(), entry.vector())))
                .sorted(Comparator.comparingDouble(SmartConnectionSearchResult::getScore).reversed())
                .limit(safeLimit)
                .toList();
    }

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

    List<SmartConnectionSearchResult> searchByVector(String modelKey, double[] queryVector, int limit, boolean includeBlocks) {
        Path dataDir = dataDir();
        if (dataDir == null || !Files.isDirectory(dataDir) || queryVector == null || queryVector.length == 0) {
            return List.of();
        }

        return loadEntries(dataDir).stream()
                .filter(entry -> includeBlocks || "source".equals(entry.type()))
                .filter(entry -> entry.modelKey().equals(modelKey))
                .filter(entry -> entry.vector().length == queryVector.length)
                .map(entry -> toResult(entry, cosineSimilarity(queryVector, entry.vector())))
                .sorted(Comparator.comparingDouble(SmartConnectionSearchResult::getScore).reversed())
                .limit(Math.max(1, Math.min(limit, MAX_LIMIT)))
                .toList();
    }

    List<IndexEntry> loadEntries(Path dataDir) {
        Path multiDir = dataDir.resolve(MULTI_DIR);
        if (!Files.isDirectory(multiDir)) {
            return List.of();
        }

        List<IndexEntry> entries = new ArrayList<>();
        try (Stream<Path> files = Files.list(multiDir)) {
            files.filter(path -> path.getFileName().toString().endsWith(".ajson"))
                    .forEach(path -> entries.addAll(readAjsonEntries(path)));
        } catch (IOException e) {
            log.warn("Failed to read Smart Connections index directory: {}", multiDir, e);
        }
        return entries;
    }

    private List<IndexEntry> readAjsonEntries(Path path) {
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8).trim();
            if (content.isBlank()) {
                return List.of();
            }
            String json = "{" + content.replaceFirst(",\\s*$", "") + "}";
            JsonNode root = objectMapper.readTree(json);
            List<IndexEntry> entries = new ArrayList<>();
            Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                entries.addAll(parseEntry(field.getKey(), field.getValue()));
            }
            return entries;
        } catch (Exception e) {
            log.warn("Failed to parse Smart Connections index file: {}", path, e);
            return List.of();
        }
    }

    private List<IndexEntry> parseEntry(String key, JsonNode node) {
        String type = key.startsWith("smart_sources:") ? "source"
                : key.startsWith("smart_blocks:") ? "block"
                : "other";
        if ("other".equals(type)) {
            return List.of();
        }

        JsonNode embeddings = node.path("embeddings");
        if (!embeddings.isObject()) {
            return List.of();
        }

        String path = textOrNull(node.path("path"));
        String sourcePath = "source".equals(type) ? path : sourcePathFromBlockKey(key);
        List<IndexEntry> entries = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> models = embeddings.fields();
        while (models.hasNext()) {
            Map.Entry<String, JsonNode> model = models.next();
            JsonNode vecNode = model.getValue().path("vec");
            if (!vecNode.isArray() || vecNode.isEmpty()) {
                continue;
            }
            entries.add(new IndexEntry(key, path, sourcePath, type, model.getKey(), toVector(vecNode)));
        }
        return entries;
    }

    private double[] toVector(JsonNode vecNode) {
        double[] vector = new double[vecNode.size()];
        for (int i = 0; i < vecNode.size(); i++) {
            vector[i] = vecNode.get(i).asDouble();
        }
        return vector;
    }

    private SmartConnectionSearchResult toResult(IndexEntry entry, double score) {
        return new SmartConnectionSearchResult(entry.key(), entry.path(), entry.sourcePath(),
                entry.type(), score, entry.modelKey());
    }

    private double cosineSimilarity(double[] a, double[] b) {
        double dot = 0;
        double normA = 0;
        double normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) {
            return 0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private Path dataDir() {
        String vaultPath = settingsService.getVaultPath();
        if (vaultPath == null || vaultPath.isBlank()) {
            return null;
        }
        return Paths.get(vaultPath).toAbsolutePath().normalize().resolve(SMART_ENV_DIR);
    }

    private String sourcePathFromBlockKey(String key) {
        String raw = key.replaceFirst("^smart_blocks:", "");
        int markdownIndex = raw.indexOf(".md");
        if (markdownIndex < 0) {
            return raw;
        }
        return raw.substring(0, markdownIndex + 3);
    }

    private String normalizePath(String value) {
        return value == null ? "" : value.replace('\\', '/').trim();
    }

    private String textOrNull(JsonNode node) {
        return node.isTextual() ? node.asText() : null;
    }

    record IndexEntry(String key, String path, String sourcePath, String type, String modelKey, double[] vector) {
    }
}
