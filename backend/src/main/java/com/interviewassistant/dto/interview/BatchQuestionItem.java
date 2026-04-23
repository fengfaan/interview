package com.interviewassistant.dto.interview;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class BatchQuestionItem {
    private String questionId;
    private String question;
    private String answer;
    private List<String> keywords;
}
