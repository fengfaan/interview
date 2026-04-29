package com.interviewassistant.agent;

import com.interviewassistant.common.SseUtils;
import com.interviewassistant.dto.interview.ChatMessage;
import com.interviewassistant.dto.interview.DeepDiveContextType;
import com.interviewassistant.dto.knowledge.NoteItem;
import com.interviewassistant.service.AiGateway;
import com.interviewassistant.service.InterviewAiService;
import com.interviewassistant.service.ObsidianService;
import com.interviewassistant.service.PromptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DeepDiveAgent {

    private final AiGateway aiGateway;
    private final PromptService promptService;
    private final ObsidianService obsidianService;
    private final Executor executor;
    private final InterviewAiService interviewAiService;

    public DeepDiveAgent(AiGateway aiGateway,
                         PromptService promptService,
                         ObsidianService obsidianService,
                         @Qualifier("sseTaskExecutor") Executor executor,
                         InterviewAiService interviewAiService) {
        this.aiGateway = aiGateway;
        this.promptService = promptService;
        this.obsidianService = obsidianService;
        this.executor = executor;
        this.interviewAiService = interviewAiService;
    }

    public SseEmitter execute(String question, List<String> expectedKeywords,
                               DeepDiveContextType contextType, String contextContent,
                               List<ChatMessage> messages) {
        SseEmitter emitter = SseUtils.createShortEmitter();
        executor.execute(() -> {
            try {
                String prompt = interviewAiService.buildDeepDivePrompt(
                        question, expectedKeywords, contextType, contextContent, messages);

                String contextPreview = contextContent != null && contextContent.length() > 200
                        ? contextContent.substring(0, 200)
                        : (contextContent != null ? contextContent : "");
                String latestQuestion = messages != null && !messages.isEmpty()
                        ? messages.get(messages.size() - 1).getContent()
                        : question;

                AgentDecision decision = decide(question, expectedKeywords, latestQuestion, contextPreview);

                String finalPrompt = prompt;
                if (decision.wantsSearch()) {
                    List<NoteItem> notes = searchKnowledge(decision.getKeyword());
                    if (!notes.isEmpty()) {
                        String searchContext = buildSearchContext(notes);
                        SseUtils.sendAgentStep(emitter, decision.getKeyword(),
                                notes.stream().map(NoteItem::getTitle).collect(Collectors.toList()));

                        finalPrompt = prompt + "\n\n【知识库检索结果（关键词：" + decision.getKeyword() + "）】\n"
                                + searchContext
                                + "\n请在回答中适当引用知识库中的相关内容，帮助候选人更深入理解。";
                    }
                    log.info("Agent decision: search_notes keyword={} notes={}", decision.getKeyword(), notes.size());
                } else {
                    log.info("Agent decision: answer_directly reason={}", decision.getReason());
                }

                String systemPrompt = promptService.load("interview/system.md");
                aiGateway.streamText(emitter, systemPrompt, finalPrompt,
                        "深度追问生成失败", "启动深度追问失败");
            } catch (Exception e) {
                log.error("DeepDiveAgent execution failed", e);
                SseUtils.sendError(emitter, "AGENT_ERROR", "深度追问智能检索失败，请重试");
            }
        });
        return emitter;
    }

    AgentDecision decide(String question, List<String> expectedKeywords,
                         String latestQuestion, String contextPreview) {
        try {
            String userPrompt = promptService.render("interview/deep-dive-agent-decide.md", Map.of(
                    "question", question,
                    "expectedKeywords", expectedKeywords != null ? expectedKeywords : List.of(),
                    "latestQuestion", latestQuestion != null ? latestQuestion : "",
                    "contextPreview", contextPreview != null ? contextPreview : ""
            ));
            String systemPrompt = "你是一个决策助手。";

            AiGateway.JsonResult<AgentDecision> result =
                    aiGateway.generateJson(systemPrompt, userPrompt, AgentDecision.class);
            return result.value();
        } catch (Exception e) {
            log.warn("Agent decision failed, falling back to answer_directly: {}", e.getMessage());
            AgentDecision fallback = new AgentDecision();
            fallback.setAction("answer_directly");
            fallback.setReason("决策失败，直接回答");
            return fallback;
        }
    }

    List<NoteItem> searchKnowledge(String keyword) {
        try {
            if (!obsidianService.isVaultConfigured()) {
                return List.of();
            }
            return obsidianService.searchNotes(keyword);
        } catch (Exception e) {
            log.warn("Knowledge search failed for keyword '{}': {}", keyword, e.getMessage());
            return List.of();
        }
    }

    String buildSearchContext(List<NoteItem> notes) {
        if (notes == null || notes.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (NoteItem note : notes) {
            sb.append("- ").append(note.getTitle());
            if (note.getTags() != null && !note.getTags().isEmpty()) {
                sb.append(" [").append(String.join(", ", note.getTags())).append("]");
            }
            sb.append(" (").append(note.getId()).append(")\n");
        }
        return sb.toString();
    }
}
