package com.interviewassistant.dto.import_;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ImportSaveResult {
    private String title;
    private boolean success;
    private String error;
}
