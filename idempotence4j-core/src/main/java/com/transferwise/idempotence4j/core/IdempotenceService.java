package com.transferwise.idempotence4j.core;

import java.util.function.Function;
import java.util.function.Supplier;

public interface IdempotenceService {
    <S, R> R execute(ActionId actionId, Function<S, R> onRetry, Supplier<R> procedure, Function<R, S> toRecord);

    default <R> R execute(ActionId actionId, Supplier<R> action) {
        return execute(actionId, Function.identity(), action, Function.identity());
    }
}
