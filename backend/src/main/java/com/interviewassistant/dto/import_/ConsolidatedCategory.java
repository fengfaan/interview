package com.interviewassistant.dto.import_;

import lombok.Data;
import java.util.List;

@Data
public class ConsolidatedCategory {
    private String name;
    private List<ParseResponse.ParsedQuestion> items;
}
