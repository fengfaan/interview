package com.interviewassistant.dto.resume;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PolishStreamRequest {
    @NotBlank(message = "待改写内容不能为空")
    private String sourceText;
    
    private String jobDescription;
}
