package com.interviewassistant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewassistant.common.ApiResponse;
import com.interviewassistant.dto.interview.BatchQuestionRequest;
import com.interviewassistant.service.InterviewAiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class InterviewControllerBatchTest {

    @Mock
    private InterviewAiService interviewAiService;

    private InterviewController controller;

    @BeforeEach
    void setUp() {
        controller = new InterviewController(interviewAiService, null, null, new ObjectMapper());
    }

    @Test
    void batchQuestions_returnsGoneAndRequiresStreamEndpoint() {
        ResponseEntity<ApiResponse<Void>> response = controller.batchQuestions(new BatchQuestionRequest());

        assertEquals(HttpStatus.GONE, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("STREAM_REQUIRED", response.getBody().getError());
        verifyNoInteractions(interviewAiService);
    }
}
