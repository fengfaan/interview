package com.interviewassistant.prompt;

import com.interviewassistant.dto.interview.InterviewDirection;
import com.interviewassistant.dto.interview.InterviewLevel;

public class InterviewLabels {

    private InterviewLabels() {
    }

    public static String questionTypeLabel(InterviewLevel level) {
        return level.label();
    }

    public static String directionLabel(InterviewDirection direction) {
        return direction.label();
    }
}
