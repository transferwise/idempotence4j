package com.transferwise.idempotence4j.core.exception;

public class ResultSerializationException extends RuntimeException {
    public ResultSerializationException(String message) {
        super(message);
    }

    public ResultSerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
