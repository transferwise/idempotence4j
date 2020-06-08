package com.transferwise.idempotence4j.core.metrics;

import com.transferwise.idempotence4j.core.ActionId;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Duration;

@RequiredArgsConstructor
@Getter
public class Metrics {
    private final ActionId actionId;
    private Duration duration;
    private Outcome outcome;
    private boolean isRetry;

    public void record(Duration duration, Outcome outcome) {
        this.duration = duration;
        this.outcome = outcome;
    }

    public void recordRetry() {
        this.isRetry = true;
    }

    public enum Outcome {
        SUCCESS, ERROR, CONFLICT
    }
}
