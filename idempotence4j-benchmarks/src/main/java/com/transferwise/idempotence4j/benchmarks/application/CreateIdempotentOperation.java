package com.transferwise.idempotence4j.benchmarks.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.transferwise.idempotence4j.benchmarks.domain.model.Operation;
import com.transferwise.idempotence4j.benchmarks.domain.model.OperationId;
import com.transferwise.idempotence4j.benchmarks.domain.model.OperationRepository;
import com.transferwise.idempotence4j.core.ActionId;
import com.transferwise.idempotence4j.core.IdempotenceService;
import lombok.AllArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
public class CreateIdempotentOperation {
    private final OperationRepository operationRepository;
    private final IdempotenceService idempotenceService;

    public Operation execute(UUID key) {
        ActionId actionId = new ActionId(key, "CREATE_OPERATION", "internal_client");
        Operation result = idempotenceService.execute(actionId, this::byId, () -> {
            Operation operation = new Operation(OperationId.nextId());
            operationRepository.save(operation);
            return operation;
        }, Operation::getOperationId, new TypeReference<OperationId>(){});

        return result;
    }

    private Operation byId(OperationId operationId) {
        return new Operation(operationId);
    }
}
