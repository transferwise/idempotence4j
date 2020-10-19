package com.transferwise.idempotence4j.benchmarks.infrastructure;

import com.transferwise.idempotence4j.benchmarks.domain.model.Operation;
import com.transferwise.idempotence4j.benchmarks.domain.model.OperationRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.Timestamp;

public class JdbcOperationRepository implements OperationRepository {
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public JdbcOperationRepository(JdbcTemplate jdbcTemplate) {
        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    @Override
    public void save(Operation operation) {
        MapSqlParameterSource queryParameters = new MapSqlParameterSource()
            .addValue("id", operation.getOperationId().getValue().toString())
            .addValue("createdAt", Timestamp.from(operation.getCreatedAt()));

        namedParameterJdbcTemplate.update(INSERT_SQL, queryParameters);
    }

    //@formatter:off
    private final static String INSERT_SQL =
        "INSERT INTO " +
            "operation(id, created_at) " +
            "VALUES (" +
            ":id, " +
            ":createdAt" +
            ")";
    //@formatter:on
}
