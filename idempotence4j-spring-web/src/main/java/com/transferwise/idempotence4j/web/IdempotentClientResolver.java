package com.transferwise.idempotence4j.web;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Resolves the client of an idempotent request. The client is part of the action identifier,
 * so the same idempotency key sent by different clients identifies different actions.
 */
@FunctionalInterface
public interface IdempotentClientResolver {
    String resolve(HttpServletRequest request);
}
