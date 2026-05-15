package com.interviewassistant.dto.resume;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HealthCheckupResponse {
    private int overallScore;
    private Map<String, FunnelScore> funnelScores;
    private List<Finding> redFlags;
    private List<Finding> warnings;
    private List<Finding> highlights;
    private List<CheckupAnnotation> annotations;
    private String summary;
}
