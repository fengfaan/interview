package com.interviewassistant.dto.resume;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StructureAnalysisRequest {
    @NotBlank(message = "简历内容不能为空")
    @Size(min = 50, message = "简历内容过短，请提供完整的简历")
    private String resume;
}
