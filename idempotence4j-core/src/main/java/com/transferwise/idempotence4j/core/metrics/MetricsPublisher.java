package com.transferwise.idempotence4j.core.metrics;

public interface MetricsPublisher {
    void publish(Metrics metrics);
}
