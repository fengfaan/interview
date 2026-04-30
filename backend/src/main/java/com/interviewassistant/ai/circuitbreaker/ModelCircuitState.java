package com.interviewassistant.ai.circuitbreaker;

import lombok.Data;

@Data
public class ModelCircuitState {
    private CircuitState state = CircuitState.CLOSED;
    private int failureCount = 0;
    private long openedAt = 0;

    public void recordSuccess() {
        this.failureCount = 0;
        this.state = CircuitState.CLOSED;
    }

    public void recordFailure() {
        this.failureCount++;
    }

    public void open() {
        this.state = CircuitState.OPEN;
        this.openedAt = System.currentTimeMillis();
    }

    public void halfOpen() {
        this.state = CircuitState.HALF_OPEN;
    }
}
