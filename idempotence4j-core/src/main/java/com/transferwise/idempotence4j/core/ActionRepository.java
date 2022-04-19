package com.transferwise.idempotence4j.core;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ActionRepository {
    Optional<Action> find(ActionId actionId);
    Action insertOrGet(Action action);
    Action update(Action action);

    /**
     * Deletes actions from the repository that were first executed before the provided timestamp, and limited by the batch size.
     * @return the number of actions deleted
     */
    int deleteOlderThan(Instant timestamp, int batchSize);

    /**
     * Batch deletes actions from the repository by their IDs.
     * @return An array containing the number of actions deleted, per action ID passed in as an argument.
     */
    int[] deleteByIds(List<ActionId> actionIds);
}
