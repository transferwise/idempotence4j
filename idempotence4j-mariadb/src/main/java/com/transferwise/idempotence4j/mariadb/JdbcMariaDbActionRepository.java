package com.transferwise.idempotence4j.mariadb;

import com.transferwise.idempotence4j.core.Action;
import com.transferwise.idempotence4j.core.ActionId;
import com.transferwise.idempotence4j.core.ActionRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class JdbcMariaDbActionRepository implements ActionRepository {
	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	private final SqlActionMapper sqlMapper = new SqlActionMapper();

	public JdbcMariaDbActionRepository(JdbcTemplate jdbcTemplate) {
		this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
	}

	@Override
	public Optional<Action> find(ActionId actionId) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
            .addValue("key", actionId.getKey())
            .addValue("type", actionId.getType())
            .addValue("client", actionId.getClient());

		return namedParameterJdbcTemplate.query(FIND_BY_ID_SQL, parameters, (rs, rowNum) -> sqlMapper.toEntity(rs))
				.stream()
				.findFirst();
	}

	@Override
	public Action insertOrGet(Action action) {
        return find(action.getActionId())
            .orElseGet(() -> {
                namedParameterJdbcTemplate.update(UPSERT_SQL, sqlMapper.toSql(SqlAction.of(action)));
                return action;
            });
	}

	@Override
	public Action update(Action action) {
		namedParameterJdbcTemplate.update(UPDATE_SQL, sqlMapper.toSql(SqlAction.of(action)));
		return action;
	}

    @Override
    public void deleteOlderThan(Instant timestamp, int batchSize) {
        MapSqlParameterSource queryParameters = new MapSqlParameterSource()
            .addValue("createdAt", Timestamp.from(timestamp))
            .addValue("limit", batchSize);

        List<ActionId> actionIdList = namedParameterJdbcTemplate.query(FIND_OLDER_THAN_SQL, queryParameters, (rs, rowNum) -> sqlMapper.toId(rs));

        deleteByIds(actionIdList);
    }

    @Override
    public void deleteByIds(List<ActionId> actionIdList) {
        MapSqlParameterSource[] deleteBatchParameters = actionIdList.stream().map(actionId -> new MapSqlParameterSource()
            .addValue("key", actionId.getKey())
            .addValue("type", actionId.getType())
            .addValue("client", actionId.getClient()))
            .toArray(size -> new MapSqlParameterSource[size]);

        namedParameterJdbcTemplate.batchUpdate(DELETE_SQL, deleteBatchParameters);
    }

    //@formatter:off
	private final static String FIND_BY_ID_SQL =
			"SELECT " +
				"seq_id, `key`, type, client, created_at, last_run_at, completed_at, result, result_type " +
			"FROM idempotent_action " +
			"WHERE " +
				"`key` = :key " +
				"AND type = :type " +
				"AND client = :client " +
			"LIMIT 1";

	private final static String UPSERT_SQL =
			"INSERT INTO " +
				"idempotent_action(seq_id, `key`, type, client, created_at, last_run_at, completed_at, result, result_type) " +
			"VALUES (" +
				":seqId, " +
				":key, " +
				":type, " +
				":client, " +
				":createdAt, " +
				":lastRunAt, " +
				":completedAt, " +
				":result, " +
				":resultType" +
				")" +
			"ON DUPLICATE KEY UPDATE " +
				"`key` = `key`, " +
				"type = type, " +
				"client = client";

	private final static String UPDATE_SQL =
			"INSERT INTO " +
				"idempotent_action(seq_id, `key`, type, client, created_at, last_run_at, completed_at, result, result_type) " +
			"VALUES (" +
				":seqId, " +
				":key, " +
				":type, " +
				":client, " +
				":createdAt, " +
				":lastRunAt, " +
				":completedAt, " +
				":result, " +
				":resultType" +
			")" +
			"ON DUPLICATE KEY UPDATE " +
				"last_run_at = :lastRunAt, " +
				"completed_at = :completedAt, " +
				"result = :result, " +
				"result_type = :resultType";

    private final static String FIND_OLDER_THAN_SQL =
        "SELECT " +
            "`key`, type, client " +
            "FROM idempotent_action " +
            "WHERE " +
            "created_at < :createdAt " +
            "LIMIT :limit";

    private final static String DELETE_SQL =
        "DELETE " +
            "FROM idempotent_action " +
            "WHERE " +
            "`key` = :key " +
            "AND type = :type " +
            "AND client = :client";
	//@formatter:on
}
