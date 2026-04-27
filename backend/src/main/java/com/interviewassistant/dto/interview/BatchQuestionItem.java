package com.interviewassistant.dto.interview;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchQuestionItem {
    private String questionId;
    private String question;
    private String answer;
    private List<String> keywords;
}
