package com.interviewassistant.dto.resume;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Finding {
    private String category;
    private String title;
    private String detail;
}
