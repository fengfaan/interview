package com.interviewassistant.controller;

import com.interviewassistant.service.BatchQuestionStreamService;
import com.interviewassistant.service.InterviewAiService;
import com.interviewassistant.service.InterviewStreamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InterviewControllerDeepDiveTest {

    @Mock
    private InterviewAiService interviewAiService;

    @Mock
    private InterviewStreamService interviewStreamService;

    @Mock
    private BatchQuestionStreamService batchQuestionStreamService;

    @Mock
    private Executor executor;

    private InterviewController controller;

    @BeforeEach
    void setUp() {
        controller = new InterviewController(
                interviewAiService, interviewStreamService, batchQuestionStreamService, executor);
    }

    @Test
    void streamDeepDive_delegatesToStreamService() {
        SseEmitter expectedEmitter = new SseEmitter();
        when(interviewStreamService.streamDeepDive(any())).thenReturn(expectedEmitter);

        com.interviewassistant.dto.interview.DeepDiveRequest request = new com.interviewassistant.dto.interview.DeepDiveRequest();
        request.setQuestion("什么是B+树");
        request.setContextType(com.interviewassistant.dto.interview.DeepDiveContextType.RECOMMENDED_ANSWER);
        request.setContextContent("B+树索引是...");
        request.setMessages(java.util.List.of());

        SseEmitter result = controller.streamDeepDive(request);

        assertSame(expectedEmitter, result);
        verify(interviewStreamService).streamDeepDive(request);
    }

    @Test
    void streamDeepDive_doesNotCallAiService() {
        when(interviewStreamService.streamDeepDive(any())).thenReturn(new SseEmitter());

        com.interviewassistant.dto.interview.DeepDiveRequest request = new com.interviewassistant.dto.interview.DeepDiveRequest();
        request.setQuestion("Q");
        request.setContextType(com.interviewassistant.dto.interview.DeepDiveContextType.FEEDBACK);
        request.setContextContent("feedback text");
        request.setMessages(java.util.List.of());

        controller.streamDeepDive(request);

        verifyNoInteractions(interviewAiService);
    }
}
