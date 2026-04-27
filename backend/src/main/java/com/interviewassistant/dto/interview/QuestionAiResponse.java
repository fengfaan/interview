package com.interviewassistant.dto.interview;

import lombok.Data;

import java.util.List;

@Data
public class QuestionAiResponse {
    private String questionId;
    private String question;
    private List<String> expectedKeywords;
}
