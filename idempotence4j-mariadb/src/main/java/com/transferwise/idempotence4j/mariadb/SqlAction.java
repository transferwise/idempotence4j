package com.transferwise.idempotence4j.mariadb;

import com.transferwise.idempotence4j.core.Action;
import com.transferwise.idempotence4j.core.ActionId;
import com.transferwise.idempotence4j.core.Result;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

import java.time.Instant;
import java.util.Optional;

@Getter
@EqualsAndHashCode(callSuper = true)
public class SqlAction extends Action {
    private Long sequentialId;

    public static SqlAction of(Action action) {
        if(action instanceof SqlAction) {
            return (SqlAction) action;
        }

        SqlAction sqlAction = new SqlAction(
            action.getActionId(),
            action.getCreatedAt(),
            action.getLastRunAt().orElse(null),
            action.getCompletedAt().orElse(null),
            action.getResult().orElse(null)
        );

        return sqlAction;
    }

    public SqlAction(@NonNull Long sequentialId, @NonNull ActionId actionId) {
        super(actionId);
        this.sequentialId = sequentialId;
    }

    private SqlAction(
        @NonNull ActionId actionId,
        @NonNull Instant createdAt,
        Instant lastRunAt,
        Instant completedAt,
        Result result
    ) {
        super(actionId, createdAt, lastRunAt, completedAt, result);
    }

    public Optional<Long> getSequentialId() {
        return Optional.ofNullable(sequentialId);
    }
}
