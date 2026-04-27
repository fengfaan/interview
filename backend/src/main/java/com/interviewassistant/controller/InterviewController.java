package com.interviewassistant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewassistant.common.ApiResponse;
import com.interviewassistant.common.AiErrorUtils;
import com.interviewassistant.common.SseUtils;
import com.interviewassistant.config.AiConfig;
import com.interviewassistant.dto.interview.*;
import com.interviewassistant.service.InterviewAiService;
import com.interviewassistant.service.PromptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@RestController
@RequestMapping("/api/interview")
@RequiredArgsConstructor
public class InterviewController {

    private static final int MAX_CONSECUTIVE_BATCH_FAILURES = 3;
    private static final long BATCH_INTERVAL_MS = 400L;

    private final InterviewAiService interviewService;
    private final AiConfig aiConfig;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @PostMapping("/question")
    public ApiResponse<QuestionResponse> generateQuestion(@Valid @RequestBody QuestionRequest request) {
        QuestionResponse response = interviewService.generateQuestion(
                request.getDirection(), request.getLevel(), request.getHistory());
        return ApiResponse.ok(response);
    }

    @PostMapping("/batch-questions")
    public ResponseEntity<ApiResponse<Void>> batchQuestions(@Valid @RequestBody BatchQuestionRequest request) {
        return ResponseEntity.status(HttpStatus.GONE)
                .body(ApiResponse.fail("STREAM_REQUIRED", "批量背题已改为流式生成，请刷新页面后重试"));
    }

    @PostMapping(value = "/batch-questions/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamBatchQuestions(@Valid @RequestBody BatchQuestionRequest request) {
        SseEmitter emitter = SseUtils.createEmitter();
        AtomicBoolean closed = new AtomicBoolean(false);

        Runnable cancelBatchTasks = () -> {
            closed.set(true);
        };
        emitter.onTimeout(() -> {
            log.warn("Batch question stream timed out");
            cancelBatchTasks.run();
            emitter.complete();
        });
        emitter.onCompletion(cancelBatchTasks);
        emitter.onError(error -> {
            log.warn("Batch question stream closed: {}", AiErrorUtils.compactMessage(error));
            cancelBatchTasks.run();
        });

        executor.execute(() -> {
            int batchSize = interviewService.batchSize();
            int total = request.getCount();
            int batches = (total + batchSize - 1) / batchSize;
            if (!sendBatchEvent(emitter, "progress", new BatchProgress("批量出题已开始", total, batchSize, batches, 0, 0))) {
                cancelBatchTasks.run();
                return;
            }

            int emitted = 0;
            int consecutiveFailures = 0;
            Exception lastError = null;
            List<Integer> failedBatches = new java.util.ArrayList<>();
            for (int batch = 0; batch < batches; batch++) {
                if (closed.get()) {
                    return;
                }
                try {
                    int batchNumber = batch + 1;
                    int startIndex = batch * batchSize + 1;
                    int batchCount = Math.min(batchSize, total - batch * batchSize);
                    if (!sendBatchEvent(emitter, "batch_start",
                            new BatchStart(batchNumber, batches, startIndex, batchCount, total, emitted))) {
                        cancelBatchTasks.run();
                        return;
                    }
                    List<BatchQuestionItem> chunk = interviewService.generateBatchQuestionChunk(
                            request.getDirection(), request.getLevel(), batchCount, batchNumber, startIndex);
                    if (chunk.isEmpty()) {
                        consecutiveFailures++;
                        failedBatches.add(batchNumber);
                        if (!sendBatchEvent(emitter, "batch_error",
                                new BatchError(batchNumber, "第 " + batchNumber + " 批没有生成有效题目", emitted, total))) {
                            cancelBatchTasks.run();
                            return;
                        }
                        if (consecutiveFailures >= MAX_CONSECUTIVE_BATCH_FAILURES) {
                            SseUtils.sendError(emitter, "BATCH_GENERATION_FAILED", "连续多批生成失败，请稍后重试或减少题目数量");
                            return;
                        }
                        pauseBetweenBatches(batch, batches);
                        continue;
                    }
                    emitted += chunk.size();
                    consecutiveFailures = 0;
                    if (!sendBatchEvent(emitter, "batch", new BatchPayload(batchNumber, chunk, emitted, total))) {
                        cancelBatchTasks.run();
                        return;
                    }
                    pauseBetweenBatches(batch, batches);
                } catch (Exception e) {
                    lastError = e;
                    consecutiveFailures++;
                    int batchNumber = batch + 1;
                    failedBatches.add(batchNumber);
                    log.warn("Batch question chunk failed: {}", AiErrorUtils.compactMessage(e));
                    if (AiErrorUtils.isRateLimit(e) || AiErrorUtils.isUnauthorized(e)) {
                        cancelBatchTasks.run();
                        SseUtils.sendAiError(emitter, e, "批量出题失败");
                        return;
                    }
                    if (!sendBatchEvent(emitter, "batch_error",
                            new BatchError(batchNumber, "第 " + batchNumber + " 批生成失败，已跳过继续生成", emitted, total))) {
                        cancelBatchTasks.run();
                        return;
                    }
                    if (consecutiveFailures >= MAX_CONSECUTIVE_BATCH_FAILURES) {
                        SseUtils.sendError(emitter, "BATCH_GENERATION_FAILED", "连续多批生成失败，请稍后重试或减少题目数量");
                        return;
                    }
                    pauseBetweenBatches(batch, batches);
                }
            }

            if (closed.get()) {
                return;
            }
            if (emitted == 0) {
                SseUtils.sendAiError(emitter,
                        lastError != null ? lastError : new IllegalStateException("All batch chunks failed"),
                        "批量出题失败，请稍后重试或减少题目数量");
                return;
            }
            if (!sendBatchEvent(emitter, "done", new BatchDone(total, emitted, failedBatches))) {
                cancelBatchTasks.run();
                return;
            }
            emitter.complete();
        });
        return emitter;
    }

