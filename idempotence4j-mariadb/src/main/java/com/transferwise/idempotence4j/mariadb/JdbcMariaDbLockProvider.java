package com.transferwise.idempotence4j.mariadb;

import com.transferwise.idempotence4j.core.ActionId;
import com.transferwise.idempotence4j.core.Lock;
import com.transferwise.idempotence4j.core.LockProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;

import javax.sql.DataSource;
import java.util.Optional;

@Slf4j
public class JdbcMariaDbLockProvider implements LockProvider {
	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final SqlActionMapper sqlMapper = new SqlActionMapper();

	public JdbcMariaDbLockProvider(JdbcTemplate jdbcTemplate) {
		// Spring 6 switched to SQLExceptionSubclassTranslator by default, which doesn't use
		// vendor-specific error codes. We need SQLErrorCodeSQLExceptionTranslator to properly
		// translate MariaDB error 1205 (lock wait timeout) to CannotAcquireLockException.
		//
		// We create a new JdbcTemplate instead of mutating the passed instance to avoid
		// affecting other components that may share it. Transaction safety is preserved
		// because Spring's DataSourceTransactionManager binds connections at the DataSource
		// level, not the JdbcTemplate level - both templates will use the same connection
		// within a transaction.
		DataSource dataSource = jdbcTemplate.getDataSource();
		JdbcTemplate localJdbcTemplate = new JdbcTemplate(dataSource);
		if (dataSource != null) {
			localJdbcTemplate.setExceptionTranslator(new SQLErrorCodeSQLExceptionTranslator(dataSource));
		}
		this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(localJdbcTemplate);
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
                "seq_id, `key`, type, client, created_at, last_run_at, completed_at, result, result_type " +
			"FROM idempotent_action " +
			"WHERE " +
				"`key` = :key " +
				"AND type = :type " +
				"AND client = :client " +
			"FOR UPDATE NOWAIT";
	//@formatter:on
}
