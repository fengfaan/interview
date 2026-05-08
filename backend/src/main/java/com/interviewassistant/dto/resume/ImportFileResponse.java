package com.interviewassistant.dto.resume;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportFileResponse {
    private String text;
    private String fileName;
    private int pageCount;
    private String warning;
}
