package com.interviewassistant.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class AiConfig {

    private static final String BASE_URL = "https://open.bigmodel.cn";
    private static final String COMPLETIONS_PATH = "/api/paas/v4/chat/completions";
    private static final String MODEL = "glm-4-flash";
    private static final double TEMPERATURE = 0.7;

    private volatile ChatClient currentChatClient;
    private volatile boolean realKeyConfigured = false;

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        this.currentChatClient = builder.build();
        return this.currentChatClient;
    }

    public ChatClient getCurrentChatClient() {
        if (!realKeyConfigured) {
            throw new IllegalStateException("请先在设置页面配置 API Key");
        }
        return currentChatClient;
    }

    public boolean isConfigured() {
        return realKeyConfigured;
    }

    public synchronized void refreshApiKey(String newApiKey) {
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
                        .model(MODEL)
                        .temperature(TEMPERATURE)
                        .build())
                .build();

        this.currentChatClient = ChatClient.builder(model).build();
        this.realKeyConfigured = true;
    }
}
