package com.interviewassistant.dto.settings;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StyleProfileResponse {
    private String direction;
    private String level;
    private String focusAreas;
    private String scenarioPreference;
    private String keywordStyle;
}
