package com.interviewassistant.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.client.reactive.JdkClientHttpConnector;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;

@Slf4j
@Configuration
public class AiConfig {

    public static final String PROVIDER_ZHIPU = "zhipu";
    public static final String PROVIDER_OPENROUTER = "openrouter";
    private static final String ZHIPU_BASE_URL = "https://open.bigmodel.cn";
    private static final String ZHIPU_COMPLETIONS_PATH = "/api/paas/v4/chat/completions";
    private static final String OPENROUTER_BASE_URL = "https://openrouter.ai/api/v1";
    private static final String OPENROUTER_COMPLETIONS_PATH = "/chat/completions";
    public static final String DEFAULT_PROVIDER = PROVIDER_ZHIPU;
    public static final String DEFAULT_MODEL = "glm-4-flash";
    public static final String OPENROUTER_DEFAULT_MODEL = "openrouter/free";
    public static final String PROVIDER_MIMO = "mimo";
    private static final String MIMO_BASE_URL = "https://token-plan-cn.xiaomimimo.com/v1";
    private static final String MIMO_COMPLETIONS_PATH = "/chat/completions";
    public static final String MIMO_DEFAULT_MODEL = "mimo-v2-flash";
    private static final double TEMPERATURE = 0.7;
    private static final int CONNECT_TIMEOUT_MS = 20_000;
    private static final int READ_TIMEOUT_MS = 90_000;

    @Value("${app.openrouter.proxy:${OPENROUTER_PROXY:}}")
    private String openRouterProxy;

    private volatile ChatClient currentChatClient;
    private volatile boolean realKeyConfigured = false;
    private volatile String currentProvider = DEFAULT_PROVIDER;
    private volatile String currentModel = DEFAULT_MODEL;
    private volatile String currentKeyMask = "";
    private volatile OpenAiChatModel currentChatModel;

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

    public String getCurrentProvider() {
        return currentProvider;
    }

    public String getCurrentKeyMask() {
        return currentKeyMask;
    }

    public OpenAiChatModel getCurrentChatModel() {
        if (!realKeyConfigured || currentChatModel == null) {
            throw new IllegalStateException("请先在设置页面配置 API Key");
        }
        return currentChatModel;
    }

    public String getOpenRouterProxy() {
        return openRouterProxy;
    }

    public synchronized void refreshApiKey(String newApiKey) {
        refreshClient(newApiKey, currentProvider, currentModel);
    }

    public synchronized void clearClient(String provider, String modelName) {
        this.currentProvider = normalizeProvider(provider);
        this.currentModel = modelName == null || modelName.isBlank()
                ? defaultModelFor(this.currentProvider)
                : modelName.trim();
        this.currentChatClient = null;
        this.currentChatModel = null;
        this.currentKeyMask = "";
        this.realKeyConfigured = false;
        log.info("AI client cleared. provider={}, model={}", currentProvider, currentModel);
    }

    public synchronized void refreshClient(String newApiKey, String provider, String modelName) {
        String normalizedProvider = normalizeProvider(provider);
        String normalizedModel = modelName == null || modelName.isBlank()
                ? defaultModelFor(normalizedProvider)
                : modelName.trim();
        if (newApiKey == null || newApiKey.isBlank()) {
            throw new IllegalArgumentException("API Key 不能为空");
        }

        RestClient.Builder restClientBuilder = RestClient.builder()
                .requestFactory(requestFactoryFor(normalizedProvider));
        WebClient.Builder webClientBuilder = WebClient.builder();
        if (PROVIDER_OPENROUTER.equals(normalizedProvider)) {
            restClientBuilder.defaultHeader("Accept-Encoding", "identity");
            restClientBuilder.defaultHeader("X-Title", "Interview Assistant");
            webClientBuilder.defaultHeader("Accept-Encoding", "identity");
            webClientBuilder.defaultHeader("X-Title", "Interview Assistant");
            webClientBuilder.clientConnector(new JdkClientHttpConnector(httpClientForOpenRouter()));
        }

        OpenAiApi api = OpenAiApi.builder()
                .apiKey(newApiKey)
                .baseUrl(baseUrlFor(normalizedProvider))
                .completionsPath(completionsPathFor(normalizedProvider))
                .restClientBuilder(restClientBuilder)
                .webClientBuilder(webClientBuilder)
                .build();

        OpenAiChatModel.Builder modelBuilder = OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(normalizedModel)
                        .temperature(TEMPERATURE)
                        .build());
        if (PROVIDER_OPENROUTER.equals(normalizedProvider)) {
            modelBuilder.retryTemplate(openRouterRetryTemplate());
        }
        OpenAiChatModel model = modelBuilder.build();
        this.currentChatModel = model;

