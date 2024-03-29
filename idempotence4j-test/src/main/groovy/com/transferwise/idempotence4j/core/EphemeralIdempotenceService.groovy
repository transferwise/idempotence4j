package com.transferwise.idempotence4j.core

import groovy.transform.Synchronized

import java.lang.reflect.Type
import java.util.function.Function
import java.util.function.Supplier

class EphemeralIdempotenceService implements IdempotenceService {
    Map<ActionId, Object> storage = [:]

    @Override
    @Synchronized
    def <S, R> R execute(ActionId actionId, Function<S, R> onRetry, Supplier<R> procedure, Function<R, S> toRecord, Type recordType) {
        if(storage.containsKey(actionId)) {
            return onRetry.apply(storage.get(actionId))
        }

        R result = procedure.get()
        storage.put(actionId, toRecord.apply(result))
        return result
    }
}
