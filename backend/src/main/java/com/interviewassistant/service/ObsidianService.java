package com.interviewassistant.service;

import com.interviewassistant.dto.knowledge.CreateNoteRequest;
import com.interviewassistant.dto.knowledge.NoteDetail;
import com.interviewassistant.dto.knowledge.NoteItem;
import com.interviewassistant.dto.knowledge.SimilarNotesResult;
import com.interviewassistant.dto.knowledge.SmartConnectionSearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ObsidianService {

    private final SettingsService settingsService;
    private final SmartConnectionsIndexService smartConnectionsIndexService;

    private static final String KNOWLEDGE_DIR = "面试知识库";
    private static final long MAX_FILE_SIZE = 1024 * 1024; // 1MB
    private static final Pattern FRONTMATTER_PATTERN = Pattern.compile(
            "^---\\s*\\n(.*?)\\n---\\s*\\n", Pattern.DOTALL
    );
    private static final Pattern YAML_FIELD_PATTERN = Pattern.compile(
            "^(\\w[\\w-]*):\\s*(.*)$", Pattern.MULTILINE
    );
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final DateTimeFormatter FILE_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private static final Map<String, String> DIRECTION_MAP = Map.of(
            "BACKEND", "后端",
            "FRONTEND", "前端",
            "SYSTEM_DESIGN", "系统设计"
    );

    public boolean isVaultConfigured() {
        String path = settingsService.getVaultPath();
        if (path == null || path.isBlank()) {
            return false;
        }
        Path vaultPath = Paths.get(path).toAbsolutePath().normalize();
        return Files.isDirectory(vaultPath) && Files.isReadable(vaultPath) && Files.isWritable(vaultPath);
    }

    public Path getVaultPath() {
        String path = settingsService.getVaultPath();
        if (path == null || path.isBlank()) {
            throw new IllegalStateException("Obsidian Vault 未配置，请先在设置页配置路径");
        }
        return Paths.get(path).toAbsolutePath().normalize();
    }

    public List<NoteItem> listNotes(String direction) {
        Path vaultPath = getVaultPath();
        Path knowledgeDir = vaultPath.resolve(KNOWLEDGE_DIR);
        if (!Files.exists(knowledgeDir)) {
            return Collections.emptyList();
        }

        List<NoteItem> notes = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(knowledgeDir, 2)) {
            walk.filter(p -> p.toString().endsWith(".md"))
                    .filter(p -> Files.isRegularFile(p))
                    .map(p -> parseNoteItem(p, knowledgeDir))
                    .filter(Objects::nonNull)
                    .filter(note -> direction == null || direction.isBlank() || direction.equals(note.getDirection()))
                    .forEach(notes::add);
        } catch (IOException e) {
            log.error("Failed to list notes", e);
            throw new RuntimeException("读取笔记列表失败: " + e.getMessage());
        }

        notes.sort((a, b) -> {
            if (a.getCreated() == null || b.getCreated() == null) return 0;
            return b.getCreated().compareTo(a.getCreated());
        });
        return notes;
    }

    public NoteDetail readNote(String noteId) {
        Path vaultPath = getVaultPath();
        Path knowledgeDir = vaultPath.resolve(KNOWLEDGE_DIR).toAbsolutePath().normalize();
        Path filePath = resolveSafePath(knowledgeDir, noteId);
        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException("笔记不存在: " + noteId);
        }
        try {
            if (Files.size(filePath) > MAX_FILE_SIZE) {
                throw new IllegalArgumentException("笔记文件过大，无法读取");
            }
        } catch (IOException e) {
            throw new RuntimeException("读取文件信息失败: " + e.getMessage());
        }

        String content = readFileContent(filePath);
        NoteItem item = parseNoteItem(filePath, knowledgeDir);
        if (item == null) {
            throw new IllegalArgumentException("笔记格式无法解析: " + noteId);
        }
        String bodyContent = stripFrontmatter(content);

        return new NoteDetail(
                item.getId(), item.getTitle(), item.getDirection(),
                item.getTags(), item.getCreated(), item.getFileName(),
                bodyContent
        );
    }

    public SimilarNotesResult findSimilarNotes(String title, String direction) {
        Path vaultPath = getVaultPath();
        Path knowledgeDir = vaultPath.resolve(KNOWLEDGE_DIR);
        if (title == null || title.isBlank() || !Files.exists(knowledgeDir)) {
            return new SimilarNotesResult("none", Collections.emptyList());
        }

        List<NoteItem> vectorResults = searchByVector(title);
        if (!vectorResults.isEmpty()) {
            return new SimilarNotesResult("vector", vectorResults);
        }

        List<NoteItem> textResults = searchTextSimilar(title, direction);
        if (!textResults.isEmpty()) {
            return new SimilarNotesResult("text", textResults);
        }

        return new SimilarNotesResult("none", Collections.emptyList());
    }

    private List<NoteItem> searchByVector(String title) {
        try {
            List<SmartConnectionSearchResult> results = smartConnectionsIndexService.search(title, 5, false);
            if (results.isEmpty()) {
                return List.of();
            }
            Path vaultPath = getVaultPath();
            Path knowledgeDir = vaultPath.resolve(KNOWLEDGE_DIR);
            List<NoteItem> matched = new ArrayList<>();
            for (SmartConnectionSearchResult result : results) {
                if (result.getScore() < 0.8) {
                    continue;
                }
                String notePath = result.getSourcePath();
                if (notePath == null || notePath.isBlank()) {
                    continue;
                }
                // Smart Connections stores paths relative to vault root (e.g. "面试知识库/Go-后端/note.md")
                Path filePath = vaultPath.resolve(notePath);
                if (!Files.exists(filePath)) {
                    continue;
                }
                NoteItem item = parseNoteItem(filePath, knowledgeDir);
                if (item != null) {
                    matched.add(item);
                }
            }
            return matched;
        } catch (Exception e) {
            log.warn("Vector search failed, fallback to text: {}", e.getMessage());
            return List.of();
        }
    }

    private List<NoteItem> searchTextSimilar(String title, String direction) {
        String normalized = unquoteYamlScalar(title).trim().toLowerCase();
        Set<String> titleTerms = extractSignificantTerms(normalized);
        List<NoteItem> allNotes = listNotes(direction);
        List<NoteItem> similar = new ArrayList<>();
        for (NoteItem note : allNotes) {
            String noteTitle = unquoteYamlScalar(note.getTitle()).trim().toLowerCase();
            if (noteTitle.equals(normalized) || noteTitle.contains(normalized) || normalized.contains(noteTitle)) {
                similar.add(note);
                continue;
            }
            Set<String> noteTerms = extractSignificantTerms(noteTitle);
            noteTerms.retainAll(titleTerms);
            if (noteTerms.size() >= Math.max(2, titleTerms.size() / 2)) {
                similar.add(note);
            }
        }
        return similar;
    }

    private Set<String> extractSignificantTerms(String text) {
        Set<String> terms = new LinkedHashSet<>();
        for (String term : text.split("[\\s,，。！？?；;：:、()（）\\[\\]{}<>《》\"'`/+·|\\\\]+")) {
            String t = term.trim();
            if (t.length() >= 2) {
                terms.add(t);
            }
        }
        for (int start = 0; start < text.length(); start++) {
            for (int len = 2; len <= 4 && start + len <= text.length(); len++) {
                String sub = text.substring(start, start + len);
                if (sub.chars().allMatch(c -> Character.isLetterOrDigit(c) || c == '+' || c == '#')) {
                    terms.add(sub);
                }
            }
        }
        return terms;
    }

    public NoteItem createNote(CreateNoteRequest request) {
        Path vaultPath = getVaultPath();
        String subDir = sanitizeDirectoryName(DIRECTION_MAP.getOrDefault(request.getDirection(), request.getDirection()));
        Path knowledgeDir = vaultPath.resolve(KNOWLEDGE_DIR);
        Path dirPath = knowledgeDir.resolve(subDir);

        try {
            Files.createDirectories(dirPath);
        } catch (IOException e) {
            throw new RuntimeException("创建笔记目录失败: " + e.getMessage());
        }

        String timestamp = LocalDateTime.now().format(FILE_DATE_FMT);
        String safeName = sanitizeFileName(request.getTitle()) + "-" + timestamp + ".md";
        Path filePath = dirPath.resolve(safeName);

        String frontmatter = buildFrontmatter(request, LocalDateTime.now().format(DATE_FMT));
        String fileContent = frontmatter + "\n" + request.getContent();

        try {
            Files.writeString(filePath, fileContent, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            log.info("Note created: {}", filePath);
        } catch (IOException e) {
            throw new RuntimeException("保存笔记失败: " + e.getMessage());
        }

        return parseNoteItem(filePath, knowledgeDir);
    }

    public List<NoteItem> searchNotes(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return Collections.emptyList();
        }
        String lowerKeyword = keyword.toLowerCase();
        Path vaultPath = getVaultPath();
        Path knowledgeDir = vaultPath.resolve(KNOWLEDGE_DIR);
        if (!Files.exists(knowledgeDir)) {
            return Collections.emptyList();
        }

        List<NoteItem> results = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(knowledgeDir, 2)) {
            walk.filter(p -> p.toString().endsWith(".md"))
                    .filter(p -> Files.isRegularFile(p))
                    .filter(p -> {
                        String fileName = p.getFileName().toString().toLowerCase();
                        if (fileName.contains(lowerKeyword)) return true;
                        try {
                            if (Files.size(p) > MAX_FILE_SIZE) return false;
                            String content = Files.readString(p, StandardCharsets.UTF_8).toLowerCase();
                            return content.contains(lowerKeyword);
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .map(p -> parseNoteItem(p, knowledgeDir))
                    .filter(Objects::nonNull)
                    .forEach(results::add);
        } catch (IOException e) {
            log.error("Failed to search notes", e);
            throw new RuntimeException("搜索笔记失败: " + e.getMessage());
        }
        return results;
    }

    private Path resolveSafePath(Path basePath, String relativePath) {
        Path normalizedBase = basePath.toAbsolutePath().normalize();
        Path resolved = normalizedBase.resolve(relativePath).normalize();
        if (!resolved.startsWith(normalizedBase)) {
            throw new IllegalArgumentException("非法路径: " + relativePath);
        }
        if (!resolved.toString().endsWith(".md")) {
            throw new IllegalArgumentException("只能访问 Markdown 文件");
        }
        return resolved;
    }

    private NoteItem parseNoteItem(Path filePath, Path knowledgeDir) {
        try {
            if (Files.size(filePath) > MAX_FILE_SIZE) {
                return null;
            }
            String content = readFileContent(filePath);
            Map<String, String> fm = parseFrontmatter(content);

            String relativePath = knowledgeDir.relativize(filePath).toString().replace('\\', '/');
            String title = unquoteYamlScalar(fm.getOrDefault("title", filePath.getFileName().toString().replace(".md", "")));
            String direction = unquoteYamlScalar(fm.getOrDefault("direction", ""));
            String created = unquoteYamlScalar(fm.getOrDefault("created", ""));
            String tagsStr = fm.getOrDefault("tags", "");
            List<String> tags = tagsStr.isBlank() ? Collections.emptyList()
                    : Arrays.stream(tagsStr.split("[,\\[\\]\"\\s]+"))
                    .filter(s -> !s.isBlank())
                    .limit(20)
                    .toList();

            return new NoteItem(relativePath, title, direction, tags, created, filePath.getFileName().toString());
        } catch (Exception e) {
            log.warn("Failed to parse note: {}", filePath, e);
            return null;
        }
    }

    private Map<String, String> parseFrontmatter(String content) {
        Map<String, String> fields = new LinkedHashMap<>();
        Matcher fmMatcher = FRONTMATTER_PATTERN.matcher(content);
        if (!fmMatcher.find()) return fields;

        String yaml = fmMatcher.group(1);
        Matcher fieldMatcher = YAML_FIELD_PATTERN.matcher(yaml);
        while (fieldMatcher.find()) {
            fields.put(fieldMatcher.group(1).trim(), fieldMatcher.group(2).trim());
        }
        return fields;
    }

    private String stripFrontmatter(String content) {
        return FRONTMATTER_PATTERN.matcher(content).replaceFirst("");
    }

    private String buildFrontmatter(CreateNoteRequest request, String created) {
        String tagsStr = request.getTags() == null || request.getTags().isEmpty()
                ? "[]"
                : "[" + request.getTags().stream()
                .map(this::yamlDoubleQuote)
                .limit(20)
                .reduce((a, b) -> a + ", " + b)
                .orElse("") + "]";
        StringBuilder sb = new StringBuilder("---\n");
        sb.append("title: ").append(yamlDoubleQuote(request.getTitle())).append("\n");
        sb.append("direction: ").append(yamlDoubleQuote(request.getDirection())).append("\n");
        sb.append("tags: ").append(tagsStr).append("\n");
        sb.append("created: ").append(yamlDoubleQuote(created)).append("\n");
        sb.append("source: ").append(yamlDoubleQuote(request.getSource() != null ? request.getSource() : "interview-assistant")).append("\n");
        if (request.getQuestionId() != null && !request.getQuestionId().isBlank()) {
            sb.append("questionId: ").append(yamlDoubleQuote(request.getQuestionId())).append("\n");
        }
        if (request.getUrl() != null && !request.getUrl().isBlank()) {
            sb.append("url: ").append(yamlDoubleQuote(request.getUrl())).append("\n");
        }
        sb.append("---");
        return sb.toString();
    }

    private String yamlDoubleQuote(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private String unquoteYamlScalar(String value) {
        if (value == null) return "";
        String trimmed = value.trim();
        if (trimmed.length() >= 2) {
            char first = trimmed.charAt(0);
            char last = trimmed.charAt(trimmed.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return trimmed.substring(1, trimmed.length() - 1)
                        .replace("\\\"", "\"")
                        .replace("\\\\", "\\");
            }
        }
        return trimmed;
    }

    private String sanitizeDirectoryName(String name) {
        String safe = sanitizeFileName(name);
        return safe.isBlank() ? "未分类" : safe;
    }

    private String sanitizeFileName(String title) {
        String safe = title.replaceAll("[\\\\/:*?\"<>|#^\\[\\]{}]", "");
        safe = safe.replaceAll("\\s+", "-");
        if (safe.length() > 50) safe = safe.substring(0, 50);
        return safe.isBlank() ? "note" : safe;
    }

    private String readFileContent(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("读取文件失败: " + e.getMessage());
        }
    }
}
