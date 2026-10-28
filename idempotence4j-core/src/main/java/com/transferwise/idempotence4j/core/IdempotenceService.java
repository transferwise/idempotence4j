package com.transferwise.idempotence4j.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.transferwise.idempotence4j.core.exception.ActionPayloadMismatchException;

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

    /**
     * Same as {@link #execute(ActionId, Function, Supplier, Function, Type)}, additionally verifying that a repeated
     * action carries the same payload. If an action with the same identifier was already submitted with a different
     * fingerprint, {@link ActionPayloadMismatchException} is thrown instead of executing or replaying the action.
     *
     * @param fingerprint digest of the request payload computed by the client, e.g. a SHA-256 hash, up to 255 characters.
     *                    Longer values are rejected with {@link IllegalArgumentException}, {@code null} disables the check for this call
     */
    default <S, R> R execute(ActionId actionId, String fingerprint, Function<S, R> onRetry, Supplier<R> procedure, Function<R, S> toRecord, Type recordType) {
        throw new UnsupportedOperationException(getClass().getName() + " does not support payload fingerprints");
    }

    default <S, R> R execute(ActionId actionId, String fingerprint, Function<S, R> onRetry, Supplier<R> procedure, Function<R, S> toRecord, TypeReference<S> recordTypeRef) {
        return execute(actionId, fingerprint, onRetry, procedure, toRecord, recordTypeRef.getType());
    }

    default <R> R execute(ActionId actionId, String fingerprint, Supplier<R> action, Type recordType) {
        return execute(actionId, fingerprint, Function.identity(), action, Function.identity(), recordType);
    }

    default <R> R execute(ActionId actionId, String fingerprint, Supplier<R> action, TypeReference<R> recordTypeRef) {
        return execute(actionId, fingerprint, Function.identity(), action, Function.identity(), recordTypeRef);
    }
}
