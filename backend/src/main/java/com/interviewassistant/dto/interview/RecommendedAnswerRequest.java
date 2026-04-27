package com.interviewassistant.dto.interview;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class RecommendedAnswerRequest {
    @NotNull(message = "direction is required")
    private InterviewDirection direction;

    @NotNull(message = "level is required")
    private InterviewLevel level;

    @NotBlank(message = "question is required")
    private String question;

    private List<String> expectedKeywords;
}
