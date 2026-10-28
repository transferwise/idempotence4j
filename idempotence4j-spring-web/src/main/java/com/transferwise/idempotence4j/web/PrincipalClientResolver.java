package com.transferwise.idempotence4j.web;

import jakarta.servlet.http.HttpServletRequest;

import java.security.Principal;

/**
 * Uses the authenticated principal name as the client, or {@value #ANONYMOUS} for unauthenticated requests.
 */
public class PrincipalClientResolver implements IdempotentClientResolver {
    public static final String ANONYMOUS = "anonymous";

    @Override
    public String resolve(HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        return principal != null ? principal.getName() : ANONYMOUS;
    }
}
