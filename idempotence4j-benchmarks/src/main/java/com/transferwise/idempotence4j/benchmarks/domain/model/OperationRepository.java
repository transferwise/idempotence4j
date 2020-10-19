package com.transferwise.idempotence4j.benchmarks.domain.model;

public interface OperationRepository {
    void save(Operation operation);
}
