package com.interviewassistant.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class AiConfig {

    private static final String BASE_URL = "https://open.bigmodel.cn";
    private static final String COMPLETIONS_PATH = "/api/paas/v4/chat/completions";
    public static final String DEFAULT_MODEL = "glm-4-flash";
    private static final double TEMPERATURE = 0.7;

    private volatile ChatClient currentChatClient;
    private volatile boolean realKeyConfigured = false;
    private volatile String currentModel = DEFAULT_MODEL;
    private volatile String currentKeyMask = "";

    public ChatClient getCurrentChatClient() {
        if (!realKeyConfigured || currentChatClient == null) {
            throw new IllegalStateException("请先在设置页面配置 API Key");
        }
        return currentChatClient;
    }

    public boolean isConfigured() {
        return realKeyConfigured;
    }

    public String getCurrentModel() {
        return currentModel;
    }

    public String getCurrentKeyMask() {
        return currentKeyMask;
    }

    public synchronized void refreshApiKey(String newApiKey) {
        refreshClient(newApiKey, currentModel);
    }

    public synchronized void refreshClient(String newApiKey, String modelName) {
        String normalizedModel = modelName == null || modelName.isBlank() ? DEFAULT_MODEL : modelName.trim();
        if (newApiKey == null || newApiKey.isBlank()) {
            throw new IllegalArgumentException("API Key 不能为空");
        }

        RestClient.Builder restClientBuilder = RestClient.builder()
                .requestFactory(new HttpComponentsClientHttpRequestFactory());

        OpenAiApi api = OpenAiApi.builder()
                .apiKey(newApiKey)
                .baseUrl(BASE_URL)
                .completionsPath(COMPLETIONS_PATH)
                .restClientBuilder(restClientBuilder)
                .build();

        OpenAiChatModel model = OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(normalizedModel)
                        .temperature(TEMPERATURE)
                        .build())
                .build();

        this.currentChatClient = ChatClient.builder(model).build();
        this.currentModel = normalizedModel;
        this.currentKeyMask = maskKey(newApiKey);
        this.realKeyConfigured = true;
        log.info("AI client refreshed. model={}, key={}", normalizedModel, currentKeyMask);
    }

    private String maskKey(String key) {
        if (key == null || key.length() <= 4) {
            return "****";
        }
        return "****" + key.substring(key.length() - 4);
    }
}
