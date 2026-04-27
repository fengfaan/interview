package com.interviewassistant.dto.interview;

public enum InterviewLevel {
    BASIC("基础八股"),
    DEEP_PRINCIPLE("深度原理"),
    PROJECT_PRACTICE("项目实战");

    private final String label;

    InterviewLevel(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
