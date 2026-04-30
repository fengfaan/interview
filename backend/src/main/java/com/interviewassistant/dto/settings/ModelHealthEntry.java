package com.interviewassistant.dto.settings;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ModelHealthEntry {
    private String model;
    private String state;
    private int failureCount;
    private String openedAt;
}
