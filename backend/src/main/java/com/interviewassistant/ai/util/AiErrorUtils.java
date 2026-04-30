package com.interviewassistant.ai.util;

public class AiErrorUtils {

    private AiErrorUtils() {
    }

    public static boolean isRateLimit(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase();
                if (normalized.contains("429")
                        || normalized.contains("rate limit")
                        || normalized.contains("too many requests")
                        || message.contains("速率限制")
                        || message.contains("\"1302\"")
                        || message.contains("code\":\"1302")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    public static boolean isUnauthorized(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase();
                if (normalized.contains("401")
                        || normalized.contains("unauthorized")
                        || normalized.contains("invalid api key")
                        || normalized.contains("api key")
                        || message.contains("认证")
                        || message.contains("鉴权")
                        || message.contains("未授权")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    public static boolean isNetworkError(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String className = current.getClass().getName();
            String message = current.getMessage();
            if (className.contains("ResourceAccessException")
                    || className.contains("SocketException")
                    || className.contains("SocketTimeoutException")
                    || className.contains("UnknownHostException")
                    || className.contains("SSLHandshakeException")) {
                return true;
            }
            if (message != null) {
                String normalized = message.toLowerCase();
                if (normalized.contains("connection reset")
                        || normalized.contains("connection timed out")
                        || normalized.contains("read timed out")
                        || normalized.contains("unknownhost")
                        || normalized.contains("network is unreachable")
                        || normalized.contains("network connection lost")
                        || normalized.contains("connection refused")
                        || normalized.contains("premature end of chunk")
                        || normalized.contains("chunked transfer encoding")
                        || normalized.contains("\"code\":502")
                        || normalized.contains(" 502 ")
                        || normalized.startsWith("502 -")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    public static String rateLimitMessage() {
        return "AI 请求过于频繁，已触发供应商速率限制。请等待 30-60 秒后重试，或在设置里切换额度更高的模型/API Key。";
    }

    public static String unauthorizedMessage() {
        return "AI 服务认证失败，请在设置中检查 API Key 是否正确、是否过期，并保存后重试。";
    }

    public static String networkMessage() {
        return "AI 服务网络连接失败。如果当前使用 OpenRouter，通常是本机网络无法直连 openrouter.ai，请为后端配置代理 OPENROUTER_PROXY=http://127.0.0.1:7890，或切换到国内可直连的智谱模型。";
    }

    public static String errorCode(Throwable throwable) {
        if (isRateLimit(throwable)) {
            return "AI_RATE_LIMIT";
        }
        if (isUnauthorized(throwable)) {
            return "AI_UNAUTHORIZED";
        }
        if (isNetworkError(throwable)) {
            return "AI_NETWORK_ERROR";
        }
        return "AI_SERVICE_ERROR";
    }

    public static String userMessage(Throwable throwable, String fallbackMessage) {
        if (isRateLimit(throwable)) {
            return rateLimitMessage();
        }
        if (isUnauthorized(throwable)) {
            return unauthorizedMessage();
        }
        if (isNetworkError(throwable)) {
            return networkMessage();
        }
        return fallbackMessage;
    }

    public static String compactMessage(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            return throwable.getClass().getSimpleName();
        }
        return message.length() > 300 ? message.substring(0, 300) + "..." : message;
    }
}
