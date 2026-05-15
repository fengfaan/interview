package com.interviewassistant.dto.resume;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class HealthCheckupRequest {
    @NotBlank(message = "简历内容不能为空")
    @Size(min = 50, message = "简历内容过短，请提供完整的简历")
    @Size(max = 20000, message = "简历内容不能超过20000字符")
    private String resume;

    @Size(max = 20000, message = "职位描述不能超过20000字符")
    private String jobDescription;
}
