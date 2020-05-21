package com.transferwise.idempotence4j.core;

import java.util.Optional;

public interface ActionRepository {
    Optional<Action> find(ActionId actionId);

    Action insertOrGet(Action action);

    Action update(Action action);
}
