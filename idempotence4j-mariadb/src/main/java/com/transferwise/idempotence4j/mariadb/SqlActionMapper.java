package com.transferwise.idempotence4j.mariadb;

import com.transferwise.idempotence4j.core.Action;
import com.transferwise.idempotence4j.core.ActionId;
import com.transferwise.idempotence4j.core.Result;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import static com.transferwise.idempotence4j.jdbc.utils.Hydrator.hydrateField;

public class SqlActionMapper {
    public Action toEntity(ResultSet rs) throws SQLException {
        SqlAction action = new SqlAction(rs.getLong("seq_id"), toId(rs));

        hydrateResult(action, rs);
        hydrateInstant(action, rs, "createdAt", "created_at");
        hydrateInstant(action, rs, "lastRunAt", "last_run_at");
        hydrateInstant(action, rs, "completedAt", "completed_at");

        return action;
    }

    public ActionId toId(ResultSet rs) throws SQLException {
        return new ActionId(rs.getString("key"), rs.getString("type"), rs.getString("client"));
    }

    public SqlParameterSource toSql(SqlAction action) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("seqId", action.getSequentialId().orElse(null));
        parameters.addValue("key", action.getActionId().getKey());
        parameters.addValue("type", action.getActionId().getType());
        parameters.addValue("client", action.getActionId().getClient());
        parameters.addValue("createdAt", Timestamp.from(action.getCreatedAt()));
        parameters.addValue("lastRunAt", action.getLastRunAt().map(Timestamp::from).orElse(null));
        parameters.addValue("completedAt", action.getCompletedAt().map(Timestamp::from).orElse(null));
        parameters.addValue("result", action.hasResult() ? action.getResult().get().getContent() : null);
        parameters.addValue("resultType", action.hasResult() ? action.getResult().get().getType() : null);
        return parameters;
    }

    private void hydrateResult(Action action, ResultSet rs) throws SQLException {
        byte[] content = rs.getBytes("result");

        if (!rs.wasNull()) {
            Result result = new Result(content, rs.getString("result_type"));
            hydrateField("result", action, result);
        }
    }

    private void hydrateInstant(Action action, ResultSet rs, String property, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);

        if (!rs.wasNull()) {
            hydrateField(property, action, timestamp.toInstant());
        }
    }
}
