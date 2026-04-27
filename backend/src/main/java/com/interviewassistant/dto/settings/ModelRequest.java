package com.interviewassistant.dto.settings;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ModelRequest {
    @NotBlank(message = "provider is required")
    @Size(max = 40, message = "provider must not exceed 40 characters")
    private String provider;

    @NotBlank(message = "model is required")
    @Size(max = 100, message = "model must not exceed 100 characters")
    private String model;
}
