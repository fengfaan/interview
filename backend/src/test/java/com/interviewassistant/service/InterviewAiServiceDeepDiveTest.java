package com.interviewassistant.service;

import com.interviewassistant.ai.gateway.AiGateway;
import com.interviewassistant.ai.prompt.PromptService;
import com.interviewassistant.ai.style.StyleService;
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

    @Mock
    private StyleService styleService;

    private InterviewAiService service;

    @BeforeEach
    void setUp() {
        service = new InterviewAiService(aiGateway, promptService, styleService, new ObjectMapper());
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

    @Test
    void buildDeepDivePrompt_longContext_keepsKeywordsAndCausalLines() {
        when(promptService.render(eq("interview/deep-dive.md"), anyMap())).thenReturn("ok");

        String longContext = "# 背题答案\n"
                + "B+树的叶子节点通过链表连接。\n"
                + "因为范围查询需要顺序扫描相邻数据页，所以叶子节点链表可以减少随机 IO。\n"
                + "这是一句普通铺垫。".repeat(1_000);

        service.buildDeepDivePrompt(
                "为什么数据库索引用 B+ 树", List.of("叶子节点", "范围查询"),
                DeepDiveContextType.RECOMMENDED_ANSWER, longContext, List.of());

        verify(promptService).render(eq("interview/deep-dive.md"), argThat(map -> {
            String context = (String) map.get("contextContent");
            return context != null
                    && context.length() <= 4_000
                    && context.contains("叶子节点")
                    && context.contains("因为范围查询需要顺序扫描相邻数据页")
                    && context.contains("上下文压缩说明");
        }));
    }

    @Test
    void buildDeepDivePrompt_longContext_keepsQuestionRelatedContentWithoutKeywords() {
        when(promptService.render(eq("interview/deep-dive.md"), anyMap())).thenReturn("ok");

        ChatMessage latestQuestion = new ChatMessage();
        latestQuestion.setRole(ChatRole.USER);
        latestQuestion.setContent("为什么红黑树不适合数据库磁盘 IO？");

        String longContext = "普通铺垫。".repeat(600)
                + "红黑树通常适合内存中的动态查找结构。"
                + "因为数据库索引要尽量减少磁盘 IO，所以 B+ 树更关注树高和页内扇出。"
                + "普通结尾。".repeat(600);

        service.buildDeepDivePrompt(
                "为什么数据库索引用 B+ 树", List.of(),
                DeepDiveContextType.RECOMMENDED_ANSWER, longContext, List.of(latestQuestion));

        verify(promptService).render(eq("interview/deep-dive.md"), argThat(map -> {
            String context = (String) map.get("contextContent");
            return context != null
                    && context.length() <= 4_000
                    && context.contains("红黑树通常适合内存中的动态查找结构")
                    && context.contains("因为数据库索引要尽量减少磁盘 IO");
        }));
    }

    @Test
    void buildDeepDivePrompt_longMarkdownLine_splitsIntoSentencesBeforeSelecting() {
        when(promptService.render(eq("interview/deep-dive.md"), anyMap())).thenReturn("ok");

        String longContext = "普通说明。".repeat(500)
                + "事务隔离级别的本质是控制并发读写的一致性风险。"
                + "例如可重复读要处理不可重复读，串行化会降低并发能力。"
                + "无关铺垫。".repeat(500);

        service.buildDeepDivePrompt(
                "事务隔离级别解决什么问题", List.of("事务", "隔离级别"),
                DeepDiveContextType.FEEDBACK, longContext, List.of());

        verify(promptService).render(eq("interview/deep-dive.md"), argThat(map -> {
            String context = (String) map.get("contextContent");
            return context != null
                    && context.length() <= 4_000
                    && context.contains("事务隔离级别的本质是控制并发读写的一致性风险")
                    && context.contains("例如可重复读要处理不可重复读");
        }));
    }

    @Test
    void buildDeepDivePrompt_longHistory_keepsRecentMessagesOnly() {
        when(promptService.render(eq("interview/deep-dive.md"), anyMap())).thenReturn("ok");

        List<ChatMessage> messages = java.util.stream.IntStream.rangeClosed(1, 14)
                .mapToObj(i -> {
                    ChatMessage message = new ChatMessage();
                    message.setRole(i % 2 == 0 ? ChatRole.ASSISTANT : ChatRole.USER);
                    message.setContent("消息" + i);
                    return message;
                })
                .toList();

        service.buildDeepDivePrompt("Q", List.of(), DeepDiveContextType.FEEDBACK, "ctx", messages);

        verify(promptService).render(eq("interview/deep-dive.md"), argThat(map -> {
            String history = (String) map.get("history");
            return history != null
                    && !history.contains("候选人：消息1\n")
                    && !history.contains("教练：消息2\n")
                    && history.contains("消息3")
                    && history.contains("消息14");
        }));
    }

    @Test
    void buildDeepDivePrompt_longMessage_isCompacted() {
        when(promptService.render(eq("interview/deep-dive.md"), anyMap())).thenReturn("ok");

        ChatMessage message = new ChatMessage();
        message.setRole(ChatRole.USER);
        message.setContent("为什么".repeat(700));

        service.buildDeepDivePrompt("Q", List.of(), DeepDiveContextType.FEEDBACK, "ctx", List.of(message));

        verify(promptService).render(eq("interview/deep-dive.md"), argThat(map -> {
            String history = (String) map.get("history");
            return history != null
                    && history.length() < 1_300
                    && history.contains("已压缩");
        }));
    }

    @Test
    void buildDeepDivePrompt_nullMessages_returnsEmptyHistory() {
        when(promptService.render(eq("interview/deep-dive.md"), anyMap())).thenReturn("ok");

        service.buildDeepDivePrompt("Q", List.of(), DeepDiveContextType.FEEDBACK, "ctx", null);

        verify(promptService).render(eq("interview/deep-dive.md"), argThat(map -> {
            String history = (String) map.get("history");
            return history != null && history.isEmpty();
        }));
    }

    @Test
    void buildDeepDivePrompt_emptyMessages_returnsEmptyHistory() {
        when(promptService.render(eq("interview/deep-dive.md"), anyMap())).thenReturn("ok");

        service.buildDeepDivePrompt("Q", List.of(), DeepDiveContextType.FEEDBACK, "ctx", List.of());

        verify(promptService).render(eq("interview/deep-dive.md"), argThat(map -> {
            String history = (String) map.get("history");
            return history != null && history.isEmpty();
        }));
    }

    @Test
    void buildDeepDivePrompt_shortContext_returnsUnchanged() {
        when(promptService.render(eq("interview/deep-dive.md"), anyMap())).thenReturn("ok");

        String shortContext = "B+树是一种平衡树结构。";

        service.buildDeepDivePrompt("Q", List.of("索引"), DeepDiveContextType.FEEDBACK, shortContext, List.of());

        verify(promptService).render(eq("interview/deep-dive.md"), argThat(map -> {
            String context = (String) map.get("contextContent");
            return context != null && context.equals(shortContext);
        }));
    }

    @Test
    void buildDeepDivePrompt_nullContext_returnsEmptyContext() {
        when(promptService.render(eq("interview/deep-dive.md"), anyMap())).thenReturn("ok");

        service.buildDeepDivePrompt("Q", List.of(), DeepDiveContextType.FEEDBACK, null, List.of());

        verify(promptService).render(eq("interview/deep-dive.md"), argThat(map -> {
            String context = (String) map.get("contextContent");
            return context != null && context.isEmpty();
        }));
    }

    @Test
    void buildDeepDivePrompt_allLowValueContext_usesFallbackWithHeadTail() {
        when(promptService.render(eq("interview/deep-dive.md"), anyMap())).thenReturn("ok");

        String lowValueContext = "继续加油。".repeat(800) + "整体不错，表达清晰。".repeat(800);

        service.buildDeepDivePrompt("Q", List.of(), DeepDiveContextType.FEEDBACK, lowValueContext, List.of());

        verify(promptService).render(eq("interview/deep-dive.md"), argThat(map -> {
            String context = (String) map.get("contextContent");
            return context != null
                    && context.contains("原文前半")
                    && context.contains("原文后半")
                    && context.contains("省略");
        }));
    }
}
