package com.transferwise.idempotence4j.core;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * An action result serializer/de-serializer.
 *
 * Output of serialization is going to be persisted in the database.
 *
 * In case of successful completion for any subsequent retry requests the output of de-serialization
 * is going to be surfaced to clients.
 */
public interface ResultSerializer {
    <T> byte[] serialize(T result) throws IOException;

    <T> T deserialize(byte[] payload, Type type) throws IOException;

    String getType();
}
