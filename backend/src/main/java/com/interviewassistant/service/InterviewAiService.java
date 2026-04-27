package com.interviewassistant.service;

import com.interviewassistant.common.AiErrorUtils;
import com.interviewassistant.common.JsonOutputUtils;
import com.interviewassistant.dto.interview.*;
import com.interviewassistant.prompt.InterviewLabels;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewAiService {

    private final AiGateway aiGateway;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;
    private final AtomicInteger questionCounter = new AtomicInteger(1);
    private static final int BATCH_SIZE = 5;
    private static final int MAX_BATCH_ATTEMPTS = 3;
    private static final long INITIAL_BATCH_RETRY_DELAY_MS = 1_200L;

    public QuestionResponse generateQuestion(InterviewDirection direction, InterviewLevel level, List<HistoryEntry> history) {
        String historySummary = history == null || history.isEmpty() ? "无（首次提问）"
                : history.stream()
                .map(h -> "Q: " + h.getQuestion() + (h.isSkipped() ? " (已跳过)" : ""))
                .collect(Collectors.joining("\n"));

        String userMessage = promptService.render("interview/question.md", Map.of(
                "direction", InterviewLabels.directionLabel(direction),
                "questionType", InterviewLabels.questionTypeLabel(level),
                "history", historySummary
        ));

        AiGateway.JsonResult<QuestionAiResponse> result = aiGateway.generateJson(
                promptService.load("interview/system.md"), userMessage, QuestionAiResponse.class);
        QuestionAiResponse aiResult = result.value();
        if (aiResult.getQuestionId() == null) {
            aiResult.setQuestionId("q_" + String.format("%03d", questionCounter.getAndIncrement()));
        }
        return new QuestionResponse(
                aiResult.getQuestionId(),
                aiResult.getQuestion(),
                aiResult.getExpectedKeywords(),
                result.actualModel()
        );
    }

    public FeedbackResponse analyzeFeedback(InterviewDirection direction, InterviewLevel level, String question,
                                             String answer, List<String> expectedKeywords) {
        String userMessage = promptService.render("interview/feedback-json.md", Map.of(
                "direction", InterviewLabels.directionLabel(direction),
                "questionType", InterviewLabels.questionTypeLabel(level),
                "question", question,
                "answer", answer,
                "expectedKeywords", expectedKeywords != null ? expectedKeywords : List.of()
        ));

        return aiGateway.generateJson(
                promptService.load("interview/system.md"), userMessage, FeedbackResponse.class).value();
    }

    public String buildFeedbackStreamPrompt(InterviewDirection direction, InterviewLevel level, String question,
                                             String answer, List<String> expectedKeywords,
                                             String followUpQuestion) {
        return promptService.render("interview/feedback-stream.md", Map.of(
                "direction", InterviewLabels.directionLabel(direction),
                "questionType", InterviewLabels.questionTypeLabel(level),
                "question", question,
                "answer", answer,
                "expectedKeywords", expectedKeywords != null ? expectedKeywords : List.of(),
                "followUpQuestion", followUpQuestion != null ? followUpQuestion : "（请基于用户回答生成一个相关的进阶问题）"
        ));
    }

    public String buildRecommendedAnswerPrompt(InterviewDirection direction, InterviewLevel level, String question,
                                               List<String> expectedKeywords) {
        return promptService.render("interview/recommended-answer.md", Map.of(
                "direction", InterviewLabels.directionLabel(direction),
                "questionType", InterviewLabels.questionTypeLabel(level),
                "question", question,
                "expectedKeywords", expectedKeywords != null ? expectedKeywords : List.of()
        ));
    }

    public List<BatchQuestionItem> generateBatchQuestions(InterviewDirection direction, InterviewLevel level, int count) {
        if (count <= 0) {
            return Collections.emptyList();
        }

        List<BatchQuestionItem> allQuestions = new ArrayList<>();
        int batches = (count + BATCH_SIZE - 1) / BATCH_SIZE;

        for (int batch = 0; batch < batches; batch++) {
            int remaining = count - allQuestions.size();
            if (remaining <= 0) {
                break;
            }
            int batchCount = Math.min(remaining, BATCH_SIZE);

            String userMessage = promptService.render("interview/batch-question.md", Map.of(
                    "direction", InterviewLabels.directionLabel(direction),
                    "questionType", InterviewLabels.questionTypeLabel(level),
                    "count", String.valueOf(batchCount),
                    "batchNumber", String.valueOf(batch + 1),
                    "startIndex", String.valueOf(allQuestions.size() + 1)
            ));

            List<BatchQuestionItem> batchResult = generateBatchWithRetry(userMessage, batch + 1);
            if (batchResult.isEmpty()) {
                log.warn("Batch {} returned null, skipping", batch + 1);
                continue;
            }

            int startId = allQuestions.size() + 1;
            for (int i = 0; i < batchResult.size(); i++) {
                BatchQuestionItem item = batchResult.get(i);
                item.setQuestionId(String.format("batch_%03d", startId + i));
                allQuestions.add(item);
                if (allQuestions.size() >= count) {
                    break;
                }
            }
        }

        return allQuestions;
    }

    public List<BatchQuestionItem> generateBatchQuestionChunk(InterviewDirection direction, InterviewLevel level,
                                                              int count, int batchNumber,
                                                              int startIndex) {
        if (count <= 0) {
            return Collections.emptyList();
        }

        String userMessage = promptService.render("interview/batch-question.md", Map.of(
                "direction", InterviewLabels.directionLabel(direction),
                "questionType", InterviewLabels.questionTypeLabel(level),
                "count", String.valueOf(count),
                "batchNumber", String.valueOf(batchNumber),
                "startIndex", String.valueOf(startIndex)
        ));

        List<BatchQuestionItem> batchResult = generateBatchWithRetry(userMessage, batchNumber);
        if (batchResult.isEmpty()) {
            return Collections.emptyList();
        }

        List<BatchQuestionItem> questions = new ArrayList<>();
        for (int i = 0; i < batchResult.size() && questions.size() < count; i++) {
            BatchQuestionItem item = batchResult.get(i);
            item.setQuestionId(String.format("batch_%03d", startIndex + i));
            questions.add(item);
        }
        return questions;
    }

    public int batchSize() {
        return BATCH_SIZE;
    }

    private List<BatchQuestionItem> generateBatchWithRetry(String userMessage, int batchNumber) {
        RuntimeException lastError = null;
        for (int attempt = 1; attempt <= MAX_BATCH_ATTEMPTS; attempt++) {
            long startedAt = System.currentTimeMillis();
            try {
                log.info("Batch {} attempt {}/{} started", batchNumber, attempt, MAX_BATCH_ATTEMPTS);
                String response = aiGateway.generateText(promptService.load("interview/system.md"), userMessage);
                List<BatchQuestionItem> items = parseCompactBatch(response);
                log.info("Batch {} attempt {}/{} finished in {} ms, parsed {} question(s)",
                        batchNumber, attempt, MAX_BATCH_ATTEMPTS,
                        System.currentTimeMillis() - startedAt, items.size());
                return items;
            } catch (RuntimeException e) {
                lastError = e;
                if (AiErrorUtils.isRateLimit(e) || AiErrorUtils.isUnauthorized(e)) {
                    log.warn("Batch {} failed without retry: {}", batchNumber, AiErrorUtils.compactMessage(e));
                    throw e;
                }
                log.warn("Batch {} attempt {}/{} failed in {} ms: {}",
                        batchNumber, attempt, MAX_BATCH_ATTEMPTS,
                        System.currentTimeMillis() - startedAt, e.getMessage());
                if (attempt < MAX_BATCH_ATTEMPTS) {
                    sleepBeforeRetry(batchNumber, attempt, e);
                }
            }
        }
        throw lastError;
    }

    private void sleepBeforeRetry(int batchNumber, int attempt, RuntimeException error) {
        long delayMs = INITIAL_BATCH_RETRY_DELAY_MS * attempt;
        if (AiErrorUtils.isNetworkError(error)) {
            delayMs *= 2;
        }
        log.info("Batch {} will retry after {} ms", batchNumber, delayMs);
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("批量出题重试已中断", interrupted);
        }
    }

    private List<BatchQuestionItem> parseCompactBatch(String response) {
        try {
            String json = JsonOutputUtils.extractJson(response);
            JsonNode root = objectMapper.reader()
                    .with(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature())
                    .readTree(json);
            JsonNode items = root.path("items");
            if (items.isMissingNode()) {
                items = root.path("questions");
            }
            if (items.isMissingNode() && root.isArray()) {
                items = root;
            }
            if (!items.isArray()) {
                return Collections.emptyList();
            }

            List<BatchQuestionItem> result = new ArrayList<>();
            for (JsonNode item : items) {
                String question = textValue(item, "q", "question");
                String answer = textValue(item, "a", "answer", "reference_answer");
                if (question == null || answer == null) {
                    continue;
                }
                result.add(new BatchQuestionItem(null, question, answer, listValue(item, "k", "keywords")));
            }
            return result;
        } catch (Exception e) {
            throw new IllegalArgumentException("批量题紧凑 JSON 解析失败，响应预览: " + preview(response), e);
        }
    }

    private String textValue(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = node.path(fieldName);
            if (value.isTextual() && !value.asText().isBlank()) {
                return value.asText();
            }
        }
        return null;
    }

    private List<String> listValue(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = node.path(fieldName);
            if (!value.isArray()) {
                continue;
            }
            List<String> values = new ArrayList<>();
            for (JsonNode item : value) {
                if (item.isTextual() && !item.asText().isBlank()) {
                    values.add(item.asText());
                }
            }
            return values;
        }
        return Collections.emptyList();
    }

    private String preview(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text.replace("\r", "\\r").replace("\n", "\\n").replace("\t", "\\t");
        return normalized.length() > 500 ? normalized.substring(0, 500) + "..." : normalized;
    }
}
