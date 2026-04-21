package com.interviewassistant.dto.resume;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Dimension {
    private String name;
    private int score;
    private String reason;
}
