package com.interviewassistant.service;

import com.interviewassistant.ai.gateway.AiGateway;
import com.interviewassistant.ai.prompt.PromptService;
import com.interviewassistant.dto.interview.BatchQuestionItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewassistant.dto.interview.InterviewDirection;
import com.interviewassistant.dto.interview.InterviewLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * TDD tests for batch question generation in InterviewAiService.
 *
 * Tests the core batch logic: splitting count into small batches,
 * calling AI per batch, merging results with correct numbering.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InterviewAiServiceBatchTest {

    @Mock
    private AiGateway aiGateway;

    @Mock
    private PromptService promptService;

    private InterviewAiService service;

    @BeforeEach
    void setUp() {
        when(promptService.load("interview/system.md")).thenReturn("You are an interviewer.");
        when(promptService.render(eq("interview/batch-question.md"), anyMap())).thenReturn("rendered prompt");

        service = new InterviewAiService(aiGateway, promptService, new ObjectMapper());
    }

    @Test
    void generateBatchQuestions_withCount5_returns5Items() {
        // AI returns 5 questions in one batch
        String aiResponse = """
        {
          "questions": [
            {"question": "Q1", "answer": "A1", "keywords": ["k1"]},
            {"question": "Q2", "answer": "A2", "keywords": ["k2"]},
            {"question": "Q3", "answer": "A3", "keywords": ["k3"]},
            {"question": "Q4", "answer": "A4", "keywords": ["k4"]},
            {"question": "Q5", "answer": "A5", "keywords": ["k5"]}
          ]
        }
        """;
        when(aiGateway.generateText(anyString(), anyString())).thenReturn(aiResponse);

        List<BatchQuestionItem> result = service.generateBatchQuestions(InterviewDirection.GO_BACKEND, InterviewLevel.BASIC, 5);

        assertEquals(5, result.size());
        assertEquals("batch_001", result.get(0).getQuestionId());
        assertEquals("batch_005", result.get(4).getQuestionId());
        verify(aiGateway, times(1)).generateText(anyString(), anyString()); // 5 questions = 1 batch
    }

    @Test
    void generateBatchQuestions_withCount15_callsAiThreeTimes() {
        // Three batches return 5 questions each
        String batch1Response = """
        {
          "questions": [
            {"question": "Q1", "answer": "A1", "keywords": []},
            {"question": "Q2", "answer": "A2", "keywords": []},
            {"question": "Q3", "answer": "A3", "keywords": []},
            {"question": "Q4", "answer": "A4", "keywords": []},
            {"question": "Q5", "answer": "A5", "keywords": []}
          ]
        }
        """;
        String batch2Response = """
        {
          "questions": [
            {"question": "Q6", "answer": "A6", "keywords": []},
            {"question": "Q7", "answer": "A7", "keywords": []},
            {"question": "Q8", "answer": "A8", "keywords": []},
            {"question": "Q9", "answer": "A9", "keywords": []},
            {"question": "Q10", "answer": "A10", "keywords": []}
          ]
        }
        """;
        String batch3Response = """
        {
          "questions": [
            {"question": "Q11", "answer": "A11", "keywords": []},
            {"question": "Q12", "answer": "A12", "keywords": []},
            {"question": "Q13", "answer": "A13", "keywords": []},
            {"question": "Q14", "answer": "A14", "keywords": []},
            {"question": "Q15", "answer": "A15", "keywords": []}
          ]
        }
        """;
        when(aiGateway.generateText(anyString(), anyString())).thenReturn(batch1Response, batch2Response, batch3Response);

        List<BatchQuestionItem> result = service.generateBatchQuestions(InterviewDirection.GO_BACKEND, InterviewLevel.BASIC, 15);

        assertEquals(15, result.size());
        assertEquals("batch_001", result.get(0).getQuestionId());
        assertEquals("batch_015", result.get(14).getQuestionId());
        verify(aiGateway, times(3)).generateText(anyString(), anyString()); // 15 questions = 3 batches (5 + 5 + 5)
    }

    @Test
    void generateBatchQuestions_withCount10_callsAiTwice() {
        String batch1Response = """
        {
          "questions": [
            {"question": "Q1", "answer": "A1", "keywords": []},
            {"question": "Q2", "answer": "A2", "keywords": []},
            {"question": "Q3", "answer": "A3", "keywords": []},
            {"question": "Q4", "answer": "A4", "keywords": []},
            {"question": "Q5", "answer": "A5", "keywords": []}
          ]
        }
        """;
        String batch2Response = """
        {
          "questions": [
            {"question": "Q6", "answer": "A6", "keywords": []},
            {"question": "Q7", "answer": "A7", "keywords": []},
            {"question": "Q8", "answer": "A8", "keywords": []},
            {"question": "Q9", "answer": "A9", "keywords": []},
            {"question": "Q10", "answer": "A10", "keywords": []}
          ]
        }
        """;
        when(aiGateway.generateText(anyString(), anyString())).thenReturn(batch1Response, batch2Response);

        List<BatchQuestionItem> result = service.generateBatchQuestions(InterviewDirection.REACT_FRONTEND, InterviewLevel.DEEP_PRINCIPLE, 10);

        assertEquals(10, result.size());
        verify(aiGateway, times(2)).generateText(anyString(), anyString());
    }

    @Test
    void generateBatchQuestions_assignsSequentialIds() {
        String aiResponse = """
        {
          "questions": [
            {"question": "What is GC?", "answer": "GC is...", "keywords": ["gc", "memory"]},
            {"question": "What is goroutine?", "answer": "Goroutine is...", "keywords": ["goroutine"]}
          ]
        }
        """;
        when(aiGateway.generateText(anyString(), anyString())).thenReturn(aiResponse);

        List<BatchQuestionItem> result = service.generateBatchQuestions(InterviewDirection.GO_BACKEND, InterviewLevel.BASIC, 2);

        assertEquals("batch_001", result.get(0).getQuestionId());
        assertEquals("batch_002", result.get(1).getQuestionId());
    }

    @Test
    void generateBatchQuestions_usesCorrectPromptTemplate() {
        String aiResponse = """
        {"questions": [{"question": "Q1", "answer": "A1", "keywords": []}]}
        """;
        when(aiGateway.generateText(anyString(), anyString())).thenReturn(aiResponse);
        when(promptService.render(eq("interview/batch-question.md"), anyMap())).thenReturn("rendered prompt");

        service.generateBatchQuestions(InterviewDirection.GO_BACKEND, InterviewLevel.BASIC, 1);

        verify(promptService).render(eq("interview/batch-question.md"), anyMap());
    }

    @Test
    void generateBatchQuestions_returnsEmptyWhenCountIsZero() {
        List<BatchQuestionItem> result = service.generateBatchQuestions(InterviewDirection.GO_BACKEND, InterviewLevel.BASIC, 0);

        assertTrue(result.isEmpty());
        verifyNoInteractions(aiGateway);
    }
}
