package com.interviewassistant.dto.settings;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PromptFileResponse {
    private String path;
    private String group;
    private String name;
    private String description;
    private long size;
    private String lastModified;
}
