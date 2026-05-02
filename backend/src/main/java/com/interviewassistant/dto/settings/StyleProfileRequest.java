package com.interviewassistant.dto.settings;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StyleProfileRequest {
    @Size(max = 2000, message = "出题侧重不能超过2000字")
    private String focusAreas;

    @Size(max = 2000, message = "场景偏好不能超过2000字")
    private String scenarioPreference;

    @Size(max = 1000, message = "关键词风格不能超过1000字")
    private String keywordStyle;
}
