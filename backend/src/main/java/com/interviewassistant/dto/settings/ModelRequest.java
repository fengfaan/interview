package com.interviewassistant.dto.settings;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ModelRequest {
    @NotBlank(message = "model is required")
    @Size(max = 100, message = "model must not exceed 100 characters")
    private String model;
}
