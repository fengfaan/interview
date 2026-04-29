package com.interviewassistant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewassistant.common.AiResponseFormatException;
import com.interviewassistant.common.AiErrorUtils;
import com.interviewassistant.common.JsonOutputUtils;
import com.interviewassistant.common.SseUtils;
import com.interviewassistant.config.AiConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiGateway {

    private final AiConfig aiConfig;
    private final ObjectMapper objectMapper;
    private static final int JSON_GENERATION_ATTEMPTS = 2;

    @Value("${app.ai.sync-timeout-ms:120000}")
    private long syncTimeoutMs;

    public record JsonResult<T>(T value, String actualModel) {
    }

    public <T> JsonResult<T> generateJson(String systemPrompt, String userPrompt, Class<T> responseType) {
        var converter = new BeanOutputConverter<>(responseType);
        AiResponseFormatException lastFormatError = null;
        for (int attempt = 1; attempt <= JSON_GENERATION_ATTEMPTS; attempt++) {
            ChatResponse response = callWithTimeout(() -> aiConfig.getCurrentChatClient().prompt()
                            .system(systemPrompt)
                            .user(userPrompt + "\n\n" + converter.getFormat())
                            .call()
                            .chatResponse(),
                    "结构化 AI 生成");
            try {
                String rawText = response.getResult().getOutput().getText();
                String json = JsonOutputUtils.extractJson(rawText);
                if (json.isBlank()) {
                    throw new AiResponseFormatException(
                            "AI_EMPTY_RESPONSE",
                            "AI 返回了空内容，请稍后重试，或在设置中切换到更稳定的模型。"
                    );
                }
                T value = objectMapper.readValue(json, responseType);
                String actualModel = response.getMetadata() != null ? response.getMetadata().getModel() : null;
                return new JsonResult<>(value, actualModel);
            } catch (AiResponseFormatException e) {
                lastFormatError = e;
            } catch (Exception e) {
                lastFormatError = new AiResponseFormatException(
                        "AI_RESPONSE_FORMAT_ERROR",
                        "AI 返回内容格式不符合要求，请重试；如果频繁出现，请切换模型或优化提示词。",
                        e
                );
            }

            if (attempt < JSON_GENERATION_ATTEMPTS) {
                log.warn("AI JSON response parse failed, retrying. attempt={}/{}, reason={}",
                        attempt, JSON_GENERATION_ATTEMPTS, lastFormatError.getMessage());
            }
        }
        throw lastFormatError;
    }

    public String generateText(String userPrompt) {
        return callWithTimeout(() -> aiConfig.getCurrentChatClient().prompt()
                        .user(userPrompt)
                        .call()
                        .content(),
                "文本 AI 生成");
    }

    public String generateText(String systemPrompt, String userPrompt) {
        return callWithTimeout(() -> aiConfig.getCurrentChatClient().prompt()
                        .system(systemPrompt)
                        .user(userPrompt)
                        .call()
                        .content(),
                "文本 AI 生成");
    }

    public ChatResponse callWithTools(List<Message> messages, ToolCallback... toolCallbacks) {
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(aiConfig.getCurrentModel())
                .temperature(0.7)
                .toolCallbacks(toolCallbacks)
                .internalToolExecutionEnabled(false)
                .build();
        Prompt prompt = new Prompt(messages, options);
        return callWithTimeout(() -> aiConfig.getCurrentChatModel().call(prompt), "工具调用 AI 生成");
    }

    public void streamText(SseEmitter emitter, String systemPrompt, String userPrompt,
                           String failureMessage, String startupFailureMessage) {
        try {
            AtomicBoolean closed = new AtomicBoolean(false);
            Disposable subscription = aiConfig.getCurrentChatClient().prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .stream()
                    .content()
                    .subscribe(
                            chunk -> {
                                if (!closed.get()) {
                                    SseUtils.sendChunk(emitter, chunk);
                                }
                            },
                            error -> {
                                if (!closed.get()) {
                                    sendStreamError(emitter, error, failureMessage);
                                } else {
                                    log.debug("AI stream failed after SSE closed: {}", AiErrorUtils.compactMessage(error));
                                }
                            },
                            () -> {
                                if (!closed.get()) {
                                    SseUtils.sendDone(emitter);
                                }
                            }
                    );
            Runnable cancelUpstream = () -> {
                closed.set(true);
                subscription.dispose();
            };
            emitter.onTimeout(cancelUpstream);
            emitter.onCompletion(cancelUpstream);
            emitter.onError(error -> {
                log.debug("SSE stream closed before AI stream completed: {}", AiErrorUtils.compactMessage(error));
                cancelUpstream.run();
            });
        } catch (Exception e) {
            sendStreamStartupError(emitter, e, startupFailureMessage);
        }
    }

    private <T> T callWithTimeout(Supplier<T> supplier, String operationName) {
        try {
            return CompletableFuture.supplyAsync(supplier)
                    .get(syncTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new RuntimeException(operationName + "超时，请稍后重试或切换响应更快的模型", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(operationName + "已中断", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new RuntimeException(operationName + "失败", cause);
        }
    }

    private void sendStreamError(SseEmitter emitter, Throwable error, String fallbackMessage) {
        if (AiErrorUtils.isRateLimit(error) || AiErrorUtils.isUnauthorized(error) || AiErrorUtils.isNetworkError(error)) {
            log.warn("AI stream expected error: {}", AiErrorUtils.compactMessage(error));
            SseUtils.sendAiError(emitter, error, fallbackMessage);
        } else {
            log.error("AI stream error", error);
            SseUtils.sendError(emitter, "AI_SERVICE_ERROR", fallbackMessage);
        }
    }

    private void sendStreamStartupError(SseEmitter emitter, Exception error, String fallbackMessage) {
        if (AiErrorUtils.isRateLimit(error) || AiErrorUtils.isUnauthorized(error) || AiErrorUtils.isNetworkError(error)) {
            log.warn("AI stream startup expected error: {}", AiErrorUtils.compactMessage(error));
            SseUtils.sendAiError(emitter, error, fallbackMessage);
        } else {
            log.error("Failed to start AI stream", error);
            SseUtils.sendError(emitter, "AI_SERVICE_ERROR", fallbackMessage);
        }
    }
}
