package com.interviewassistant.dto.resume;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckupAnnotation {
    private String quote;
    private String location;
    private String category;
    private String problem;
    private String suggestion;
    private String rewrite;
}
