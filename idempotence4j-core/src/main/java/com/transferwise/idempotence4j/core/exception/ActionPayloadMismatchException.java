package com.transferwise.idempotence4j.core.exception;

public class ActionPayloadMismatchException extends RuntimeException {
    public ActionPayloadMismatchException(String message) {
        super(message);
    }
}
