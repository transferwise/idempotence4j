package com.transferwise.idempotence4j.core

import com.transferwise.idempotence4j.core.exception.ActionPayloadMismatchException
import groovy.transform.Synchronized

import java.lang.reflect.Type
import java.util.function.Function
import java.util.function.Supplier

class EphemeralIdempotenceService implements IdempotenceService {
    Map<ActionId, Object> storage = [:]
    Map<ActionId, String> fingerprints = [:]

    @Override
    def <S, R> R execute(ActionId actionId, Function<S, R> onRetry, Supplier<R> procedure, Function<R, S> toRecord, Type recordType) {
        return execute(actionId, null, onRetry, procedure, toRecord, recordType)
    }

    @Override
    @Synchronized
    def <S, R> R execute(ActionId actionId, String fingerprint, Function<S, R> onRetry, Supplier<R> procedure, Function<R, S> toRecord, Type recordType) {
        if(storage.containsKey(actionId)) {
            def storedFingerprint = fingerprints.get(actionId)
            if(storedFingerprint != null && fingerprint != null && storedFingerprint != fingerprint) {
                throw new ActionPayloadMismatchException("Action already submitted with a different payload")
            }
            return onRetry.apply(storage.get(actionId))
        }

        R result = procedure.get()
        storage.put(actionId, toRecord.apply(result))
        fingerprints.put(actionId, fingerprint)
        return result
    }
}
