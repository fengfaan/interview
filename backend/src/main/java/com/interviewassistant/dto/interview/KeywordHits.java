package com.interviewassistant.dto.interview;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class KeywordHits {
    private List<String> hit;
    private List<String> miss;
}
