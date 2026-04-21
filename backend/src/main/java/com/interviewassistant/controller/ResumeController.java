package com.interviewassistant.controller;

import com.interviewassistant.common.ApiResponse;
import com.interviewassistant.common.SseUtils;
import com.interviewassistant.dto.resume.AnalyzeRequest;
import com.interviewassistant.dto.resume.AnalyzeResponse;
import com.interviewassistant.dto.resume.RewriteStreamRequest;
import com.interviewassistant.prompt.ResumePrompts;
import com.interviewassistant.service.ResumeAiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@RestController
@RequestMapping("/api/resume")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeAiService resumeService;
    private final ChatClient chatClient;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @PostMapping("/analyze")
    public ApiResponse<AnalyzeResponse> analyze(@Valid @RequestBody AnalyzeRequest request) {
        AnalyzeResponse response = resumeService.analyze(
                request.getJobDescription(), request.getResume());
        return ApiResponse.ok(response);
    }

    @PostMapping(value = "/rewrite/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamRewrite(@Valid @RequestBody RewriteStreamRequest request) {
        SseEmitter emitter = SseUtils.createEmitter();
        String prompt = resumeService.buildRewritePrompt(
                request.getJobDescription(), request.getResume(),
                request.getSuggestion().getTitle(),
                request.getSuggestion().getSourceText());

        executor.execute(() -> {
            try {
                chatClient.prompt()
                        .system(ResumePrompts.SYSTEM_PROMPT)
                        .user(prompt)
                        .stream()
                        .content()
                        .subscribe(
                                chunk -> SseUtils.sendChunk(emitter, chunk),
                                error -> {
                                    log.error("Stream error", error);
                                    SseUtils.sendError(emitter, "AI_SERVICE_ERROR", "STAR 改写生成失败");
                                },
                                () -> SseUtils.sendDone(emitter)
                        );
            } catch (Exception e) {
                log.error("Failed to start stream", e);
                SseUtils.sendError(emitter, "AI_SERVICE_ERROR", "启动 STAR 改写失败");
            }
        });

        return emitter;
    }
}
