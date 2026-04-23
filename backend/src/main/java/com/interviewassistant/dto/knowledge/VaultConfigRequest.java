package com.interviewassistant.dto.knowledge;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VaultConfigRequest {
    @NotBlank(message = "Vault 路径不能为空")
    private String path;
}
