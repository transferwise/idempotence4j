package com.transferwise.idempotence4j.autoconfigure;

import lombok.Data;

@Data
public class IdempotentWebProperties {
    /**
     * Request header carrying the idempotency key of {@code @Idempotent} handler methods
     * Default value: Idempotency-Key
     */
    private String keyHeader = "Idempotency-Key";
}
