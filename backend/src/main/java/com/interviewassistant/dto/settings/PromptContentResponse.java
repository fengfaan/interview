package com.interviewassistant.dto.settings;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PromptContentResponse {
    private String path;
    private String content;
}
