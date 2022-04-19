package com.transferwise.idempotence4j.core;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ActionRepository {
    Optional<Action> find(ActionId actionId);
    Action insertOrGet(Action action);
    Action update(Action action);
    void deleteOlderThan(Instant timestamp, int batchSize);
    void deleteByIds(List<ActionId> actionIds);
}
