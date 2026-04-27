package com.interviewassistant.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@Slf4j
public class SseUtils {

    public static final long SHORT_STREAM_TIMEOUT = 120_000L;
    public static final long BATCH_STREAM_TIMEOUT = 600_000L;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static SseEmitter createEmitter() {
        return createShortEmitter();
    }

    public static SseEmitter createShortEmitter() {
        return new SseEmitter(SHORT_STREAM_TIMEOUT);
    }

    public static SseEmitter createBatchEmitter() {
        return new SseEmitter(BATCH_STREAM_TIMEOUT);
    }

    public static void sendChunk(SseEmitter emitter, String text) {
        try {
            emitter.send(SseEmitter.event().data(text, MediaType.TEXT_PLAIN));
        } catch (Exception e) {
            log.debug("SSE chunk send failed: {}", AiErrorUtils.compactMessage(e));
        }
    }

    public static void sendProgress(SseEmitter emitter, String text) {
        try {
            emitter.send(SseEmitter.event().name("progress").data(text, MediaType.TEXT_PLAIN));
        } catch (Exception e) {
            log.debug("SSE progress send failed: {}", AiErrorUtils.compactMessage(e));
        }
    }

    public static void sendDone(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event().data("[DONE]"));
        } catch (Exception ignored) {
            return;
        }
        completeQuietly(emitter);
    }

    public static void sendError(SseEmitter emitter, String errorCode, String message) {
        try {
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data(OBJECT_MAPPER.writeValueAsString(Map.of(
                            "error", errorCode == null ? "" : errorCode,
                            "message", message == null ? "" : message
                    )), MediaType.APPLICATION_JSON));
        } catch (Exception ignored) {
            return;
        }
        completeQuietly(emitter);
    }

    public static void sendAiError(SseEmitter emitter, Throwable error, String fallbackMessage) {
        sendError(emitter, AiErrorUtils.errorCode(error), AiErrorUtils.userMessage(error, fallbackMessage));
    }

    private static void completeQuietly(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (Exception ignored) {
        }
    }
}
