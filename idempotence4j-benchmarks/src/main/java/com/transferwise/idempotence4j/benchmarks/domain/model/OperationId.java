package com.transferwise.idempotence4j.benchmarks.domain.model;

import com.github.f4b6a3.uuid.UuidCreator;
import lombok.Value;

import java.util.UUID;

@Value
public class OperationId {
    private UUID value;

    public static OperationId nextId() {
        return new OperationId(UuidCreator.getTimeOrdered());
    }
}
