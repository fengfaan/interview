package com.interviewassistant.dto.interview;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChatMessage {
    @NotNull(message = "role is required")
    private ChatRole role;

    @NotBlank(message = "content is required")
    private String content;
}
