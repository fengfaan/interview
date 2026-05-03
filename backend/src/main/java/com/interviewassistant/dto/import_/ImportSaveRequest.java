package com.interviewassistant.dto.import_;

import com.interviewassistant.dto.import_.ParseResponse.ParsedQuestion;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class ImportSaveRequest {
    @NotEmpty(message = "题目列表不能为空")
    private List<ParsedQuestion> items;
    private String direction;
    private String level;
    private String sourceUrl;
}
