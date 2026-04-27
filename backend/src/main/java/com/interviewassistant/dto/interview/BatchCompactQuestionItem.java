package com.interviewassistant.dto.interview;

import lombok.Data;

import java.util.List;

@Data
public class BatchCompactQuestionItem {
    private String q;
    private String a;
    private List<String> k;
}
