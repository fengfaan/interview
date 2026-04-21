package com.interviewassistant.dto.resume;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Suggestion {
    private String id;
    private String priority;
    private String title;
    private String reason;
    private String sourceText;
}
