package com.interviewassistant.dto.interview;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class RecommendedAnswerRequest {
    @NotBlank(message = "direction is required")
    private String direction;

    @NotBlank(message = "level is required")
    private String level;

    @NotBlank(message = "question is required")
    private String question;

    private List<String> expectedKeywords;
}
