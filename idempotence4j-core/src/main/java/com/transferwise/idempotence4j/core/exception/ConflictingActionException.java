package com.transferwise.idempotence4j.core.exception;

public class ConflictingActionException extends RuntimeException {
    public ConflictingActionException(String message) {
        super(message);
    }
}
