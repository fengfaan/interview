package com.interviewassistant.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    private static final String BASE_URL = "https://open.bigmodel.cn";
    private static final String COMPLETIONS_PATH = "/api/paas/v4/chat/completions";
    private static final String MODEL = "glm-4-flash";
    private static final double TEMPERATURE = 0.7;

    private volatile ChatClient currentChatClient;

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        this.currentChatClient = builder.build();
        return this.currentChatClient;
    }

    public ChatClient getCurrentChatClient() {
        return currentChatClient;
    }

    public synchronized void refreshApiKey(String newApiKey) {
        OpenAiApi api = OpenAiApi.builder()
                .apiKey(newApiKey)
                .baseUrl(BASE_URL)
                .completionsPath(COMPLETIONS_PATH)
                .build();

        OpenAiChatModel model = OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(MODEL)
                        .temperature(TEMPERATURE)
                        .build())
                .build();

        this.currentChatClient = ChatClient.builder(model).build();
    }
}
