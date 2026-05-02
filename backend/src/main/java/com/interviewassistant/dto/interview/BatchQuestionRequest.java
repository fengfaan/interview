package com.interviewassistant.dto.interview;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class BatchQuestionRequest {
    @NotNull(message = "方向不能为空")
    private InterviewDirection direction;

    @NotNull(message = "题型不能为空")
    private InterviewLevel level;

    @Min(value = 1, message = "至少生成 1 道题")
    @Max(value = 100, message = "最多生成 100 道题")
    private int count;

    private List<String> existingQuestions;
}
