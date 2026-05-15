package com.interviewassistant.dto.resume;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FunnelScore {
    private Integer score;
    private String detail;
    private boolean skipped;
}
