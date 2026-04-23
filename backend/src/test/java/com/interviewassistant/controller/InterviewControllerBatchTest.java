package com.interviewassistant.controller;

import com.interviewassistant.common.ApiResponse;
import com.interviewassistant.dto.interview.BatchQuestionItem;
import com.interviewassistant.service.InterviewAiService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InterviewControllerBatchTest {

    @Mock
    private InterviewAiService interviewAiService;

    @InjectMocks
    private InterviewController controller;

    @Test
    void batchQuestions_returnsQuestionsFromService() {
        List<BatchQuestionItem> mockItems = List.of(
                new BatchQuestionItem("batch_001", "What is Go?", "Go is...", List.of("go", "language")),
                new BatchQuestionItem("batch_002", "What is a goroutine?", "A goroutine is...", List.of("goroutine"))
        );
        when(interviewAiService.generateBatchQuestions("GO_BACKEND", "BASIC", 2)).thenReturn(mockItems);

        ApiResponse<List<BatchQuestionItem>> response = controller.batchQuestions(
                new com.interviewassistant.dto.interview.BatchQuestionRequest() {{
                    setDirection("GO_BACKEND");
                    setLevel("BASIC");
                    setCount(2);
                }}
        );

        assertTrue(response.isSuccess());
        assertEquals(2, response.getData().size());
        assertEquals("batch_001", response.getData().get(0).getQuestionId());
        verify(interviewAiService).generateBatchQuestions("GO_BACKEND", "BASIC", 2);
    }

    @Test
    void batchQuestions_withZeroCount_returnsEmptyList() {
        when(interviewAiService.generateBatchQuestions("GO_BACKEND", "BASIC", 0))
                .thenReturn(Collections.emptyList());

        ApiResponse<List<BatchQuestionItem>> response = controller.batchQuestions(
                new com.interviewassistant.dto.interview.BatchQuestionRequest() {{
                    setDirection("GO_BACKEND");
                    setLevel("BASIC");
                    setCount(0);
                }}
        );

        assertTrue(response.isSuccess());
        assertTrue(response.getData().isEmpty());
    }
}
