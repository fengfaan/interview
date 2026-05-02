package com.interviewassistant.controller;

import com.interviewassistant.common.ApiResponse;
import com.interviewassistant.ai.util.AiErrorUtils;
import com.interviewassistant.common.SseUtils;
import com.interviewassistant.dto.interview.*;
import com.interviewassistant.service.BatchQuestionStreamService;
import com.interviewassistant.service.InterviewAiService;
import com.interviewassistant.service.InterviewStreamService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@RestController
@RequestMapping("/api/interview")
public class InterviewController {

    private final InterviewAiService interviewService;
    private final InterviewStreamService interviewStreamService;
    private final BatchQuestionStreamService batchStreamService;
    private final Executor executor;

    public InterviewController(InterviewAiService interviewService,
                               InterviewStreamService interviewStreamService,
                               BatchQuestionStreamService batchStreamService,
                               @Qualifier("sseTaskExecutor") Executor executor) {
        this.interviewService = interviewService;
        this.interviewStreamService = interviewStreamService;
        this.batchStreamService = batchStreamService;
        this.executor = executor;
    }

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
        SseEmitter emitter = SseUtils.createBatchEmitter();
        AtomicBoolean closed = new AtomicBoolean(false);

        Runnable cancelBatchTasks = () -> closed.set(true);
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

        executor.execute(() -> batchStreamService.executeBatchStream(
                emitter, closed, request.getDirection(), request.getLevel(), request.getCount(),
                request.getExistingQuestions()));
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
        return interviewStreamService.streamFeedback(request);
    }

    @PostMapping(value = "/recommended-answer/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamRecommendedAnswer(@Valid @RequestBody RecommendedAnswerRequest request) {
        return interviewStreamService.streamRecommendedAnswer(request);
    }

    @PostMapping(value = "/batch-answer/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamBatchAnswer(@Valid @RequestBody RecommendedAnswerRequest request) {
        return interviewStreamService.streamBatchAnswer(request);
    }

    @PostMapping(value = "/deep-dive/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamDeepDive(@Valid @RequestBody DeepDiveRequest request) {
        return interviewStreamService.streamDeepDive(request);
    }
}
