package com.transferwise.idempotence4j.postgres;

import com.transferwise.idempotence4j.core.ActionId;
import com.transferwise.idempotence4j.core.Lock;
import com.transferwise.idempotence4j.core.LockProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.Optional;

public class JdbcPostgresLockProvider implements LockProvider {
	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	public JdbcPostgresLockProvider(JdbcTemplate jdbcTemplate) {
		this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
	}

	@Override
	public Optional<? extends Lock> lock(ActionId actionId) {
		MapSqlParameterSource params = new MapSqlParameterSource() {
			{
				addValue("key", actionId.getKey());
				addValue("type", actionId.getType());
				addValue("client", actionId.getClient());
			}
		};

		return namedParameterJdbcTemplate.query(LOCK_SQL, params, (rs, rowNum) -> new PostgresRowLock()).stream().findFirst();
	}

	//@formatter:off
	private final static String LOCK_SQL =
			"SELECT 1 " +
			"FROM idempotent_action " +
			"WHERE " +
				"key = :key " +
				"AND type = :type " +
				"AND client = :client " +
			"FOR UPDATE " +
			"SKIP LOCKED";
	//@formatter:on
}
