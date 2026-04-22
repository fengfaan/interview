package com.interviewassistant.common;

public class PromptLoadException extends RuntimeException {
    public PromptLoadException(String message) {
        super(message);
    }

    public PromptLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
