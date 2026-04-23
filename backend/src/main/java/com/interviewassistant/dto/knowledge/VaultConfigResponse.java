package com.interviewassistant.dto.knowledge;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VaultConfigResponse {
    private boolean configured;
    private String path;
}
