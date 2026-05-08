package com.interviewassistant.dto.import_;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParseResponse {
    private List<ParsedQuestion> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParsedQuestion {
        private String question;
        private String answer;
        private List<String> keywords;
    }
}
