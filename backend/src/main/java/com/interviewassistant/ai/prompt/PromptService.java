package com.interviewassistant.ai.prompt;

import com.interviewassistant.ai.exception.PromptLoadException;
import com.interviewassistant.dto.settings.PromptFileResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PromptService {

    private final Path promptDirectory;
    private final Map<String, CachedPrompt> cache = new ConcurrentHashMap<>();

    private record CachedPrompt(String content, long lastModifiedMillis) {
    }

    public PromptService(@Value("${app.prompts.directory:prompts}") String promptDirectory) {
        Path configuredDirectory = Path.of(promptDirectory).toAbsolutePath().normalize();
        Path repoRootDirectory = Path.of("backend", "prompts").toAbsolutePath().normalize();
        if ("prompts".equals(promptDirectory) && !Files.isDirectory(configuredDirectory)
                && Files.isDirectory(repoRootDirectory)) {
            this.promptDirectory = repoRootDirectory;
        } else {
            this.promptDirectory = configuredDirectory;
        }
    }

    public String load(String relativePath) {
        Path path = resolvePromptPath(relativePath);
        try {
            long lastModified = Files.getLastModifiedTime(path).toMillis();
            String cacheKey = promptDirectory.relativize(path).toString().replace('\\', '/');
            CachedPrompt cached = cache.get(cacheKey);
            if (cached != null && cached.lastModifiedMillis() == lastModified) {
                return cached.content();
            }
            String content = Files.readString(path, StandardCharsets.UTF_8);
            cache.put(cacheKey, new CachedPrompt(content, lastModified));
            return content;
        } catch (NoSuchFileException e) {
            throw new PromptLoadException("提示词文件不存在: " + path, e);
        } catch (IOException e) {
            throw new PromptLoadException("读取提示词文件失败: " + path, e);
        }
    }

    public String render(String relativePath, Map<String, ?> variables) {
        String prompt = load(relativePath);
        for (Map.Entry<String, ?> entry : variables.entrySet()) {
            prompt = prompt.replace("{{" + entry.getKey() + "}}", String.valueOf(entry.getValue()));
        }
        return prompt;
    }

    public List<PromptFileResponse> listFiles() {
        if (!Files.isDirectory(promptDirectory)) {
            throw new PromptLoadException("提示词目录不存在: " + promptDirectory);
        }
        try (var stream = Files.walk(promptDirectory)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".md"))
                    .map(this::toPromptFileResponse)
                    .sorted(Comparator.comparing(PromptFileResponse::getGroup)
                            .thenComparing(PromptFileResponse::getName))
                    .toList();
        } catch (IOException e) {
            throw new PromptLoadException("读取提示词目录失败: " + promptDirectory, e);
        }
    }

    public void save(String relativePath, String content) {
        Path path = resolvePromptPath(relativePath);
        if (!Files.exists(path)) {
            throw new PromptLoadException("只能保存已存在的提示词文件: " + path);
        }
        try {
            Files.writeString(path, content, StandardCharsets.UTF_8);
            cache.remove(promptDirectory.relativize(path).toString().replace('\\', '/'));
        } catch (IOException e) {
            throw new PromptLoadException("保存提示词文件失败: " + path, e);
        }
    }

    private Path resolvePromptPath(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new PromptLoadException("提示词路径不能为空");
        }
        Path relative = Path.of(relativePath.replace('\\', '/'));
        if (relative.isAbsolute()) {
            throw new PromptLoadException("提示词路径不能是绝对路径: " + relativePath);
        }
        Path path = promptDirectory.resolve(relative).normalize();
        if (!path.startsWith(promptDirectory)) {
            throw new PromptLoadException("非法提示词路径: " + relativePath);
        }
        if (!path.getFileName().toString().endsWith(".md")) {
            throw new PromptLoadException("只支持 Markdown 提示词文件: " + relativePath);
        }
        return path;
    }

    private PromptFileResponse toPromptFileResponse(Path path) {
        try {
            String relativePath = promptDirectory.relativize(path).toString().replace('\\', '/');
            String[] parts = relativePath.split("/");
            String group = parts.length > 1 ? parts[0] : "root";
            String name = path.getFileName().toString();
            return new PromptFileResponse(
                    relativePath,
                    group,
                    name,
                    Files.size(path),
                    Files.getLastModifiedTime(path).toInstant().toString()
            );
        } catch (IOException e) {
            throw new PromptLoadException("读取提示词文件信息失败: " + path, e);
        }
    }
}
