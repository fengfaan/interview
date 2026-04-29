package com.interviewassistant.agent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AgentDecision {
    private String action;
    private String keyword;
    private String reason;

    public boolean wantsSearch() {
        return "search_notes".equals(action) && keyword != null && !keyword.isBlank();
    }
}
