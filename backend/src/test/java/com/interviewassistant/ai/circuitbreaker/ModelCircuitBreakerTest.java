package com.interviewassistant.ai.circuitbreaker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModelCircuitBreakerTest {

    private static final int FAILURE_THRESHOLD = 3;
    private static final long OPEN_DURATION_MS = 30_000;
    private ModelCircuitBreaker breaker;

    @BeforeEach
    void setUp() {
        breaker = new ModelCircuitBreaker(FAILURE_THRESHOLD, OPEN_DURATION_MS);
    }

    @Test
    void initialStateIsClosed() {
        assertEquals(CircuitState.CLOSED, breaker.getState("model-a"));
        assertEquals(0, breaker.getFailureCount("model-a"));
    }

    @Test
    void failureCountIncrementsOnRecordFailure() {
        breaker.recordFailure("model-a");
        assertEquals(1, breaker.getFailureCount("model-a"));
        assertEquals(CircuitState.CLOSED, breaker.getState("model-a"));
    }

    @Test
    void opensAfterThresholdFailures() {
        for (int i = 0; i < FAILURE_THRESHOLD; i++) {
            breaker.recordFailure("model-a");
        }
        assertEquals(CircuitState.OPEN, breaker.getState("model-a"));
        assertTrue(breaker.getOpenedAt("model-a") > 0);
    }

    @Test
    void isOpenReturnsFalseWhenClosed() {
        assertFalse(breaker.isOpen("model-a"));
    }

    @Test
    void isOpenReturnsTrueWhenOpen() {
        for (int i = 0; i < FAILURE_THRESHOLD; i++) {
            breaker.recordFailure("model-a");
        }
        assertTrue(breaker.isOpen("model-a"));
    }

    @Test
    void transitionsToHalfOpenAfterCoolDown() {
        ModelCircuitBreaker fastBreaker = new ModelCircuitBreaker(FAILURE_THRESHOLD, 0);
        for (int i = 0; i < FAILURE_THRESHOLD; i++) {
            fastBreaker.recordFailure("model-a");
        }
        assertEquals(CircuitState.OPEN, fastBreaker.getState("model-a"));
        assertFalse(fastBreaker.isOpen("model-a"));
        assertEquals(CircuitState.HALF_OPEN, fastBreaker.getState("model-a"));
    }

    @Test
    void halfOpenGoesToClosedOnSuccess() {
        ModelCircuitBreaker fastBreaker = new ModelCircuitBreaker(FAILURE_THRESHOLD, 0);
        for (int i = 0; i < FAILURE_THRESHOLD; i++) {
            fastBreaker.recordFailure("model-a");
        }
        fastBreaker.isOpen("model-a");
        assertEquals(CircuitState.HALF_OPEN, fastBreaker.getState("model-a"));
        fastBreaker.recordSuccess("model-a");
        assertEquals(CircuitState.CLOSED, fastBreaker.getState("model-a"));
        assertEquals(0, fastBreaker.getFailureCount("model-a"));
    }

    @Test
    void halfOpenGoesBackToOpenOnFailure() {
        ModelCircuitBreaker fastBreaker = new ModelCircuitBreaker(FAILURE_THRESHOLD, 0);
        for (int i = 0; i < FAILURE_THRESHOLD; i++) {
            fastBreaker.recordFailure("model-a");
        }
        fastBreaker.isOpen("model-a");
        fastBreaker.recordFailure("model-a");
        assertEquals(CircuitState.OPEN, fastBreaker.getState("model-a"));
    }

    @Test
    void recordSuccessResetsClosedState() {
        breaker.recordFailure("model-a");
        breaker.recordFailure("model-a");
        breaker.recordSuccess("model-a");
        assertEquals(CircuitState.CLOSED, breaker.getState("model-a"));
        assertEquals(0, breaker.getFailureCount("model-a"));
    }

    @Test
    void differentModelsAreIndependent() {
        breaker.recordFailure("model-a");
        breaker.recordFailure("model-a");
        assertEquals(2, breaker.getFailureCount("model-a"));
        assertEquals(0, breaker.getFailureCount("model-b"));
        assertEquals(CircuitState.CLOSED, breaker.getState("model-b"));
    }

    @Test
    void resetClearsAllState() {
        breaker.recordFailure("model-a");
        breaker.recordFailure("model-b");
        breaker.reset("model-a");
        assertEquals(0, breaker.getFailureCount("model-a"));
        assertEquals(CircuitState.CLOSED, breaker.getState("model-a"));
        assertEquals(1, breaker.getFailureCount("model-b"));
    }

    @Test
    void isOpenStaysOpenWithinCooldown() {
        for (int i = 0; i < FAILURE_THRESHOLD; i++) {
            breaker.recordFailure("model-a");
        }
        assertTrue(breaker.isOpen("model-a"));
        assertEquals(CircuitState.OPEN, breaker.getState("model-a"));
    }
}
