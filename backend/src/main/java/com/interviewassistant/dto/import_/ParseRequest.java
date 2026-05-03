package com.interviewassistant.dto.import_;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ParseRequest {
    @NotBlank(message = "内容不能为空")
    private String content;

    private String direction;
    private String level;
}
