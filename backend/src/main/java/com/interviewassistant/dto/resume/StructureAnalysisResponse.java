package com.interviewassistant.dto.resume;

import lombok.Data;
import java.util.List;

@Data
public class StructureAnalysisResponse {
    private int structureScore;
    private List<ModuleCheck> moduleChecks;
    private List<StructuralIssue> issues;
    private String summary;

    @Data
    public static class ModuleCheck {
        private String name;
        private String status; // pass, warn, fail
        private String detail;
    }

    @Data
    public static class StructuralIssue {
        private String severity; // critical, warning, info
        private String description;
        private String suggestion;
    }
}
