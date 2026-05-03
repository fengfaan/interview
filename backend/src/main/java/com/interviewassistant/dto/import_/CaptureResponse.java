package com.interviewassistant.dto.import_;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CaptureResponse {
    private String title;
    private String content;
    private String url;
    private String capturedAt;
}
