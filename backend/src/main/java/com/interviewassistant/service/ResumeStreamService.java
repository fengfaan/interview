package com.interviewassistant.service;

import com.interviewassistant.ai.gateway.AiGateway;
import com.interviewassistant.ai.prompt.PromptService;
import com.interviewassistant.common.SseUtils;
import com.interviewassistant.dto.resume.RewriteStreamRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.Executor;

@Service
public class ResumeStreamService {

    private final ResumeAiService resumeService;
    private final PromptService promptService;
    private final AiGateway aiGateway;
    private final Executor executor;

    public ResumeStreamService(ResumeAiService resumeService,
                               PromptService promptService,
                               AiGateway aiGateway,
                               @Qualifier("sseTaskExecutor") Executor executor) {
        this.resumeService = resumeService;
        this.promptService = promptService;
        this.aiGateway = aiGateway;
        this.executor = executor;
    }

    public SseEmitter streamRewrite(RewriteStreamRequest request) {
        SseEmitter emitter = SseUtils.createShortEmitter();
        String prompt = resumeService.buildRewritePrompt(
                request.getJobDescription(), request.getResume(),
                request.getSuggestion().getTitle(),
                request.getSuggestion().getSourceText());
        executor.execute(() -> aiGateway.streamText(
                emitter,
                promptService.load("resume/system.md"),
                prompt,
                "STAR 改写生成失败",
                "启动 STAR 改写失败"
        ));
        return emitter;
    }
}
