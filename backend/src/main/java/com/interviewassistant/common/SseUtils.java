package com.interviewassistant.common;

import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

public class SseUtils {

    public static final long SSE_TIMEOUT = 120_000L;

    public static SseEmitter createEmitter() {
        return new SseEmitter(SSE_TIMEOUT);
    }

    public static void sendChunk(SseEmitter emitter, String text) {
        try {
            emitter.send(SseEmitter.event().data(text, MediaType.TEXT_PLAIN));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }

    public static void sendDone(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event().data("[DONE]"));
            emitter.complete();
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }

    public static void sendError(SseEmitter emitter, String errorCode, String message) {
        try {
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data("{\"error\":\"" + escapeJson(errorCode) + "\",\"message\":\"" + escapeJson(message) + "\"}",
                            MediaType.APPLICATION_JSON));
        } catch (IOException ignored) {
        }
        emitter.complete();
    }

    public static void sendAiError(SseEmitter emitter, Throwable error, String fallbackMessage) {
        sendError(emitter, AiErrorUtils.errorCode(error), AiErrorUtils.userMessage(error, fallbackMessage));
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
