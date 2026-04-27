package com.interviewassistant.common;

public class JsonOutputUtils {

    private JsonOutputUtils() {
    }

    public static String extractJson(String text) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline >= 0 && lastFence > firstNewline) {
                trimmed = trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        int objectStart = trimmed.indexOf('{');
        int arrayStart = trimmed.indexOf('[');
        int start;
        if (objectStart < 0) {
            start = arrayStart;
        } else if (arrayStart < 0) {
            start = objectStart;
        } else {
            start = Math.min(objectStart, arrayStart);
        }
        if (start > 0) {
            trimmed = trimmed.substring(start).trim();
        }
        int objectEnd = trimmed.lastIndexOf('}');
        int arrayEnd = trimmed.lastIndexOf(']');
        int end = Math.max(objectEnd, arrayEnd);
        if (end >= 0 && end < trimmed.length() - 1) {
            trimmed = trimmed.substring(0, end + 1).trim();
        }
        return trimmed;
    }
}
