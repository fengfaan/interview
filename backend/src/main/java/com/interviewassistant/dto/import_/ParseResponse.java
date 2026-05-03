package com.interviewassistant.dto.import_;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ParseResponse {
    private List<ParsedQuestion> items;

    @Data
    @AllArgsConstructor
    public static class ParsedQuestion {
        private String question;
        private String answer;
        private List<String> keywords;
    }
}
