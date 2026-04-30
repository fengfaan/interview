package com.interviewassistant.ai.exception;

public class AiResponseFormatException extends RuntimeException {

    private final String errorCode;
    private final String userMessage;

    public AiResponseFormatException(String errorCode, String userMessage) {
        super(userMessage);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
    }

    public AiResponseFormatException(String errorCode, String userMessage, Throwable cause) {
        super(userMessage, cause);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getUserMessage() {
        return userMessage;
    }
}
