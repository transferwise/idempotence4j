package com.transferwise.idempotence4j.benchmarks.configuration;

import com.transferwise.idempotence4j.benchmarks.application.CreateIdempotentOperation;
import com.transferwise.idempotence4j.benchmarks.application.CreateOperation;
import com.transferwise.idempotence4j.benchmarks.domain.model.OperationRepository;
import com.transferwise.idempotence4j.core.IdempotenceService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceConfiguration {

    @Bean
    public CreateIdempotentOperation createIdempotentOperation(OperationRepository operationRepository, IdempotenceService idempotenceService) {
        return new CreateIdempotentOperation(operationRepository, idempotenceService);
    }

    @Bean
    public CreateOperation createOperation(OperationRepository operationRepository) {
        return new CreateOperation(operationRepository);
    }
}
