package com.interviewassistant.controller;

import com.interviewassistant.common.ApiResponse;
import com.interviewassistant.common.SseUtils;
import com.interviewassistant.dto.interview.*;
import com.interviewassistant.service.InterviewAiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.interviewassistant.prompt.InterviewPrompts;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@RestController
@RequestMapping("/api/interview")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewAiService interviewService;
    private final org.springframework.ai.chat.client.ChatClient chatClient;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @PostMapping("/question")
    public ApiResponse<QuestionResponse> generateQuestion(@Valid @RequestBody QuestionRequest request) {
        QuestionResponse response = interviewService.generateQuestion(
                request.getDirection(), request.getLevel(), request.getHistory());
        return ApiResponse.ok(response);
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
                chatClient.prompt()
                        .system(InterviewPrompts.SYSTEM_PROMPT)
                        .user(prompt)
                        .stream()
                        .content()
                        .subscribe(
                                chunk -> SseUtils.sendChunk(emitter, chunk),
                                error -> {
                                    log.error("Stream error", error);
                                    SseUtils.sendError(emitter, "AI_SERVICE_ERROR", "流式生成失败");
                                },
                                () -> SseUtils.sendDone(emitter)
                        );
            } catch (Exception e) {
                log.error("Failed to start stream", e);
                SseUtils.sendError(emitter, "AI_SERVICE_ERROR", "启动流式生成失败");
            }
        });

        return emitter;
    }
}
