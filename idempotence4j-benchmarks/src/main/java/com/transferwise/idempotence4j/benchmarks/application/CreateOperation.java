package com.transferwise.idempotence4j.benchmarks.application;

import com.transferwise.idempotence4j.benchmarks.domain.model.Operation;
import com.transferwise.idempotence4j.benchmarks.domain.model.OperationId;
import com.transferwise.idempotence4j.benchmarks.domain.model.OperationRepository;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class CreateOperation {
    private final OperationRepository operationRepository;

    public Operation execute() {
        Operation operation = new Operation(OperationId.nextId());
        operationRepository.save(operation);
        return operation;
    }
}
