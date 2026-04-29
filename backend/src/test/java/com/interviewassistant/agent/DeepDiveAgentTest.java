package com.interviewassistant.agent;

import com.interviewassistant.dto.interview.ChatMessage;
import com.interviewassistant.dto.interview.ChatRole;
import com.interviewassistant.dto.interview.DeepDiveContextType;
import com.interviewassistant.dto.knowledge.NoteItem;
import com.interviewassistant.service.AiGateway;
import com.interviewassistant.service.InterviewAiService;
import com.interviewassistant.service.ObsidianService;
import com.interviewassistant.service.PromptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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
    private DeepDiveAgent agent;

    @BeforeEach
    void setUp() {
        agent = new DeepDiveAgent(aiGateway, promptService, obsidianService, executor, interviewAiService);
    }

    @Test
    void decide_returnsAgentDecision_whenValidJson() {
        AgentDecision decision = new AgentDecision();
        decision.setAction("search_notes");
        decision.setKeyword("B+树");
        decision.setReason("用户追问B+树底层结构");

        when(promptService.render(eq("interview/deep-dive-agent-decide.md"), anyMap())).thenReturn("rendered prompt");
        when(aiGateway.generateJson(eq("你是一个决策助手。"), anyString(), eq(AgentDecision.class)))
                .thenReturn(new AiGateway.JsonResult<>(decision, "test-model"));

        AgentDecision result = agent.decide("什么是B+树", List.of("索引"), "B+树叶子节点", "上下文摘要...");
        assertTrue(result.wantsSearch());
        assertEquals("B+树", result.getKeyword());
    }

    @Test
    void decide_answerDirectly_doesNotWantSearch() {
        AgentDecision decision = new AgentDecision();
        decision.setAction("answer_directly");
        decision.setReason("上下文已充分");

        when(promptService.render(anyString(), anyMap())).thenReturn("prompt");
        when(aiGateway.generateJson(anyString(), anyString(), eq(AgentDecision.class)))
                .thenReturn(new AiGateway.JsonResult<>(decision, "test-model"));

        AgentDecision result = agent.decide("Q", List.of(), "追问", "上下文");
        assertFalse(result.wantsSearch());
    }

    @Test
    void decide_fallsBackToAnswerDirectly_whenJsonParseFails() {
        when(promptService.render(anyString(), anyMap())).thenReturn("prompt");
        when(aiGateway.generateJson(anyString(), anyString(), eq(AgentDecision.class)))
                .thenThrow(new RuntimeException("JSON parse failed"));

        AgentDecision result = agent.decide("Q", List.of(), "追问", "上下文");
        assertFalse(result.wantsSearch());
        assertEquals("answer_directly", result.getAction());
    }

    @Test
    void searchKnowledge_returnsNoteTitles() {
        NoteItem note = new NoteItem("test.md", "B+树索引详解", "BACKEND", List.of("索引"), "2026-01-01", "test.md");
        when(obsidianService.isVaultConfigured()).thenReturn(true);
        when(obsidianService.searchNotes("B+树")).thenReturn(List.of(note));

        List<NoteItem> results = agent.searchKnowledge("B+树");
        assertEquals(1, results.size());
        assertEquals("B+树索引详解", results.get(0).getTitle());
    }

    @Test
    void searchKnowledge_returnsEmptyList_whenVaultNotConfigured() {
        when(obsidianService.isVaultConfigured()).thenReturn(false);

        List<NoteItem> results = agent.searchKnowledge("B+树");
        assertTrue(results.isEmpty());
    }

    @Test
    void searchKnowledge_returnsEmptyList_onException() {
        when(obsidianService.isVaultConfigured()).thenReturn(true);
        when(obsidianService.searchNotes(anyString())).thenThrow(new RuntimeException("vault error"));

        List<NoteItem> results = agent.searchKnowledge("B+树");
        assertTrue(results.isEmpty());
    }

    @Test
    void buildSearchContext_formatsNotesCorrectly() {
        NoteItem note = new NoteItem("btree.md", "B+树索引详解", "BACKEND", List.of("索引"), "2026-01-01", "btree.md");
        String context = agent.buildSearchContext(List.of(note));
        assertTrue(context.contains("B+树索引详解"));
        assertTrue(context.contains("btree.md"));
    }

    @Test
    void buildSearchContext_returnsEmptyString_whenNoNotes() {
        String context = agent.buildSearchContext(List.of());
        assertEquals("", context);
    }

    @Test
    void execute_answerDirectly_streamsDirectly() throws Exception {
        AgentDecision decision = new AgentDecision();
        decision.setAction("answer_directly");
        decision.setReason("上下文已充分");

        when(interviewAiService.buildDeepDivePrompt(anyString(), any(), any(), anyString(), any()))
                .thenReturn("built-prompt");
        when(promptService.render(anyString(), anyMap())).thenReturn("decision-prompt");
        when(aiGateway.generateJson(anyString(), anyString(), eq(AgentDecision.class)))
                .thenReturn(new AiGateway.JsonResult<>(decision, "test-model"));
        when(promptService.load("interview/system.md")).thenReturn("system-prompt");

        ChatMessage msg = new ChatMessage();
        msg.setRole(ChatRole.USER);
        msg.setContent("追问");

        SseEmitter emitter = agent.execute("Q", List.of(), DeepDiveContextType.RECOMMENDED_ANSWER, "ctx", List.of(msg));

        assertNotNull(emitter);
        Thread.sleep(300);
        verify(obsidianService, never()).searchNotes(anyString());
        verify(aiGateway).streamText(any(SseEmitter.class), eq("system-prompt"), eq("built-prompt"), anyString(), anyString());
    }

    @Test
    void execute_searchNotes_searchesAndStreams() throws Exception {
        AgentDecision decision = new AgentDecision();
        decision.setAction("search_notes");
        decision.setKeyword("B+树");
        decision.setReason("需要补充知识");

        NoteItem note = new NoteItem("btree.md", "B+树索引详解", "BACKEND", List.of("索引"), "2026-01-01", "btree.md");

        when(interviewAiService.buildDeepDivePrompt(anyString(), any(), any(), anyString(), any()))
                .thenReturn("built-prompt");
        when(promptService.render(anyString(), anyMap())).thenReturn("decision-prompt");
        when(aiGateway.generateJson(anyString(), anyString(), eq(AgentDecision.class)))
                .thenReturn(new AiGateway.JsonResult<>(decision, "test-model"));
        when(obsidianService.isVaultConfigured()).thenReturn(true);
        when(obsidianService.searchNotes("B+树")).thenReturn(List.of(note));
        when(promptService.load("interview/system.md")).thenReturn("system-prompt");

        ChatMessage msg = new ChatMessage();
        msg.setRole(ChatRole.USER);
        msg.setContent("B+树怎么实现范围查询？");

        SseEmitter emitter = agent.execute("Q", List.of(), DeepDiveContextType.RECOMMENDED_ANSWER, "ctx", List.of(msg));

        assertNotNull(emitter);
        Thread.sleep(300);
        verify(obsidianService).searchNotes("B+树");
        verify(aiGateway).streamText(any(SseEmitter.class), eq("system-prompt"), contains("B+树索引详解"), anyString(), anyString());
    }
}
