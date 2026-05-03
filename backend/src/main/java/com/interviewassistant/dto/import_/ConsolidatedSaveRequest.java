package com.interviewassistant.dto.import_;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class ConsolidatedSaveRequest {
    @NotEmpty(message = "分类列表不能为空")
    private List<ConsolidatedCategory> categories;
    private String sourceUrl;
    private String title;
}
