package com.transferwise.idempotence4j.benchmarks.configuration;

import com.transferwise.idempotence4j.benchmarks.domain.model.OperationRepository;
import com.transferwise.idempotence4j.benchmarks.infrastructure.JdbcOperationRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class RepositoryConfiguration {

    @Bean
    public OperationRepository operationRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcOperationRepository(jdbcTemplate);
    }
}
