package com.interviewassistant.dto.settings;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ModelResponse {
    private String model;
    private String defaultModel;
    private List<String> options;
}
