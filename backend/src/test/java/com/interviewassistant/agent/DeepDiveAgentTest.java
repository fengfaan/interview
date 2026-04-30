package com.interviewassistant.agent;

import com.interviewassistant.common.SseUtils;
import com.interviewassistant.dto.interview.ChatMessage;
import com.interviewassistant.dto.interview.ChatRole;
import com.interviewassistant.dto.interview.DeepDiveContextType;
import com.interviewassistant.dto.knowledge.NoteItem;
import com.interviewassistant.service.AiGateway;
import com.interviewassistant.service.InterviewAiService;
import com.interviewassistant.service.ObsidianService;
import com.interviewassistant.service.PromptService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeepDiveAgentTest {

    @Mock private AiGateway aiGateway;
    @Mock private PromptService promptService;
    @Mock private ObsidianService obsidianService;
    @Mock private InterviewAiService interviewAiService;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private KnowledgeTools knowledgeTools;
    private DeepDiveAgent agent;

    @BeforeEach
    void setUp() {
        knowledgeTools = new KnowledgeTools(obsidianService);
        agent = new DeepDiveAgent(aiGateway, promptService, executor,
                interviewAiService, knowledgeTools, objectMapper);
    }

    @Test
    void extractNoteTitles_parsesSearchResult() {
        String result = "找到 2 条相关笔记：\n- B+树索引详解 [索引] (btree.md)\n- 红黑树对比 [树结构] (rbtree.md)\n";
        List<String> titles = agent.extractNoteTitles(result);
        assertEquals(List.of("B+树索引详解", "红黑树对比"), titles);
    }

    @Test
    void extractNoteTitles_returnsEmpty_whenNoMatches() {
        List<String> titles = agent.extractNoteTitles("未找到相关笔记。");
        assertTrue(titles.isEmpty());
    }

    @Test
    void runReActLoop_noToolCalls_returnsOriginalPrompt() {
        SseEmitter emitter = SseUtils.createShortEmitter();
        List<org.springframework.ai.chat.messages.Message> chatMessages = new ArrayList<>();
        chatMessages.add(new SystemMessage("system"));
        chatMessages.add(new UserMessage("user prompt"));

        AssistantMessage assistantNoTools = new AssistantMessage("这是回答");
        ChatResponse response = new ChatResponse(List.of(new Generation(assistantNoTools)));
        when(aiGateway.callWithTools(anyList(), any(ToolCallback[].class))).thenReturn(response);

        String result = agent.runReActLoop(emitter, chatMessages);

        assertEquals("user prompt", result);
    }

    @Test
    void runReActLoop_withToolCall_appendsResults() {
        SseEmitter emitter = SseUtils.createShortEmitter();
        List<org.springframework.ai.chat.messages.Message> chatMessages = new ArrayList<>();
        chatMessages.add(new SystemMessage("system"));
        chatMessages.add(new UserMessage("user prompt"));

        AssistantMessage assistantWithTool = AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(new AssistantMessage.ToolCall(
                        "call_1", "function", "searchNotes", "{\"keyword\":\"B+树\"}")))
                .build();
        ChatResponse round1Response = new ChatResponse(List.of(new Generation(assistantWithTool)));

        AssistantMessage assistantFinal = new AssistantMessage("B+树是一种平衡树...");
        ChatResponse round2Response = new ChatResponse(List.of(new Generation(assistantFinal)));

        when(aiGateway.callWithTools(anyList(), any(ToolCallback[].class)))
                .thenReturn(round1Response, round2Response);
        when(obsidianService.isVaultConfigured()).thenReturn(true);
        NoteItem note = new NoteItem("btree.md", "B+树索引详解", "BACKEND", List.of("索引"), "2026-01-01", "btree.md");
        when(obsidianService.searchNotes("B+树")).thenReturn(List.of(note));

        String result = agent.runReActLoop(emitter, chatMessages);

        assertTrue(result.contains("知识库检索结果"));
        assertTrue(result.contains("B+树索引详解"));
        verify(obsidianService).searchNotes("B+树");
    }

    @Test
    void runReActLoop_stopsAfterMaxRounds() {
        SseEmitter emitter = SseUtils.createShortEmitter();
        List<org.springframework.ai.chat.messages.Message> chatMessages = new ArrayList<>();
        chatMessages.add(new SystemMessage("system"));
        chatMessages.add(new UserMessage("user prompt"));

        AssistantMessage assistantWithTool = AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(new AssistantMessage.ToolCall(
                        "call_1", "function", "searchNotes", "{\"keyword\":\"test\"}")))
                .build();
        ChatResponse alwaysToolCall = new ChatResponse(List.of(new Generation(assistantWithTool)));

        when(aiGateway.callWithTools(anyList(), any(ToolCallback[].class)))
                .thenReturn(alwaysToolCall);
        when(obsidianService.isVaultConfigured()).thenReturn(false);

        String result = agent.runReActLoop(emitter, chatMessages);

        verify(aiGateway, times(3)).callWithTools(anyList(), any(ToolCallback[].class));
        assertTrue(result.contains("工具调用 3"));
    }

    @Test
    void execute_answerDirectly_streamsDirectly() throws Exception {
        when(interviewAiService.buildDeepDivePrompt(anyString(), any(), any(), anyString(), any()))
                .thenReturn("built-prompt");
        when(promptService.load("interview/deep-dive-agent-system.md")).thenReturn("agent-system");

        AssistantMessage assistantNoTools = new AssistantMessage("回答");
        ChatResponse response = new ChatResponse(List.of(new Generation(assistantNoTools)));
        when(aiGateway.callWithTools(anyList(), any(ToolCallback[].class))).thenReturn(response);

        ChatMessage msg = new ChatMessage();
        msg.setRole(ChatRole.USER);
        msg.setContent("追问");

        SseEmitter emitter = agent.execute("Q", List.of(), DeepDiveContextType.RECOMMENDED_ANSWER, "ctx", List.of(msg));

        assertNotNull(emitter);
        Thread.sleep(300);
        verify(obsidianService, never()).searchNotes(anyString());
        verify(aiGateway).streamText(any(SseEmitter.class), eq("agent-system"), eq("built-prompt"), anyString(), anyString());
    }

    @Test
    void execute_searchNotes_searchesAndStreams() throws Exception {
        when(interviewAiService.buildDeepDivePrompt(anyString(), any(), any(), anyString(), any()))
                .thenReturn("built-prompt");
        when(promptService.load("interview/deep-dive-agent-system.md")).thenReturn("agent-system");

        AssistantMessage assistantWithTool = AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(new AssistantMessage.ToolCall(
                        "call_1", "function", "searchNotes", "{\"keyword\":\"B+树\"}")))
                .build();
        ChatResponse round1 = new ChatResponse(List.of(new Generation(assistantWithTool)));

        AssistantMessage assistantFinal = new AssistantMessage("回答");
        ChatResponse round2 = new ChatResponse(List.of(new Generation(assistantFinal)));

        when(aiGateway.callWithTools(anyList(), any(ToolCallback[].class)))
                .thenReturn(round1, round2);
        when(obsidianService.isVaultConfigured()).thenReturn(true);
        NoteItem note = new NoteItem("btree.md", "B+树索引详解", "BACKEND", List.of("索引"), "2026-01-01", "btree.md");
        when(obsidianService.searchNotes("B+树")).thenReturn(List.of(note));

        ChatMessage msg = new ChatMessage();
        msg.setRole(ChatRole.USER);
        msg.setContent("B+树怎么实现范围查询？");

        SseEmitter emitter = agent.execute("Q", List.of(), DeepDiveContextType.RECOMMENDED_ANSWER, "ctx", List.of(msg));

        assertNotNull(emitter);
        Thread.sleep(300);
        verify(obsidianService).searchNotes("B+树");
        verify(aiGateway).streamText(any(SseEmitter.class), eq("agent-system"), contains("B+树索引详解"), anyString(), anyString());
    }
}
