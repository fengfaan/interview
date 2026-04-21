package com.interviewassistant.dto.interview;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class FeedbackRequest {
    @NotBlank(message = "direction is required")
    private String direction;

    @NotBlank(message = "level is required")
    private String level;

    @NotBlank(message = "question is required")
    private String question;

    @NotBlank(message = "answer is required")
    @Size(max = 5000, message = "answer must not exceed 5000 characters")
    private String answer;

    private List<String> expectedKeywords;
}
