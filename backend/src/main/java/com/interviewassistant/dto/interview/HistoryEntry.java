package com.interviewassistant.dto.interview;

import lombok.Data;

@Data
public class HistoryEntry {
    private String questionId;
    private String question;
    private String answer;
    private boolean skipped;
}
