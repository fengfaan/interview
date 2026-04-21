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
import java.util.Properties;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettingsService {

    private final AiConfig aiConfig;
    private final Environment environment;

    private static final String SETTINGS_FILE = "settings.properties";
    private static final String KEY_PROP = "api-key";
    private static final String PLACEHOLDER = "not-configured";
    private final ReentrantLock fileLock = new ReentrantLock();

    @PostConstruct
    void init() {
        String savedKey = readKeyFromFile();
        if (savedKey != null && !savedKey.isBlank()) {
            aiConfig.refreshApiKey(savedKey);
            log.info("Loaded API key from settings file");
        }
    }

    private boolean isRealKey(String key) {
        return key != null && !key.isBlank() && !PLACEHOLDER.equals(key);
    }

    public String getCurrentApiKey() {
        String fromFile = readKeyFromFile();
        if (isRealKey(fromFile)) return fromFile;
        String fromEnv = environment.getProperty("spring.ai.openai.api-key");
        return isRealKey(fromEnv) ? fromEnv : null;
    }

    public String maskKey(String key) {
        if (key == null || key.length() <= 4) return "";
        return "****" + key.substring(key.length() - 4);
    }

    public void saveApiKey(String newKey) {
        fileLock.lock();
        try {
            Path path = Paths.get(SETTINGS_FILE);
            Properties props = new Properties();
            if (Files.exists(path)) {
                try (var is = Files.newInputStream(path)) {
                    props.load(is);
                }
            }
            props.setProperty(KEY_PROP, newKey);
            try (var os = Files.newOutputStream(path)) {
                props.store(os, "Interview Assistant Settings");
            }
            log.info("API key saved to settings file");
        } catch (IOException e) {
            throw new RuntimeException("Failed to save API key: " + e.getMessage(), e);
        } finally {
            fileLock.unlock();
        }
        aiConfig.refreshApiKey(newKey);
    }

    private String readKeyFromFile() {
        Path path = Paths.get(SETTINGS_FILE);
        if (!Files.exists(path)) return null;
        fileLock.lock();
        try {
            Properties props = new Properties();
            try (var is = Files.newInputStream(path)) {
                props.load(is);
            }
            return props.getProperty(KEY_PROP);
        } catch (IOException e) {
            log.warn("Failed to read settings file", e);
            return null;
        } finally {
            fileLock.unlock();
        }
    }
}
