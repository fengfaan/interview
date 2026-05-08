package com.interviewassistant.ai.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewassistant.ai.circuitbreaker.ModelCircuitBreaker;
import com.interviewassistant.ai.circuitbreaker.FallbackService;
import com.interviewassistant.ai.exception.AiResponseFormatException;
import com.interviewassistant.ai.util.AiErrorUtils;
import com.interviewassistant.ai.util.JsonOutputUtils;
import com.interviewassistant.common.SseUtils;
import com.interviewassistant.ai.config.AiConfig;
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
    private final ModelCircuitBreaker circuitBreaker;
    private final FallbackService fallbackService;
    private static final int JSON_GENERATION_ATTEMPTS = 2;

    @Value("${app.ai.sync-timeout-ms:120000}")
    private long syncTimeoutMs;

    public record JsonResult<T>(T value, String actualModel) {
    }

    public <T> JsonResult<T> generateJson(String systemPrompt, String userPrompt, Class<T> responseType) {
        return generateJson(systemPrompt, userPrompt, responseType, syncTimeoutMs);
    }

    public <T> JsonResult<T> generateJson(String systemPrompt, String userPrompt, Class<T> responseType, long timeoutMs) {
        ensureModelAvailable();
        var converter = new BeanOutputConverter<>(responseType);
        logFinalPrompt("generateJson", systemPrompt, userPrompt + "\n\n" + converter.getFormat());
        AiResponseFormatException lastFormatError = null;
        for (int attempt = 1; attempt <= JSON_GENERATION_ATTEMPTS; attempt++) {
            ChatResponse response = callWithTimeout(() -> aiConfig.getCurrentChatClient().prompt()
                            .system(systemPrompt)
                            .user(userPrompt + "\n\n" + converter.getFormat())
                            .call()
                            .chatResponse(),
                    "结构化 AI 生成", timeoutMs);
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
                recordSuccess();
                return new JsonResult<>(value, actualModel);
            } catch (AiResponseFormatException e) {
                lastFormatError = e;
            } catch (Exception e) {
                recordIfRetryable(e);
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
        return generateText(userPrompt, syncTimeoutMs);
    }

    public String generateText(String userPrompt, long timeoutMs) {
        ensureModelAvailable();
        logFinalPrompt("generateText", null, userPrompt);
        try {
            String result = callWithTimeout(() -> aiConfig.getCurrentChatClient().prompt()
                            .user(userPrompt)
                            .call()
                            .content(),
                    "文本 AI 生成", timeoutMs);
            recordSuccess();
            return result;
        } catch (Exception e) {
            recordIfRetryable(e);
            throw e;
        }
    }

    public String generateText(String systemPrompt, String userPrompt) {
        return generateText(systemPrompt, userPrompt, syncTimeoutMs);
    }

    public String generateText(String systemPrompt, String userPrompt, long timeoutMs) {
        ensureModelAvailable();
        logFinalPrompt("generateText", systemPrompt, userPrompt);
        try {
            String result = callWithTimeout(() -> aiConfig.getCurrentChatClient().prompt()
                            .system(systemPrompt)
                            .user(userPrompt)
                            .call()
                            .content(),
                    "文本 AI 生成", timeoutMs);
            recordSuccess();
            return result;
        } catch (Exception e) {
            recordIfRetryable(e);
            throw e;
        }
    }

    public ChatResponse callWithTools(List<Message> messages, ToolCallback... toolCallbacks) {
        ensureModelAvailable();
        logFinalPrompt("callWithTools", messages);
        try {
            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .model(aiConfig.getCurrentModel())
                    .temperature(0.7)
                    .toolCallbacks(toolCallbacks)
                    .internalToolExecutionEnabled(false)
                    .build();
            Prompt prompt = new Prompt(messages, options);
            ChatResponse result = callWithTimeout(() -> aiConfig.getCurrentChatModel().call(prompt), "工具调用 AI 生成");
            recordSuccess();
            return result;
        } catch (Exception e) {
            recordIfRetryable(e);
            throw e;
        }
    }

    public void streamText(SseEmitter emitter, String systemPrompt, String userPrompt,
                           String failureMessage, String startupFailureMessage) {
        ensureModelAvailable();
        String model = aiConfig.getCurrentModel();
        logFinalPrompt("streamText", systemPrompt, userPrompt);
        try {
            AtomicBoolean closed = new AtomicBoolean(false);
            AtomicBoolean completed = new AtomicBoolean(false);
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
                                completed.set(true);
                                recordIfRetryable(error, model);
                                if (!closed.get()) {
                                    sendStreamError(emitter, error, failureMessage);
                                } else {
                                    log.debug("AI stream failed after SSE closed: {}", AiErrorUtils.compactMessage(error));
                                }
                            },
                            () -> {
                                completed.set(true);
                                if (!closed.get()) {
                                    circuitBreaker.recordSuccess(model);
                                    SseUtils.sendDone(emitter);
                                }
                            }
                    );
            Runnable cancelUpstream = () -> {
                closed.set(true);
                subscription.dispose();
            };
            emitter.onTimeout(() -> {
                if (!completed.get()) {
                    log.warn("AI stream timed out for model [{}], recording failure", model);
                    circuitBreaker.recordFailure(model);
                }
                cancelUpstream.run();
            });
            emitter.onCompletion(cancelUpstream);
            emitter.onError(error -> {
                log.debug("SSE stream closed before AI stream completed: {}", AiErrorUtils.compactMessage(error));
                cancelUpstream.run();
            });
        } catch (Exception e) {
            recordIfRetryable(e, model);
            sendStreamStartupError(emitter, e, startupFailureMessage);
        }
    }

    private <T> T callWithTimeout(Supplier<T> supplier, String operationName) {
        return callWithTimeout(supplier, operationName, syncTimeoutMs);
    }

    private <T> T callWithTimeout(Supplier<T> supplier, String operationName, long timeoutMs) {
        try {
            return CompletableFuture.supplyAsync(supplier)
                    .get(timeoutMs, TimeUnit.MILLISECONDS);
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

    private void ensureModelAvailable() {
        String currentModel = aiConfig.getCurrentModel();
        if (circuitBreaker.isOpen(currentModel)) {
            String fallback = fallbackService.fallback(currentModel);
            if (fallback == null) {
                throw new RuntimeException("所有 AI 模型均不可用，请稍后重试");
            }
        }
    }

    private void recordSuccess() {
        circuitBreaker.recordSuccess(aiConfig.getCurrentModel());
    }

    private boolean isRetryableError(Throwable error) {
        return AiErrorUtils.isRateLimit(error) || AiErrorUtils.isNetworkError(error)
                || is5xxError(error) || isTimeout(error) || isModelUnavailable(error);
    }

    private boolean isModelUnavailable(Throwable error) {
        Throwable current = error;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase();
                if (normalized.contains("not supported model")
                        || normalized.contains("model not found")
                        || normalized.contains("model is not available")
                        || normalized.contains("model has been deprecated")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean isTimeout(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof TimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean is5xxError(Throwable error) {
        Throwable current = error;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase();
                if (normalized.contains("500") || normalized.contains("502")
                        || normalized.contains("503") || normalized.contains("504")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private void recordIfRetryable(Throwable error) {
        recordIfRetryable(error, aiConfig.getCurrentModel());
    }

    private void recordIfRetryable(Throwable error, String model) {
        if (isRetryableError(error)) {
            circuitBreaker.recordFailure(model);
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

    private void logFinalPrompt(String operation, String systemPrompt, String userPrompt) {
        log.info("\n========== AI PROMPT [{}] ==========\n[SYSTEM]\n{}\n[USER]\n{}\n========== END PROMPT ==========", operation, systemPrompt, userPrompt);
    }

    private void logFinalPrompt(String operation, List<Message> messages) {
        StringBuilder sb = new StringBuilder();
        for (Message msg : messages) {
            sb.append("\n[").append(msg.getMessageType()).append("]\n").append(msg.getText());
        }
        log.info("\n========== AI PROMPT [{}] =========={}\n========== END PROMPT ==========", operation, sb);
    }
}
