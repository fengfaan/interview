package com.interviewassistant.dto.import_;

import lombok.Data;

@Data
public class ConsolidatedSaveResult {
    private String filePath;
    private int questionCount;
    private boolean success;
    private String error;

    public ConsolidatedSaveResult(String filePath, int questionCount) {
        this.filePath = filePath;
        this.questionCount = questionCount;
        this.success = true;
    }

    public ConsolidatedSaveResult(String error) {
        this.success = false;
        this.error = error;
    }
}
