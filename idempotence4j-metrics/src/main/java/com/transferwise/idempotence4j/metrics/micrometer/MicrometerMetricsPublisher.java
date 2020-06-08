package com.transferwise.idempotence4j.metrics.micrometer;

import com.transferwise.idempotence4j.core.metrics.Metrics;
import com.transferwise.idempotence4j.core.metrics.MetricsPublisher;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MicrometerMetricsPublisher implements MetricsPublisher {
    private final MeterRegistry meterRegistry;

    @Override
    public void publish(Metrics metrics) {
        meterRegistry.counter("idempotence4j.executions",
            "type", metrics.getActionId().getType(),
            "client", metrics.getActionId().getClient(),
            "outcome", metrics.getOutcome().name()).increment();

        if (metrics.isRetry()) {
            meterRegistry.counter("idempotence4j.executions.retries",
                "type", metrics.getActionId().getType(),
                "client", metrics.getActionId().getClient()).increment();
        }

        meterRegistry.timer("idempotence4j.execution.latency",
            "type", metrics.getActionId().getType(),
            "client", metrics.getActionId().getClient(),
            "outcome", metrics.getOutcome().name()).record(metrics.getDuration());
    }
}
