package com.interviewassistant.dto.import_;

import lombok.Data;
import java.util.List;

@Data
public class ConsolidateResult {
    private List<ConsolidatedCategory> categories;
    private int dedupCount;
    private int totalCount;
}
