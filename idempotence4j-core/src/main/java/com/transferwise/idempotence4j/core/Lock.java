package com.transferwise.idempotence4j.core;

import java.io.Closeable;

public interface Lock extends Closeable {
    void release();

    @Override
    default void close() {
        this.release();
    }
}
