package com.interviewassistant.service;

import com.interviewassistant.config.AiConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
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
    private static final String PROVIDER_PROP = "provider";
    private static final String MODEL_PROP = "model";
    private static final String VAULT_PATH_PROP = "obsidian.vault.path";
    private static final String PLACEHOLDER = "not-configured";
    private static final List<String> ZHIPU_MODEL_OPTIONS = List.of(
            "glm-4-flash",
            "glm-4-air",
            "glm-4-airx",
            "glm-4-plus",
            "glm-4-long",
            "glm-z1-flash"
    );
    private static final List<String> OPENROUTER_MODEL_OPTIONS = List.of(
            "openrouter/free",
            "qwen/qwen3-coder:free",
            "qwen/qwen3-next-80b-a3b-instruct:free",
            "z-ai/glm-4.5-air:free",
            "tencent/hy3-preview:free",
            "nvidia/nemotron-3-super-120b-a12b:free",
            "nvidia/nemotron-3-nano-30b-a3b:free",
            "minimax/minimax-m2.5:free",
            "google/gemma-4-31b-it:free",
            "google/gemma-4-26b-a4b-it:free",
            "meta-llama/llama-3.3-70b-instruct:free",
            "meta-llama/llama-3.2-3b-instruct:free",
            "openai/gpt-oss-120b:free",
            "openai/gpt-oss-20b:free"
    );
    private final ReentrantLock fileLock = new ReentrantLock();

    @EventListener(ApplicationReadyEvent.class)
    void init() {
        log.info("Settings file resolved: {}", settingsPath());
        String realKey = getCurrentApiKey();
        if (realKey != null) {
            aiConfig.refreshClient(realKey, getCurrentProvider(), getCurrentModel());
            log.info("Loaded AI settings. provider={}, model={}, key={}",
                    aiConfig.getCurrentProvider(), aiConfig.getCurrentModel(), aiConfig.getCurrentKeyMask());
        } else {
            aiConfig.clearClient(getCurrentProvider(), getCurrentModel());
            log.info("No saved API key found. Please configure it in Settings.");
        }
    }

    private boolean isRealKey(String key) {
        return key != null && !key.isBlank() && !PLACEHOLDER.equals(key);
    }

    public String getCurrentApiKey() {
        return getApiKeyForProvider(getCurrentProvider());
    }

    public String getApiKeyForProvider(String provider) {
        String normalizedProvider = AiConfig.normalizeProvider(provider);
        String fromFile = readPropertyFromFile(providerKeyProp(normalizedProvider));
        if (isRealKey(fromFile)) return fromFile;

        if (AiConfig.PROVIDER_ZHIPU.equals(normalizedProvider)) {
            String legacy = readPropertyFromFile(KEY_PROP);
            if (isRealKey(legacy)) return legacy;
        }

        String fromEnv = environment.getProperty(envKeyForProvider(normalizedProvider));
        return isRealKey(fromEnv) ? fromEnv : null;
    }

    public String getCurrentProvider() {
        String fromFile = readPropertyFromFile(PROVIDER_PROP);
        if (fromFile != null && !fromFile.isBlank()) {
            return AiConfig.normalizeProvider(fromFile);
        }
        String fromEnv = environment.getProperty("app.ai.provider");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return AiConfig.normalizeProvider(fromEnv);
        }
        return AiConfig.DEFAULT_PROVIDER;
    }

    public String getCurrentModel() {
        String fromFile = readPropertyFromFile(MODEL_PROP);
        if (fromFile != null && !fromFile.isBlank()) return fromFile.trim();
        String fromEnv = environment.getProperty("spring.ai.openai.chat.options.model");
        if (fromEnv != null && !fromEnv.isBlank()) return fromEnv.trim();
        return AiConfig.defaultModelFor(getCurrentProvider());
    }

    public String getDefaultModel() {
        return AiConfig.defaultModelFor(getCurrentProvider());
    }

    public List<String> getModelOptions() {
        return getModelOptions(getCurrentProvider());
    }

    public List<String> getModelOptions(String provider) {
        return AiConfig.PROVIDER_OPENROUTER.equals(AiConfig.normalizeProvider(provider))
                ? OPENROUTER_MODEL_OPTIONS
                : ZHIPU_MODEL_OPTIONS;
    }

    public String maskKey(String key) {
        if (key == null || key.length() <= 4) return "";
        return "****" + key.substring(key.length() - 4);
    }

    public void saveApiKey(String provider, String newKey) {
        String normalizedProvider = AiConfig.normalizeProvider(provider);
        saveSetting(providerKeyProp(normalizedProvider), newKey);
        if (normalizedProvider.equals(getCurrentProvider())) {
            aiConfig.refreshClient(newKey, normalizedProvider, getCurrentModel());
        }
    }

    public String getVaultPath() {
        return readPropertyFromFile(VAULT_PATH_PROP);
    }

    public boolean isVaultPathValid() {
        String path = getVaultPath();
        if (path == null || path.isBlank()) {
            return false;
        }
        Path p = Paths.get(path).toAbsolutePath().normalize();
        return Files.isDirectory(p) && Files.isReadable(p) && Files.isWritable(p);
    }

    public void saveVaultPath(String path) {
        if (path != null && !path.isBlank()) {
            Path p = Paths.get(path).toAbsolutePath().normalize();
            if (!Files.exists(p) || !Files.isDirectory(p)) {
                throw new IllegalArgumentException("路径不存在或不是有效目录");
            }
            if (!Files.isReadable(p) || !Files.isWritable(p)) {
                throw new IllegalArgumentException("路径无读写权限");
            }
        }
        saveSetting(VAULT_PATH_PROP, path == null ? "" : path);
    }

    public void saveModel(String provider, String newModel) {
        String normalizedProvider = AiConfig.normalizeProvider(provider);
        String normalizedModel = normalizeModel(newModel);
        saveSetting(PROVIDER_PROP, normalizedProvider);
        saveSetting(MODEL_PROP, normalizedModel);
        String realKey = getApiKeyForProvider(normalizedProvider);
        if (realKey != null) {
            aiConfig.refreshClient(realKey, normalizedProvider, normalizedModel);
        } else {
            aiConfig.clearClient(normalizedProvider, normalizedModel);
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
        if (!normalized.matches("[A-Za-z0-9._:/-]+")) {
            throw new IllegalArgumentException("模型名称只能包含字母、数字、点、下划线、冒号、斜杠或短横线");
        }
        return normalized;
    }

    private String providerKeyProp(String provider) {
        return KEY_PROP + "." + provider;
    }

    private String envKeyForProvider(String provider) {
        return switch (provider) {
            case AiConfig.PROVIDER_OPENROUTER -> "OPENROUTER_API_KEY";
            case AiConfig.PROVIDER_ZHIPU -> "spring.ai.openai.api-key";
            default -> "spring.ai.openai.api-key";
        };
    }
}
