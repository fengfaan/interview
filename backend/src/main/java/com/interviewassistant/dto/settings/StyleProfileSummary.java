package com.interviewassistant.dto.settings;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StyleProfileSummary {
    private String direction;
    private String level;
    private boolean hasCustomization;
}
