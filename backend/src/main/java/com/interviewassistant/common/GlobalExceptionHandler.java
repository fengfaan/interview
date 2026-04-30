package com.interviewassistant.common;

import com.interviewassistant.ai.exception.AiResponseFormatException;
import com.interviewassistant.ai.exception.PromptLoadException;
import com.interviewassistant.ai.util.AiErrorUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Invalid request");
        return ResponseEntity.badRequest().body(ApiResponse.fail("BAD_REQUEST", msg));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableMessage(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail("BAD_REQUEST", "请求参数格式错误，请检查枚举值和 JSON 结构"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArg(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(ApiResponse.fail("BAD_REQUEST", e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.fail("API_KEY_NOT_CONFIGURED", e.getMessage()));
    }

    @ExceptionHandler(PromptLoadException.class)
    public ResponseEntity<ApiResponse<Void>> handlePromptLoad(PromptLoadException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail("PROMPT_LOAD_ERROR", e.getMessage()));
    }

    @ExceptionHandler(AiResponseFormatException.class)
    public ResponseEntity<ApiResponse<Void>> handleAiResponseFormat(AiResponseFormatException e) {
        log.warn("AI response format error: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.fail(e.getErrorCode(), e.getUserMessage()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResource(NoResourceFoundException e) {
        log.warn("API route not found: {}", e.getResourcePath());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.fail("API_NOT_FOUND", "接口不存在或后端尚未更新，请重启后端服务后重试"));
    }

    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public void handleAsyncTimeout(AsyncRequestTimeoutException e) {
        log.warn("SSE request timed out");
    }

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleAsyncNotUsable(AsyncRequestNotUsableException e) {
        log.warn("SSE response already closed: {}", AiErrorUtils.compactMessage(e));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception e) {
        if (AiErrorUtils.isRateLimit(e)) {
            log.warn("AI rate limit: {}", AiErrorUtils.compactMessage(e));
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.fail("AI_RATE_LIMIT", AiErrorUtils.rateLimitMessage()));
        }
        if (AiErrorUtils.isUnauthorized(e)) {
            log.warn("AI unauthorized: {}", AiErrorUtils.compactMessage(e));
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail("AI_UNAUTHORIZED", AiErrorUtils.unauthorizedMessage()));
        }
        if (AiErrorUtils.isNetworkError(e)) {
            log.warn("AI network error: {}", AiErrorUtils.compactMessage(e));
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.fail("AI_NETWORK_ERROR", AiErrorUtils.networkMessage()));
        }

        log.error("Unexpected error: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.fail("AI_SERVICE_ERROR", "AI 服务调用失败，请稍后重试"));
    }
}
