package com.interviewassistant.dto.resume;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class AnalyzeResponse {
    private int score;
    private List<Dimension> dimensions;
    private List<Suggestion> suggestions;
}