        this.currentChatClient = ChatClient.builder(model).build();
        this.currentProvider = normalizedProvider;
        this.currentModel = normalizedModel;
        this.currentKeyMask = maskKey(newApiKey);
        this.realKeyConfigured = true;
        log.info("AI client refreshed. provider={}, model={}, key={}",
                normalizedProvider, normalizedModel, currentKeyMask);
    }

    public static String normalizeProvider(String provider) {
        if (provider == null || provider.isBlank()) {
            return DEFAULT_PROVIDER;
        }
        String normalized = provider.trim().toLowerCase();
        return switch (normalized) {
            case PROVIDER_ZHIPU, PROVIDER_OPENROUTER, PROVIDER_MIMO -> normalized;
            default -> throw new IllegalArgumentException("不支持的 AI 渠道: " + provider);
        };
    }

    public static String defaultModelFor(String provider) {
        String p = normalizeProvider(provider);
        if (PROVIDER_OPENROUTER.equals(p)) return OPENROUTER_DEFAULT_MODEL;
        if (PROVIDER_MIMO.equals(p)) return MIMO_DEFAULT_MODEL;
        return DEFAULT_MODEL;
    }

    private String baseUrlFor(String provider) {
        if (PROVIDER_OPENROUTER.equals(provider)) return OPENROUTER_BASE_URL;
        if (PROVIDER_MIMO.equals(provider)) return MIMO_BASE_URL;
        return ZHIPU_BASE_URL;
    }

    private String completionsPathFor(String provider) {
        if (PROVIDER_OPENROUTER.equals(provider)) return OPENROUTER_COMPLETIONS_PATH;
        if (PROVIDER_MIMO.equals(provider)) return MIMO_COMPLETIONS_PATH;
        return ZHIPU_COMPLETIONS_PATH;
    }

    private ClientHttpRequestFactory requestFactoryFor(String provider) {
        if (PROVIDER_OPENROUTER.equals(provider)) {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
            factory.setReadTimeout(READ_TIMEOUT_MS);
            if (openRouterProxy != null && !openRouterProxy.isBlank()) {
                factory.setProxy(parseProxy(openRouterProxy));
                URI proxyUri = parseProxyUri(openRouterProxy);
                log.info("OpenRouter proxy enabled: {}://{}:{}",
                        proxyUri.getScheme(), proxyUri.getHost(), proxyUri.getPort());
            }
            return factory;
        }

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setConnectionRequestTimeout(CONNECT_TIMEOUT_MS);
        return factory;
    }

    private HttpClient httpClientForOpenRouter() {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(CONNECT_TIMEOUT_MS));
        if (openRouterProxy != null && !openRouterProxy.isBlank()) {
            URI proxyUri = parseProxyUri(openRouterProxy);
            builder.proxy(java.net.ProxySelector.of(new InetSocketAddress(proxyUri.getHost(), proxyUri.getPort())));
        }
        return builder.build();
    }

    private RetryTemplate openRouterRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(new SimpleRetryPolicy(2));

        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(800);
        backOffPolicy.setMultiplier(2.0);
        backOffPolicy.setMaxInterval(3_000);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        return retryTemplate;
    }

    private Proxy parseProxy(String proxyValue) {
        URI uri = parseProxyUri(proxyValue);
        return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(uri.getHost(), uri.getPort()));
    }

    private URI parseProxyUri(String proxyValue) {
        String normalized = proxyValue.trim();
        if (!normalized.contains("://")) {
            normalized = "http://" + normalized;
        }
        URI uri = URI.create(normalized);
        if (uri.getHost() == null || uri.getPort() < 0) {
            throw new IllegalArgumentException("OpenRouter 代理格式错误，请使用 http://127.0.0.1:7890");
        }
        return uri;
    }

    private String maskKey(String key) {
        if (key == null || key.length() <= 4) {
            return "****";
        }
        return "****" + key.substring(key.length() - 4);
    }
}
