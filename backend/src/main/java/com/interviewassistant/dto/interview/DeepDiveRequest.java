package com.interviewassistant.dto.interview;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class DeepDiveRequest {
    @NotBlank(message = "question is required")
    private String question;

    private List<String> expectedKeywords;

    @NotNull(message = "contextType is required")
    private DeepDiveContextType contextType;

    @NotBlank(message = "contextContent is required")
    private String contextContent;

    @NotEmpty(message = "messages must not be empty")
    private List<ChatMessage> messages;
}
