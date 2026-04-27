package com.interviewassistant.dto.interview;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class QuestionRequest {
    @NotNull(message = "direction is required")
    private InterviewDirection direction;

    @NotNull(message = "level is required")
    private InterviewLevel level;

    private List<HistoryEntry> history = new ArrayList<>();
}
