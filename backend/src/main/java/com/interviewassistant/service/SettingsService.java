package com.interviewassistant.service;

import com.interviewassistant.config.AiConfig;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettingsService {

    private final AiConfig aiConfig;
    private final Environment environment;

    private static final String SETTINGS_FILE_NAME = "settings.properties";
    private static final String SETTINGS_FILE_PROP = "app.settings.file";
    private static final String KEY_PROP = "api-key";
    private static final String MODEL_PROP = "model";
    private static final String PLACEHOLDER = "not-configured";
    private static final List<String> MODEL_OPTIONS = List.of(
            "glm-4-flash",
            "glm-4-air",
            "glm-4-airx",
            "glm-4-plus",
            "glm-4-long",
            "glm-z1-flash"
    );
    private final ReentrantLock fileLock = new ReentrantLock();

    @PostConstruct
    void init() {
        log.info("Settings file resolved: {}", settingsPath());
        String realKey = getCurrentApiKey();
        if (realKey != null) {
            aiConfig.refreshClient(realKey, getCurrentModel());
            log.info("Loaded API key and model settings");
        } else {
            log.info("No saved API key found. Please configure it in Settings.");
        }
    }

    private boolean isRealKey(String key) {
        return key != null && !key.isBlank() && !PLACEHOLDER.equals(key);
    }

    public String getCurrentApiKey() {
        String fromFile = readPropertyFromFile(KEY_PROP);
        if (isRealKey(fromFile)) return fromFile;
        String fromEnv = environment.getProperty("spring.ai.openai.api-key");
        return isRealKey(fromEnv) ? fromEnv : null;
    }

    public String getCurrentModel() {
        String fromFile = readPropertyFromFile(MODEL_PROP);
        if (fromFile != null && !fromFile.isBlank()) return fromFile.trim();
        String fromEnv = environment.getProperty("spring.ai.openai.chat.options.model");
        if (fromEnv != null && !fromEnv.isBlank()) return fromEnv.trim();
        return AiConfig.DEFAULT_MODEL;
    }

    public String getDefaultModel() {
        return AiConfig.DEFAULT_MODEL;
    }

    public List<String> getModelOptions() {
        return MODEL_OPTIONS;
    }

    public String maskKey(String key) {
        if (key == null || key.length() <= 4) return "";
        return "****" + key.substring(key.length() - 4);
    }

    public void saveApiKey(String newKey) {
        saveSetting(KEY_PROP, newKey);
        aiConfig.refreshClient(newKey, getCurrentModel());
    }

    public void saveModel(String newModel) {
        String normalizedModel = normalizeModel(newModel);
        saveSetting(MODEL_PROP, normalizedModel);
        String realKey = getCurrentApiKey();
        if (realKey != null) {
            aiConfig.refreshClient(realKey, normalizedModel);
        }
    }

    private void saveSetting(String key, String value) {
        fileLock.lock();
        try {
            Path path = settingsPath();
            Properties props = new Properties();
            if (Files.exists(path)) {
                try (var is = Files.newInputStream(path)) {
                    props.load(is);
                }
            }
            props.setProperty(key, value);
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (var os = Files.newOutputStream(path)) {
                props.store(os, "Interview Assistant Settings");
            }
            log.info("Setting saved: {} -> {}", key, path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save setting: " + e.getMessage(), e);
        } finally {
            fileLock.unlock();
        }
    }

    private String readPropertyFromFile(String key) {
        Path path = settingsPath();
        if (!Files.exists(path)) return null;
        fileLock.lock();
        try {
            Properties props = new Properties();
            try (var is = Files.newInputStream(path)) {
                props.load(is);
            }
            return props.getProperty(key);
        } catch (IOException e) {
            log.warn("Failed to read settings file", e);
            return null;
        } finally {
            fileLock.unlock();
        }
    }

    private Path settingsPath() {
        String configured = environment.getProperty(SETTINGS_FILE_PROP);
        if (configured != null && !configured.isBlank()) {
            return Paths.get(configured).toAbsolutePath().normalize();
        }

        Path cwd = Paths.get("").toAbsolutePath().normalize();
        Path current = cwd;
        while (current != null) {
            Path repoBackendPom = current.resolve("backend").resolve("pom.xml");
            if (Files.exists(repoBackendPom)) {
                return current.resolve("backend").resolve(SETTINGS_FILE_NAME).toAbsolutePath().normalize();
            }

            Path backendPom = current.resolve("pom.xml");
            Path backendSrc = current.resolve("src").resolve("main");
            if (Files.exists(backendPom) && Files.isDirectory(backendSrc)) {
                return current.resolve(SETTINGS_FILE_NAME).toAbsolutePath().normalize();
            }

            current = current.getParent();
        }

        return cwd.resolve(SETTINGS_FILE_NAME).toAbsolutePath().normalize();
    }

    private String normalizeModel(String model) {
        String normalized = model == null ? "" : model.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("模型不能为空");
        }
        if (normalized.length() > 100) {
            throw new IllegalArgumentException("模型名称不能超过 100 个字符");
        }
        if (!normalized.matches("[A-Za-z0-9._:-]+")) {
            throw new IllegalArgumentException("模型名称只能包含字母、数字、点、下划线、冒号或短横线");
        }
        return normalized;
    }
}
