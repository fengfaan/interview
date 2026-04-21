package com.interviewassistant.dto.interview;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class QuestionResponse {
    private String questionId;
    private String question;
    private List<String> expectedKeywords;
}
