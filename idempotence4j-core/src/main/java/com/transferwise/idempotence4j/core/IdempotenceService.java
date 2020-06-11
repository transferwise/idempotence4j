package com.transferwise.idempotence4j.core;

import com.fasterxml.jackson.core.type.TypeReference;

import java.lang.reflect.Type;
import java.util.function.Function;
import java.util.function.Supplier;

public interface IdempotenceService {
    <S, R> R execute(ActionId actionId, Function<S, R> onRetry, Supplier<R> procedure, Function<R, S> toRecord, Type recordType);

    default <S, R> R execute(ActionId actionId, Function<S, R> onRetry, Supplier<R> procedure, Function<R, S> toRecord, TypeReference<S> recordTypeRef) {
        return execute(actionId, onRetry, procedure, toRecord, recordTypeRef.getType());
    }

    default <R> R execute(ActionId actionId, Supplier<R> action, Type recordType) {
        return execute(actionId, Function.identity(), action, Function.identity(), recordType);
    }

    default <R> R execute(ActionId actionId, Supplier<R> action, TypeReference<R> recordTypeRef) {
        return execute(actionId, Function.identity(), action, Function.identity(), recordTypeRef);
    }
}
