package com.interviewassistant.ai.exception;

public class PromptLoadException extends RuntimeException {
    public PromptLoadException(String message) {
        super(message);
    }

    public PromptLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
