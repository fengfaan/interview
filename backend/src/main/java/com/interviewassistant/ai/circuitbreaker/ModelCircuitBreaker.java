package com.interviewassistant.ai.circuitbreaker;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ModelCircuitBreaker {

    private static final int DEFAULT_FAILURE_THRESHOLD = 3;
    private static final long DEFAULT_OPEN_DURATION_MS = 30_000;

    private final int failureThreshold;
    private final long openDurationMs;
    private final ConcurrentHashMap<String, ModelCircuitState> states = new ConcurrentHashMap<>();

    public ModelCircuitBreaker() {
        this(DEFAULT_FAILURE_THRESHOLD, DEFAULT_OPEN_DURATION_MS);
    }

    public ModelCircuitBreaker(int failureThreshold, long openDurationMs) {
        this.failureThreshold = failureThreshold;
        this.openDurationMs = openDurationMs;
    }

    private ModelCircuitState getOrCreate(String model) {
        return states.computeIfAbsent(model, k -> new ModelCircuitState());
    }

    public CircuitState getState(String model) {
        return getOrCreate(model).getState();
    }

    public int getFailureCount(String model) {
        return getOrCreate(model).getFailureCount();
    }

    public long getOpenedAt(String model) {
        return getOrCreate(model).getOpenedAt();
    }

    public boolean isOpen(String model) {
        ModelCircuitState state = getOrCreate(model);
        if (state.getState() == CircuitState.CLOSED) {
            return false;
        }
        if (state.getState() == CircuitState.OPEN) {
            long elapsed = System.currentTimeMillis() - state.getOpenedAt();
            if (elapsed >= openDurationMs) {
                state.halfOpen();
                log.info("Model [{}] circuit transitioned to HALF_OPEN after {}ms cooldown", model, elapsed);
                return false;
            }
            return true;
        }
        return false;
    }

    public void recordSuccess(String model) {
        ModelCircuitState state = getOrCreate(model);
        CircuitState previous = state.getState();
        state.recordSuccess();
        if (previous != CircuitState.CLOSED) {
            log.info("Model [{}] circuit CLOSED after successful call (was {})", model, previous);
        }
    }

    public void recordFailure(String model) {
        ModelCircuitState state = getOrCreate(model);
        if (state.getState() == CircuitState.HALF_OPEN) {
            state.open();
            log.warn("Model [{}] HALF_OPEN probe failed, back to OPEN", model);
            return;
        }
        state.recordFailure();
        if (state.getFailureCount() >= failureThreshold && state.getState() != CircuitState.OPEN) {
            state.open();
            log.warn("Model [{}] circuit OPEN after {} consecutive failures", model, failureThreshold);
        }
    }

    public void reset(String model) {
        states.remove(model);
    }

    public List<ModelHealthEntry> getHealthForAll(List<String> models) {
        return models.stream().map(m -> {
            ModelCircuitState s = states.get(m);
            if (s == null) {
                return new ModelHealthEntry(m, CircuitState.CLOSED, 0, null);
            }
            String openedAtIso = s.getOpenedAt() > 0
                    ? Instant.ofEpochMilli(s.getOpenedAt()).toString()
                    : null;
            return new ModelHealthEntry(m, s.getState(), s.getFailureCount(), openedAtIso);
        }).collect(Collectors.toList());
    }

    public record ModelHealthEntry(String model, CircuitState state, int failureCount, String openedAt) {
    }
}
