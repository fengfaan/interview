package com.interviewassistant.ai.style;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StyleProfile {
    private String focusAreas;
    private String scenarioPreference;
    private String keywordStyle;
}
