package com.transferwise.idempotence4j.web;

/**
 * A request body without accessors, only serializable by a mapper configured with field visibility
 */
public class FieldOnlyBody {
    private final String name;

    public FieldOnlyBody(String name) {
        this.name = name;
    }
}
