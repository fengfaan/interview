package com.interviewassistant.service;

import com.interviewassistant.dto.interview.*;
import com.interviewassistant.prompt.InterviewPrompts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewAiService {

    private final ChatClient chatClient;
    private final AtomicInteger questionCounter = new AtomicInteger(1);

    public QuestionResponse generateQuestion(String direction, String level, List<HistoryEntry> history) {
        String historySummary = history == null || history.isEmpty() ? "无（首次提问）"
                : history.stream()
                .map(h -> "Q: " + h.getQuestion() + (h.isSkipped() ? " (已跳过)" : ""))
                .collect(Collectors.joining("\n"));

        String userMessage = String.format(InterviewPrompts.QUESTION_PROMPT,
                InterviewPrompts.directionLabel(direction),
                InterviewPrompts.difficultyLabel(level),
                historySummary);

        var converter = new BeanOutputConverter<>(QuestionResponse.class);
        String response = chatClient.prompt()
                .system(InterviewPrompts.SYSTEM_PROMPT)
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
        String userMessage = String.format(InterviewPrompts.FEEDBACK_PROMPT,
                InterviewPrompts.directionLabel(direction),
                InterviewPrompts.difficultyLabel(level),
                question, answer, expectedKeywords);

        var converter = new BeanOutputConverter<>(FeedbackResponse.class);
        String response = chatClient.prompt()
                .system(InterviewPrompts.SYSTEM_PROMPT)
                .user(userMessage + "\n\n" + converter.getFormat())
                .call()
                .content();

        return converter.convert(response);
    }

    public String buildFeedbackStreamPrompt(String direction, String level, String question,
                                             String answer, List<String> expectedKeywords,
                                             String followUpQuestion) {
        return String.format(InterviewPrompts.FEEDBACK_STREAM_PROMPT,
                InterviewPrompts.directionLabel(direction),
                InterviewPrompts.difficultyLabel(level),
                question, answer, expectedKeywords,
                followUpQuestion != null ? followUpQuestion : "（请基于用户回答生成一个相关的进阶问题）");
    }
}