    @PostMapping("/feedback")
    public ApiResponse<FeedbackResponse> analyzeFeedback(@Valid @RequestBody FeedbackRequest request) {
        if (request.getAnswer().isBlank()) {
            throw new IllegalArgumentException("回答内容不能为空");
        }
        FeedbackResponse response = interviewService.analyzeFeedback(
                request.getDirection(), request.getLevel(),
                request.getQuestion(), request.getAnswer(),
                request.getExpectedKeywords());
        return ApiResponse.ok(response);
    }

    @PostMapping(value = "/feedback/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamFeedback(@Valid @RequestBody FeedbackRequest request) {
        if (request.getAnswer().isBlank()) {
            throw new IllegalArgumentException("回答内容不能为空");
        }

        SseEmitter emitter = SseUtils.createEmitter();
        String prompt = interviewService.buildFeedbackStreamPrompt(
                request.getDirection(), request.getLevel(),
                request.getQuestion(), request.getAnswer(),
                request.getExpectedKeywords(), null);

        executor.execute(() -> {
            try {
                aiConfig.getCurrentChatClient().prompt()
                        .system(promptService.load("interview/system.md"))
                        .user(prompt)
                        .stream()
                        .content()
                        .subscribe(
                                chunk -> SseUtils.sendChunk(emitter, chunk),
                                error -> {
                                    if (AiErrorUtils.isRateLimit(error) || AiErrorUtils.isUnauthorized(error)) {
                                        log.warn("AI stream expected error: {}", AiErrorUtils.compactMessage(error));
                                        SseUtils.sendAiError(emitter, error, "流式生成失败");
                                    } else {
                                        log.error("Stream error", error);
                                        SseUtils.sendError(emitter, "AI_SERVICE_ERROR", "流式生成失败");
                                    }
                                },
                                () -> SseUtils.sendDone(emitter)
                        );
            } catch (Exception e) {
                if (AiErrorUtils.isRateLimit(e) || AiErrorUtils.isUnauthorized(e)) {
                    log.warn("AI stream startup expected error: {}", AiErrorUtils.compactMessage(e));
                    SseUtils.sendAiError(emitter, e, "启动流式生成失败");
                } else {
                    log.error("Failed to start stream", e);
                    SseUtils.sendError(emitter, "AI_SERVICE_ERROR", "启动流式生成失败");
                }
            }
        });

        return emitter;
    }

    @PostMapping(value = "/recommended-answer/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamRecommendedAnswer(@Valid @RequestBody RecommendedAnswerRequest request) {
        SseEmitter emitter = SseUtils.createEmitter();
        String prompt = interviewService.buildRecommendedAnswerPrompt(
                request.getDirection(), request.getLevel(),
                request.getQuestion(), request.getExpectedKeywords());

        executor.execute(() -> {
            try {
                aiConfig.getCurrentChatClient().prompt()
                        .system(promptService.load("interview/system.md"))
                        .user(prompt)
                        .stream()
                        .content()
                        .subscribe(
                                chunk -> SseUtils.sendChunk(emitter, chunk),
                                error -> {
                                    if (AiErrorUtils.isRateLimit(error) || AiErrorUtils.isUnauthorized(error)) {
                                        log.warn("AI recommended answer expected error: {}", AiErrorUtils.compactMessage(error));
                                        SseUtils.sendAiError(emitter, error, "推荐答案生成失败");
                                    } else {
                                        log.error("Recommended answer stream error", error);
                                        SseUtils.sendError(emitter, "AI_SERVICE_ERROR", "推荐答案生成失败");
                                    }
                                },
                                () -> SseUtils.sendDone(emitter)
                        );
            } catch (Exception e) {
                if (AiErrorUtils.isRateLimit(e) || AiErrorUtils.isUnauthorized(e)) {
                    log.warn("AI recommended answer startup expected error: {}", AiErrorUtils.compactMessage(e));
                    SseUtils.sendAiError(emitter, e, "启动推荐答案生成失败");
                } else {
                    log.error("Failed to start recommended answer stream", e);
                    SseUtils.sendError(emitter, "AI_SERVICE_ERROR", "启动推荐答案生成失败");
                }
            }
        });

        return emitter;
    }

    private boolean sendBatchEvent(SseEmitter emitter, String eventName, Object payload) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(objectMapper.writeValueAsString(payload), MediaType.APPLICATION_JSON));
            return true;
        } catch (Exception e) {
            log.warn("Batch question stream send failed: {}", AiErrorUtils.compactMessage(e));
            return false;
        }
    }

    private void pauseBetweenBatches(int batchIndex, int batches) {
        if (batchIndex >= batches - 1) {
            return;
        }
        try {
            Thread.sleep(BATCH_INTERVAL_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("批量出题已中断", e);
        }
    }

    private record BatchProgress(String message, int total, int batchSize, int batches, int generated, int failed) {
    }

    private record BatchStart(int batchNumber, int totalBatches, int startIndex, int count, int total, int generated) {
    }

    private record BatchPayload(int batchNumber, List<BatchQuestionItem> items, int generated, int total) {
    }

    private record BatchError(int batchNumber, String message, int generated, int total) {
    }

    private record BatchDone(int requested, int generated, List<Integer> failedBatches) {
    }
}
