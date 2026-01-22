package com.transferwise.idempotence4j.mariadb;

import com.transferwise.idempotence4j.core.ActionId;
import com.transferwise.idempotence4j.core.Lock;
import com.transferwise.idempotence4j.core.LockProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * MariaDB lock provider using low-level JDBC for lock acquisition.
 * <p>
 * Uses direct error code checking instead of Spring's exception translation to avoid
 * coupling to Spring's internal exception mapping behavior which changed in Spring 6.
 * See docs/adr/0001-low-level-jdbc-for-lock-detection.md for details.
 */
@Slf4j
public class JdbcMariaDbLockProvider implements LockProvider {
    /**
     * MariaDB error code for lock wait timeout (ER_LOCK_WAIT_TIMEOUT).
     * Thrown when NOWAIT is used and the row is already locked.
     */
    private static final int LOCK_WAIT_TIMEOUT_ERROR = 1205;

    private final DataSource dataSource;
    private final SqlActionMapper sqlMapper = new SqlActionMapper();

    public JdbcMariaDbLockProvider(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Optional<? extends Lock> lock(ActionId actionId) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement ps = connection.prepareStatement(LOCK_SQL)) {
            ps.setString(1, actionId.getKey());
            ps.setString(2, actionId.getType());
            ps.setString(3, actionId.getClient());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new MaridDbRowLock(sqlMapper.toEntity(rs)));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == LOCK_WAIT_TIMEOUT_ERROR) {
                log.debug("Lock acquisition failed for action {}: row already locked", actionId);
                return Optional.empty();
            }
            throw new RuntimeException("Failed to acquire lock for action " + actionId, e);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    //@formatter:off
    private static final String LOCK_SQL =
            "SELECT " +
                "seq_id, `key`, type, client, created_at, last_run_at, completed_at, result, result_type " +
            "FROM idempotent_action " +
            "WHERE " +
                "`key` = ? " +
                "AND type = ? " +
                "AND client = ? " +
            "FOR UPDATE NOWAIT";
    //@formatter:on
}
