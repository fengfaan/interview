package com.interviewassistant.dto.interview;

public enum InterviewDirection {
    GO_BACKEND("Go后端"),
    REACT_FRONTEND("React前端"),
    SYSTEM_DESIGN("系统设计"),
    DATABASE_RELATED("数据库相关"),
    AI_CODING("AI Agent 开发方向");

    private final String label;

    InterviewDirection(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
