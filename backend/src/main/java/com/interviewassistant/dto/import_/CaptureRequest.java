package com.interviewassistant.dto.import_;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CaptureRequest {
    @NotBlank(message = "URL 不能为空")
    @Pattern(regexp = "^https?://.*", message = "URL 必须以 http:// 或 https:// 开头")
    private String url;
}
