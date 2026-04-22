package com.interviewassistant.dto.settings;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PromptImproveRequest {
    @NotBlank(message = "path is required")
    private String path;

    @NotBlank(message = "content is required")
    @Size(max = 40000, message = "content must not exceed 40000 characters")
    private String content;

    @Size(max = 1000, message = "instruction must not exceed 1000 characters")
    private String instruction;
}
