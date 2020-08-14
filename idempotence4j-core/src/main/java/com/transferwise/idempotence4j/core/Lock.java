package com.transferwise.idempotence4j.core;

import lombok.Getter;
import lombok.NonNull;

import java.io.Closeable;

/**
 * Lock object holds an action under lock retrieved after lock was acquired.
 * That comes handy for database backed lock provider implementation as we can then avoid an extra database round trip.
 */
public abstract class Lock implements Closeable {
    @Getter
    private final Action lockedAction;

    public Lock(@NonNull Action lockedAction) {
        this.lockedAction = lockedAction;
    }

    abstract public void release();

    @Override
    public void close() {
        this.release();
    }
}
