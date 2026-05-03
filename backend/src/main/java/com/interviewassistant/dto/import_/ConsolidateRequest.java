package com.interviewassistant.dto.import_;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class ConsolidateRequest {
    @NotEmpty(message = "题目列表不能为空")
    private List<ParseResponse.ParsedQuestion> items;
    private String sourceUrl;
    private String title;
}
