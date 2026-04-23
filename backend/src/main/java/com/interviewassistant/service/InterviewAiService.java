package com.interviewassistant.service;

import com.interviewassistant.config.AiConfig;
import com.interviewassistant.dto.interview.*;
import com.interviewassistant.prompt.InterviewLabels;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewAiService {

    private final AiConfig aiConfig;
    private final PromptService promptService;
    private final AtomicInteger questionCounter = new AtomicInteger(1);

    public QuestionResponse generateQuestion(String direction, String level, List<HistoryEntry> history) {
        String historySummary = history == null || history.isEmpty() ? "无（首次提问）"
                : history.stream()
                .map(h -> "Q: " + h.getQuestion() + (h.isSkipped() ? " (已跳过)" : ""))
                .collect(Collectors.joining("\n"));

        String userMessage = promptService.render("interview/question.md", Map.of(
                "direction", InterviewLabels.directionLabel(direction),
                "questionType", InterviewLabels.questionTypeLabel(level),
                "history", historySummary
        ));

        var converter = new BeanOutputConverter<>(QuestionResponse.class);
        String response = aiConfig.getCurrentChatClient().prompt()
                .system(promptService.load("interview/system.md"))
                .user(userMessage + "\n\n" + converter.getFormat())
                .call()
                .content();

        QuestionResponse result = converter.convert(response);
        if (result.getQuestionId() == null) {
            result.setQuestionId("q_" + String.format("%03d", questionCounter.getAndIncrement()));
        }
        return result;
    }

    public FeedbackResponse analyzeFeedback(String direction, String level, String question,
                                             String answer, List<String> expectedKeywords) {
        String userMessage = promptService.render("interview/feedback-json.md", Map.of(
                "direction", InterviewLabels.directionLabel(direction),
                "questionType", InterviewLabels.questionTypeLabel(level),
                "question", question,
                "answer", answer,
                "expectedKeywords", expectedKeywords != null ? expectedKeywords : List.of()
        ));

        var converter = new BeanOutputConverter<>(FeedbackResponse.class);
        String response = aiConfig.getCurrentChatClient().prompt()
                .system(promptService.load("interview/system.md"))
                .user(userMessage + "\n\n" + converter.getFormat())
                .call()
                .content();

        return converter.convert(response);
    }

    public String buildFeedbackStreamPrompt(String direction, String level, String question,
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

    public String buildRecommendedAnswerPrompt(String direction, String level, String question,
                                               List<String> expectedKeywords) {
        return promptService.render("interview/recommended-answer.md", Map.of(
                "direction", InterviewLabels.directionLabel(direction),
                "questionType", InterviewLabels.questionTypeLabel(level),
                "question", question,
                "expectedKeywords", expectedKeywords != null ? expectedKeywords : List.of()
        ));
    }

    public List<BatchQuestionItem> generateBatchQuestions(String direction, String level, int count) {
        if (count <= 0) {
            return Collections.emptyList();
        }

        List<BatchQuestionItem> allQuestions = new ArrayList<>();
        int batchSize = 10;
        int batches = (count + batchSize - 1) / batchSize;

        for (int batch = 0; batch < batches; batch++) {
            int remaining = count - allQuestions.size();
            int batchCount = Math.min(remaining, batchSize);

            String existingQuestions = allQuestions.stream()
                    .map(q -> "- " + q.getQuestion())
                    .collect(Collectors.joining("\n"));

            String userMessage = promptService.render("interview/batch-question.md", Map.of(
                    "direction", InterviewLabels.directionLabel(direction),
                    "questionType", InterviewLabels.questionTypeLabel(level),
                    "count", String.valueOf(batchCount),
                    "existingQuestions", existingQuestions.isEmpty() ? "无" : existingQuestions
            ));

            var converter = new BeanOutputConverter<>(BatchAiResponse.class);
            String response = aiConfig.getCurrentChatClient().prompt()
                    .system(promptService.load("interview/system.md"))
                    .user(userMessage + "\n\n" + converter.getFormat())
                    .call()
                    .content();

            BatchAiResponse batchResult = converter.convert(response);
            if (batchResult == null || batchResult.getQuestions() == null) {
                log.warn("Batch {} returned null, skipping", batch + 1);
                continue;
            }

            int startId = allQuestions.size() + 1;
            for (int i = 0; i < batchResult.getQuestions().size(); i++) {
                BatchQuestionItem item = batchResult.getQuestions().get(i);
                item.setQuestionId(String.format("batch_%03d", startId + i));
                allQuestions.add(item);
            }
        }

        return allQuestions;
    }
}
