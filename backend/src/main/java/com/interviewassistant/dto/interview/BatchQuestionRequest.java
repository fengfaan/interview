package com.interviewassistant.dto.interview;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BatchQuestionRequest {
    @NotBlank(message = "方向不能为空")
    private String direction;

    @NotBlank(message = "题型不能为空")
    private String level;

    @Min(value = 1, message = "至少生成 1 道题")
    @Max(value = 100, message = "最多生成 100 道题")
    private int count;
}
