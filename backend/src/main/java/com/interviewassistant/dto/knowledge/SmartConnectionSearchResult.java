package com.interviewassistant.dto.knowledge;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SmartConnectionSearchResult {
    private String key;
    private String path;
    private String sourcePath;
    private String type;
    private double score;
    private String modelKey;
}
