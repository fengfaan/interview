package com.interviewassistant.dto.settings;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ApiKeyRequest {
    @NotBlank(message = "provider is required")
    private String provider;

    @NotBlank(message = "apiKey is required")
    private String apiKey;
}
