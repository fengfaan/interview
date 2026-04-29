package com.interviewassistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class SmartEmbeddingClient {

    private final ObjectMapper objectMapper;
    private final Environment environment;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    private static final String DEFAULT_ENDPOINT = "http://127.0.0.1:8765/embed";
    private static final String EXPECTED_MODEL = "TaylorAI/bge-micro-v2";
    private static final int EXPECTED_DIMS = 384;

    public EmbeddingResult embed(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("查询文本不能为空");
        }
        try {
            String requestBody = objectMapper.writeValueAsString(new EmbedRequest(text));
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint()))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Embedding sidecar 返回错误: HTTP " + response.statusCode());
            }
            JsonNode root = objectMapper.readTree(response.body());
            String model = root.path("model").asText();
            JsonNode vectorNode = root.path("vector");
            if (!vectorNode.isArray()) {
                throw new IllegalStateException("Embedding sidecar 返回格式错误: vector 缺失");
            }
            double[] vector = new double[vectorNode.size()];
            for (int i = 0; i < vectorNode.size(); i++) {
                vector[i] = vectorNode.get(i).asDouble();
            }
            if (!EXPECTED_MODEL.equals(model)) {
                throw new IllegalStateException("Embedding 模型不匹配: " + model + "，期望 " + EXPECTED_MODEL);
            }
            if (vector.length != EXPECTED_DIMS) {
                throw new IllegalStateException("Embedding 维度不匹配: " + vector.length + "，期望 " + EXPECTED_DIMS);
            }
            return new EmbeddingResult(model, vector);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Embedding 请求被中断", e);
        } catch (Exception e) {
            throw new IllegalStateException("无法生成查询向量，请确认 smart-embedding sidecar 已启动: " + e.getMessage(), e);
        }
    }

    private String endpoint() {
        return environment.getProperty("app.smart-embedding.endpoint", DEFAULT_ENDPOINT);
    }

    private record EmbedRequest(String text) {
    }

    public record EmbeddingResult(String model, double[] vector) {
    }
}
