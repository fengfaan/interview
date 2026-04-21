package com.interviewassistant.dto.settings;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApiKeyResponse {
    private String masked;
    private boolean configured;
}
