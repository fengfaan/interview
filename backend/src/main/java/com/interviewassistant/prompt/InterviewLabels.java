package com.interviewassistant.prompt;

public class InterviewLabels {

    private InterviewLabels() {
    }

    public static String questionTypeLabel(String level) {
        return switch (level) {
            case "BASIC" -> "基础八股";
            case "DEEP_PRINCIPLE" -> "深度原理";
            case "PROJECT_PRACTICE" -> "项目实战";
            default -> level;
        };
    }

    public static String directionLabel(String direction) {
        return switch (direction) {
            case "GO_BACKEND" -> "Go后端";
            case "REACT_FRONTEND" -> "React前端";
            case "SYSTEM_DESIGN" -> "系统设计";
            case "DATABASE_RELATED" -> "数据库相关";
            case "AI_CODING" -> "AI Agent 开发方向";
            default -> direction;
        };
    }
}
