package com.interviewassistant.controller;

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
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@RestController
@RequestMapping("/api/interview")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewAiService interviewService;
    private final AiConfig aiConfig;
    private final PromptService promptService;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @PostMapping("/question")
    public ApiResponse<QuestionResponse> generateQuestion(@Valid @RequestBody QuestionRequest request) {
        QuestionResponse response = interviewService.generateQuestion(
                request.getDirection(), request.getLevel(), request.getHistory());
        return ApiResponse.ok(response);
    }

    @PostMapping("/batch-questions")
    public ApiResponse<List<BatchQuestionItem>> batchQuestions(@Valid @RequestBody BatchQuestionRequest request) {
        List<BatchQuestionItem> items = interviewService.generateBatchQuestions(
                request.getDirection(), request.getLevel(), request.getCount());
        return ApiResponse.ok(items);
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
}
