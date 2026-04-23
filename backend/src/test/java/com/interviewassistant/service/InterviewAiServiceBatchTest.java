package com.interviewassistant.service;

import com.interviewassistant.config.AiConfig;
import com.interviewassistant.dto.interview.BatchQuestionItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * TDD tests for batch question generation in InterviewAiService.
 *
 * Tests the core batch logic: splitting count into batches of 10,
 * calling AI per batch, merging results with correct numbering.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InterviewAiServiceBatchTest {

    @Mock
    private AiConfig aiConfig;

    @Mock
    private PromptService promptService;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    private InterviewAiService service;

    @BeforeEach
    void setUp() {
        // Mock the ChatClient chain: aiConfig.getCurrentChatClient() → chatClient.prompt() → requestSpec
        // requestSpec.system() → requestSpec, requestSpec.user() → requestSpec, requestSpec.call() → callResponseSpec
        when(aiConfig.getCurrentChatClient()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(promptService.load("interview/system.md")).thenReturn("You are an interviewer.");

        service = new InterviewAiService(aiConfig, promptService);
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
        when(callResponseSpec.content()).thenReturn(aiResponse);

        List<BatchQuestionItem> result = service.generateBatchQuestions("GO_BACKEND", "BASIC", 5);

        assertEquals(5, result.size());
        assertEquals("batch_001", result.get(0).getQuestionId());
        assertEquals("batch_005", result.get(4).getQuestionId());
        verify(chatClient, times(1)).prompt(); // 5 questions = 1 batch
    }

    @Test
    void generateBatchQuestions_withCount15_callsAiTwice() {
        // First batch returns 10, second returns 5
        String batch1Response = """
        {
          "questions": [
            {"question": "Q1", "answer": "A1", "keywords": []},
            {"question": "Q2", "answer": "A2", "keywords": []},
            {"question": "Q3", "answer": "A3", "keywords": []},
            {"question": "Q4", "answer": "A4", "keywords": []},
            {"question": "Q5", "answer": "A5", "keywords": []},
            {"question": "Q6", "answer": "A6", "keywords": []},
            {"question": "Q7", "answer": "A7", "keywords": []},
            {"question": "Q8", "answer": "A8", "keywords": []},
            {"question": "Q9", "answer": "A9", "keywords": []},
            {"question": "Q10", "answer": "A10", "keywords": []}
          ]
        }
        """;
        String batch2Response = """
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
        when(callResponseSpec.content()).thenReturn(batch1Response, batch2Response);

        List<BatchQuestionItem> result = service.generateBatchQuestions("GO_BACKEND", "BASIC", 15);

        assertEquals(15, result.size());
        assertEquals("batch_001", result.get(0).getQuestionId());
        assertEquals("batch_015", result.get(14).getQuestionId());
        verify(chatClient, times(2)).prompt(); // 15 questions = 2 batches (10 + 5)
    }

    @Test
    void generateBatchQuestions_withCount10_callsAiOnce() {
        String aiResponse = """
        {
          "questions": [
            {"question": "Q1", "answer": "A1", "keywords": []},
            {"question": "Q2", "answer": "A2", "keywords": []},
            {"question": "Q3", "answer": "A3", "keywords": []},
            {"question": "Q4", "answer": "A4", "keywords": []},
            {"question": "Q5", "answer": "A5", "keywords": []},
            {"question": "Q6", "answer": "A6", "keywords": []},
            {"question": "Q7", "answer": "A7", "keywords": []},
            {"question": "Q8", "answer": "A8", "keywords": []},
            {"question": "Q9", "answer": "A9", "keywords": []},
            {"question": "Q10", "answer": "A10", "keywords": []}
          ]
        }
        """;
        when(callResponseSpec.content()).thenReturn(aiResponse);

        List<BatchQuestionItem> result = service.generateBatchQuestions("REACT_FRONTEND", "DEEP_PRINCIPLE", 10);

        assertEquals(10, result.size());
        verify(chatClient, times(1)).prompt();
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
        when(callResponseSpec.content()).thenReturn(aiResponse);

        List<BatchQuestionItem> result = service.generateBatchQuestions("GO_BACKEND", "BASIC", 2);

        assertEquals("batch_001", result.get(0).getQuestionId());
        assertEquals("batch_002", result.get(1).getQuestionId());
    }

    @Test
    void generateBatchQuestions_usesCorrectPromptTemplate() {
        String aiResponse = """
        {"questions": [{"question": "Q1", "answer": "A1", "keywords": []}]}
        """;
        when(callResponseSpec.content()).thenReturn(aiResponse);
        when(promptService.render(eq("interview/batch-question.md"), anyMap())).thenReturn("rendered prompt");

        service.generateBatchQuestions("GO_BACKEND", "BASIC", 1);

        verify(promptService).render(eq("interview/batch-question.md"), anyMap());
    }

    @Test
    void generateBatchQuestions_returnsEmptyWhenCountIsZero() {
        List<BatchQuestionItem> result = service.generateBatchQuestions("GO_BACKEND", "BASIC", 0);

        assertTrue(result.isEmpty());
        verifyNoInteractions(chatClient);
    }
}
