package com.transferwise.idempotence4j.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Rejection of an idempotent request: missing or invalid key ({@code 400}), request with the same key
 * in progress ({@code 409}) or same key reused with a different payload ({@code 422}).
 */
public class IdempotentRequestException extends ResponseStatusException {
    public IdempotentRequestException(HttpStatus status, String reason) {
        super(status, reason);
    }

    public IdempotentRequestException(HttpStatus status, String reason, Throwable cause) {
        super(status, reason, cause);
    }
}
