package com.interviewassistant.service;

import com.interviewassistant.ai.agent.DeepDiveAgent;
import com.interviewassistant.ai.gateway.AiGateway;
import com.interviewassistant.ai.prompt.PromptService;
import com.interviewassistant.common.SseUtils;
import com.interviewassistant.dto.interview.DeepDiveRequest;
import com.interviewassistant.dto.interview.FeedbackRequest;
import com.interviewassistant.dto.interview.RecommendedAnswerRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.Executor;

@Service
public class InterviewStreamService {

    private final InterviewAiService interviewService;
    private final PromptService promptService;
    private final AiGateway aiGateway;
    private final Executor executor;
    private final DeepDiveAgent deepDiveAgent;

    public InterviewStreamService(InterviewAiService interviewService,
                                  PromptService promptService,
                                  AiGateway aiGateway,
                                  @Qualifier("sseTaskExecutor") Executor executor,
                                  DeepDiveAgent deepDiveAgent) {
        this.interviewService = interviewService;
        this.promptService = promptService;
        this.aiGateway = aiGateway;
        this.executor = executor;
        this.deepDiveAgent = deepDiveAgent;
    }

    public SseEmitter streamFeedback(FeedbackRequest request) {
        SseEmitter emitter = SseUtils.createShortEmitter();
        String prompt = interviewService.buildFeedbackStreamPrompt(
                request.getDirection(), request.getLevel(),
                request.getQuestion(), request.getAnswer(),
                request.getExpectedKeywords(), null);
        executor.execute(() -> aiGateway.streamText(
                emitter,
                promptService.load("interview/system.md"),
                prompt,
                "流式生成失败",
                "启动流式生成失败"
        ));
        return emitter;
    }

    public SseEmitter streamRecommendedAnswer(RecommendedAnswerRequest request) {
        SseEmitter emitter = SseUtils.createShortEmitter();
        String prompt = interviewService.buildRecommendedAnswerPrompt(
                request.getDirection(), request.getLevel(),
                request.getQuestion(), request.getExpectedKeywords());
        executor.execute(() -> aiGateway.streamText(
                emitter,
                promptService.load("interview/system.md"),
                prompt,
                "推荐答案生成失败",
                "启动推荐答案生成失败"
        ));
        return emitter;
    }

    public SseEmitter streamBatchAnswer(RecommendedAnswerRequest request) {
        SseEmitter emitter = SseUtils.createShortEmitter();
        String prompt = interviewService.buildBatchAnswerPrompt(
                request.getDirection(), request.getLevel(),
                request.getQuestion(), request.getExpectedKeywords());
        executor.execute(() -> aiGateway.streamText(
                emitter,
                promptService.load("interview/system.md"),
                prompt,
                "答案生成失败",
                "启动答案生成失败"
        ));
        return emitter;
    }

    public SseEmitter streamDeepDive(DeepDiveRequest request) {
        return deepDiveAgent.execute(request.getQuestion(), request.getExpectedKeywords(),
                request.getContextType(), request.getContextContent(),
                request.getMessages());
    }
}
