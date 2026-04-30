package com.interviewassistant.ai.agent;

import com.interviewassistant.common.SseUtils;
import com.interviewassistant.dto.interview.ChatMessage;
import com.interviewassistant.dto.interview.DeepDiveContextType;
import com.interviewassistant.ai.gateway.AiGateway;
import com.interviewassistant.service.InterviewAiService;
import com.interviewassistant.service.ObsidianService;
import com.interviewassistant.ai.prompt.PromptService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

@Slf4j
@Service
public class DeepDiveAgent {

    private static final int MAX_TOOL_ROUNDS = 3;

    private final AiGateway aiGateway;
    private final PromptService promptService;
    private final Executor executor;
    private final InterviewAiService interviewAiService;
    private final ToolCallback[] toolCallbacks;
    private final ObjectMapper objectMapper;

    public DeepDiveAgent(AiGateway aiGateway,
                         PromptService promptService,
                         @Qualifier("sseTaskExecutor") Executor executor,
                         InterviewAiService interviewAiService,
                         KnowledgeTools knowledgeTools,
                         ObjectMapper objectMapper) {
        this.aiGateway = aiGateway;
        this.promptService = promptService;
        this.executor = executor;
        this.interviewAiService = interviewAiService;
        this.objectMapper = objectMapper;
        this.toolCallbacks = MethodToolCallbackProvider.builder()
                .toolObjects(knowledgeTools)
                .build()
                .getToolCallbacks();
    }

    public SseEmitter execute(String question, List<String> expectedKeywords,
                               DeepDiveContextType contextType, String contextContent,
                               List<ChatMessage> messages) {
        SseEmitter emitter = SseUtils.createShortEmitter();
        executor.execute(() -> {
            try {
                String userPrompt = interviewAiService.buildDeepDivePrompt(
                        question, expectedKeywords, contextType, contextContent, messages);

                String systemPrompt = promptService.load("interview/deep-dive-agent-system.md");

                List<org.springframework.ai.chat.messages.Message> chatMessages = new ArrayList<>();
                chatMessages.add(new SystemMessage(systemPrompt));
                chatMessages.add(new UserMessage(userPrompt));

                String finalPrompt = runReActLoop(emitter, chatMessages);

                aiGateway.streamText(emitter, systemPrompt, finalPrompt,
                        "深度追问生成失败", "启动深度追问失败");
            } catch (Exception e) {
                log.error("DeepDiveAgent execution failed", e);
                SseUtils.sendError(emitter, "AGENT_ERROR", "深度追问智能检索失败，请重试");
            }
        });
        return emitter;
    }

    String runReActLoop(SseEmitter emitter,
                        List<org.springframework.ai.chat.messages.Message> chatMessages) {
        StringBuilder toolResults = new StringBuilder();
        for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {
            ChatResponse response = aiGateway.callWithTools(chatMessages, toolCallbacks);

            if (response == null || response.getResult() == null
                    || response.getResult().getOutput() == null) {
                log.warn("Empty response from LLM at round {}", round);
                break;
            }

            AssistantMessage assistant = response.getResult().getOutput();
            if (!assistant.hasToolCalls()) {
                break;
            }

            chatMessages.add(assistant);

            List<ToolResponseMessage.ToolResponse> toolResponses = new ArrayList<>();
            for (AssistantMessage.ToolCall toolCall : assistant.getToolCalls()) {
                log.info("Agent tool call round={}: name={} args={}", round, toolCall.name(), toolCall.arguments());

                String result = executeToolCall(toolCall);
                toolResponses.add(new ToolResponseMessage.ToolResponse(
                        toolCall.id(), toolCall.name(), result));

                if ("searchNotes".equals(toolCall.name())) {
                    sendAgentStepFromResult(emitter, toolCall.arguments(), result);
                }

                toolResults.append("\n\n【知识库检索结果（工具调用 ").append(round + 1).append("）】\n")
                        .append(result);
            }

            ToolResponseMessage toolResponseMessage = ToolResponseMessage.builder()
                    .responses(toolResponses)
                    .build();
            chatMessages.add(toolResponseMessage);
        }

        String originalUserPrompt = ((UserMessage) chatMessages.get(1)).getText();
        if (toolResults.isEmpty()) {
            return originalUserPrompt;
        }
        return originalUserPrompt + toolResults;
    }

    private String executeToolCall(AssistantMessage.ToolCall toolCall) {
        for (ToolCallback callback : toolCallbacks) {
            if (callback.getToolDefinition().name().equals(toolCall.name())) {
                try {
                    return callback.call(toolCall.arguments());
                } catch (Exception e) {
                    log.warn("Tool execution failed for {}: {}", toolCall.name(), e.getMessage());
                    return "工具执行失败：" + e.getMessage() + "。请直接基于现有信息回答。";
                }
            }
        }
        return "未知工具：" + toolCall.name();
    }

    private void sendAgentStepFromResult(SseEmitter emitter, String arguments, String result) {
        try {
            String keyword = "";
            JsonNode args = objectMapper.readTree(arguments);
            if (args.has("keyword")) {
                keyword = args.get("keyword").asText();
            }
            List<String> noteTitles = extractNoteTitles(result);
            if (!keyword.isEmpty() && !noteTitles.isEmpty()) {
                SseUtils.sendAgentStep(emitter, keyword, noteTitles);
            }
        } catch (Exception e) {
            log.debug("Failed to parse tool call arguments for agent_step: {}", e.getMessage());
        }
    }

    List<String> extractNoteTitles(String searchResult) {
        List<String> titles = new ArrayList<>();
        for (String line : searchResult.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("- ")) {
                String rest = trimmed.substring(2);
                int bracketIdx = rest.indexOf('[');
                int parenIdx = rest.indexOf('(');
                int endIdx = rest.length();
                if (bracketIdx > 0) endIdx = Math.min(endIdx, bracketIdx);
                if (parenIdx > 0) endIdx = Math.min(endIdx, parenIdx);
                titles.add(rest.substring(0, endIdx).trim());
            }
        }
        return titles;
    }
}
