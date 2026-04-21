package com.interviewassistant.dto.interview;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FeedbackResponse {
    private KeywordHits keywordHits;
    private int score;
}
