package com.transferwise.idempotence4j.benchmarks.domain.model;

import lombok.NonNull;
import lombok.Value;

import java.time.Instant;

@Value
public class Operation {
    private OperationId operationId;
    private Instant createdAt;

    public Operation(@NonNull OperationId operationId) {
        this.operationId = operationId;
        this.createdAt = Instant.now();
    }
}
