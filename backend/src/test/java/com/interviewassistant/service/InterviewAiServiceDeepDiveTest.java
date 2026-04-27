package com.interviewassistant.service;

import com.interviewassistant.dto.interview.ChatMessage;
import com.interviewassistant.dto.interview.ChatRole;
import com.interviewassistant.dto.interview.DeepDiveContextType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InterviewAiServiceDeepDiveTest {

    @Mock
    private AiGateway aiGateway;

    @Mock
    private PromptService promptService;

    private InterviewAiService service;

    @BeforeEach
    void setUp() {
        service = new InterviewAiService(aiGateway, promptService, new ObjectMapper());
    }

    @Test
    void buildDeepDivePrompt_rendersWithCorrectTemplate() {
        when(promptService.render(eq("interview/deep-dive.md"), anyMap())).thenReturn("rendered");

        String result = service.buildDeepDivePrompt(
                "什么是B+树", List.of("索引", "叶子节点"),
                DeepDiveContextType.RECOMMENDED_ANSWER, "B+树索引是一种...",
                List.of(new ChatMessage() {{ setRole(ChatRole.USER); setContent("叶子节点和内节点的区别？"); }}));

        assertEquals("rendered", result);
        verify(promptService).render(eq("interview/deep-dive.md"), anyMap());
    }

    @Test
    void buildDeepDivePrompt_formatsUserMessagesAsCandidate() {
        when(promptService.render(eq("interview/deep-dive.md"), anyMap())).thenReturn("ok");

        service.buildDeepDivePrompt(
                "Q1", List.of(), DeepDiveContextType.FEEDBACK, "context",
                List.of(new ChatMessage() {{ setRole(ChatRole.USER); setContent("为什么用B+树？"); }}));

        verify(promptService).render(eq("interview/deep-dive.md"), argThat(map -> {
            String history = (String) map.get("history");
            return history != null && history.startsWith("候选人：");
        }));
    }

    @Test
    void buildDeepDivePrompt_formatsAssistantMessagesAsCoach() {
        when(promptService.render(eq("interview/deep-dive.md"), anyMap())).thenReturn("ok");

        service.buildDeepDivePrompt(
                "Q1", List.of(), DeepDiveContextType.RECOMMENDED_ANSWER, "context",
                List.of(new ChatMessage() {{ setRole(ChatRole.ASSISTANT); setContent("B+树是一种平衡树"); }}));

        verify(promptService).render(eq("interview/deep-dive.md"), argThat(map -> {
            String history = (String) map.get("history");
            return history != null && history.startsWith("教练：");
        }));
    }

    @Test
    void buildDeepDivePrompt_joinsMultipleMessagesWithDoubleNewline() {
        when(promptService.render(eq("interview/deep-dive.md"), anyMap())).thenReturn("ok");

        ChatMessage m1 = new ChatMessage();
        m1.setRole(ChatRole.USER);
        m1.setContent("问题1");
        ChatMessage m2 = new ChatMessage();
        m2.setRole(ChatRole.ASSISTANT);
        m2.setContent("回答1");
        ChatMessage m3 = new ChatMessage();
        m3.setRole(ChatRole.USER);
        m3.setContent("问题2");

        service.buildDeepDivePrompt("Q", List.of(), DeepDiveContextType.FEEDBACK, "ctx", List.of(m1, m2, m3));

        verify(promptService).render(eq("interview/deep-dive.md"), argThat(map -> {
            String history = (String) map.get("history");
            return history != null
                    && history.contains("候选人：问题1")
                    && history.contains("\n\n教练：回答1")
                    && history.contains("\n\n候选人：问题2");
        }));
    }

    @Test
    void buildDeepDivePrompt_contextTypeRecommendedAnswer_rendersChineseLabel() {
        when(promptService.render(eq("interview/deep-dive.md"), anyMap())).thenReturn("ok");

        service.buildDeepDivePrompt("Q", List.of(), DeepDiveContextType.RECOMMENDED_ANSWER, "ctx", List.of());

        verify(promptService).render(eq("interview/deep-dive.md"), argThat(map ->
                "推荐答案".equals(map.get("contextType"))));
    }

    @Test
    void buildDeepDivePrompt_contextTypeFeedback_rendersChineseLabel() {
        when(promptService.render(eq("interview/deep-dive.md"), anyMap())).thenReturn("ok");

        service.buildDeepDivePrompt("Q", List.of(), DeepDiveContextType.FEEDBACK, "ctx", List.of());

        verify(promptService).render(eq("interview/deep-dive.md"), argThat(map ->
                "反馈点评".equals(map.get("contextType"))));
    }

    @Test
    void buildDeepDivePrompt_nullKeywords_usesEmptyList() {
        when(promptService.render(eq("interview/deep-dive.md"), anyMap())).thenReturn("ok");

        service.buildDeepDivePrompt("Q", null, DeepDiveContextType.FEEDBACK, "ctx", List.of());

        verify(promptService).render(eq("interview/deep-dive.md"), argThat(map ->
                map.get("expectedKeywords") instanceof List<?> list && list.isEmpty()));
    }
}
