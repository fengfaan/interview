package com.interviewassistant.ai.style;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewassistant.dto.interview.InterviewDirection;
import com.interviewassistant.dto.interview.InterviewLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class StyleService {

    private final Path stylesDirectory;
    private final ObjectMapper objectMapper;

    public StyleService(@Value("${app.prompts.directory:prompts}") String promptDirectory,
                        ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        Path configuredDirectory = Path.of(promptDirectory).toAbsolutePath().normalize();
        Path repoRootDirectory = Path.of("backend", "prompts").toAbsolutePath().normalize();
        Path baseDir;
        if ("prompts".equals(promptDirectory) && !Files.isDirectory(configuredDirectory)
                && Files.isDirectory(repoRootDirectory)) {
            baseDir = repoRootDirectory;
        } else {
            baseDir = configuredDirectory;
        }
        this.stylesDirectory = baseDir.resolve("styles");
    }

    public StyleProfile loadProfile(InterviewDirection direction, InterviewLevel level) {
        Path file = resolveProfilePath(direction, level);
        if (!Files.exists(file)) {
            return new StyleProfile("", "", "");
        }
        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            return objectMapper.readValue(json, StyleProfile.class);
        } catch (IOException e) {
            log.warn("Failed to read style profile {}/{}: {}", direction, level, e.getMessage());
            return new StyleProfile("", "", "");
        }
    }

    public void saveProfile(InterviewDirection direction, InterviewLevel level, StyleProfile profile) {
        Path file = resolveProfilePath(direction, level);
        try {
            Files.createDirectories(file.getParent());
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(profile);
            Files.writeString(file, json, StandardCharsets.UTF_8);
            log.info("Style profile saved: {}/{}", direction, level);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save style profile: " + e.getMessage(), e);
        }
    }

    public String buildStyleInstruction(InterviewDirection direction, InterviewLevel level) {
        StyleProfile profile = loadProfile(direction, level);
        StringBuilder sb = new StringBuilder();
        if (isNotBlank(profile.getFocusAreas())) {
            sb.append("出题侧重：").append(profile.getFocusAreas().trim()).append("。");
        }
        if (isNotBlank(profile.getScenarioPreference())) {
            sb.append("场景偏好：").append(profile.getScenarioPreference().trim()).append("。");
        }
        if (isNotBlank(profile.getKeywordStyle())) {
            sb.append("关键词风格：").append(profile.getKeywordStyle().trim()).append("。");
        }
        return sb.toString();
    }

    public List<String> listDirections() {
        List<String> directions = new ArrayList<>();
        if (!Files.isDirectory(stylesDirectory)) {
            return directions;
        }
        try (var stream = Files.list(stylesDirectory)) {
            stream.filter(Files::isDirectory)
                  .map(p -> p.getFileName().toString())
                  .forEach(directions::add);
        } catch (IOException e) {
            log.warn("Failed to list style directories: {}", e.getMessage());
        }
        return directions;
    }

    private Path resolveProfilePath(InterviewDirection direction, InterviewLevel level) {
        return stylesDirectory.resolve(direction.name()).resolve(level.name() + ".json");
    }

    private boolean isNotBlank(String s) {
        return s != null && !s.isBlank();
    }
}
