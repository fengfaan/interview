package com.interviewassistant.dto.interview;

import lombok.Data;

import java.util.List;

@Data
public class BatchCompactAiResponse {
    private List<BatchCompactQuestionItem> items;
}
