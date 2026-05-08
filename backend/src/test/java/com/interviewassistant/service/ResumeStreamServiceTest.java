package com.interviewassistant.service;

import com.interviewassistant.ai.gateway.AiGateway;
import com.interviewassistant.ai.prompt.PromptService;
import com.interviewassistant.dto.resume.PolishStreamRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeStreamServiceTest {

    @Mock
    private AiGateway aiGateway;

    @Mock
    private PromptService promptService;

    @InjectMocks
    private ResumeStreamService resumeStreamService;

    @Test
    void streamPolish_shouldCallAiGatewayWithRenderedPrompts() {
        SseEmitter emitter = new SseEmitter();
        PolishStreamRequest request = new PolishStreamRequest();
        request.setSourceText("负责开发系统");
        request.setJobDescription("需要精通 Java");

        when(promptService.load("resume/system.md")).thenReturn("system_prompt");
        when(promptService.render(eq("resume/deep-polish.md"), any(Map.class))).thenReturn("user_prompt");

        resumeStreamService.streamPolish(emitter, request);

        verify(aiGateway).streamText(
                eq(emitter),
                eq("system_prompt"),
                eq("user_prompt"),
                anyString(),
                anyString()
        );
    }
}
