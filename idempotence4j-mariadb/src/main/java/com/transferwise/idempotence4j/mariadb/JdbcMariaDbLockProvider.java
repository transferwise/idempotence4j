package com.transferwise.idempotence4j.mariadb;

import com.transferwise.idempotence4j.core.ActionId;
import com.transferwise.idempotence4j.core.Lock;
import com.transferwise.idempotence4j.core.LockProvider;
import com.transferwise.idempotence4j.jdbc.mapper.ActionSqlMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.Optional;

@Slf4j
public class JdbcMariaDbLockProvider implements LockProvider {
	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final ActionSqlMapper sqlMapper = new ActionSqlMapper();

	public JdbcMariaDbLockProvider(JdbcTemplate jdbcTemplate) {
		this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
	}

	@Override
	public Optional<? extends Lock> lock(ActionId actionId) {
        MapSqlParameterSource parameters =new MapSqlParameterSource()
            .addValue("key", actionId.getKey())
            .addValue("type", actionId.getType())
            .addValue("client", actionId.getClient());

        try {
            return namedParameterJdbcTemplate.query(LOCK_SQL, parameters, (rs, rowNum) -> new MaridDbRowLock(sqlMapper.toEntity(rs)))
                .stream()
                .findFirst();
        } catch (CannotAcquireLockException ex) {
            return Optional.empty();
        }
	}

	//@formatter:off
	private final static String LOCK_SQL =
            "SELECT " +
                "`key`, type, client, created_at, last_run_at, completed_at, result, result_type " +
			"FROM idempotent_action " +
			"WHERE " +
				"`key` = :key " +
				"AND type = :type " +
				"AND client = :client " +
			"FOR UPDATE NOWAIT";
	//@formatter:on
}
