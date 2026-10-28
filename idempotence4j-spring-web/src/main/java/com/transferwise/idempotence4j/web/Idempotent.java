package com.transferwise.idempotence4j.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Makes a Spring MVC handler method idempotent. The idempotency key is read from the request header
 * configured by {@code idempotence4j.web.key-header} ({@code Idempotency-Key} by default).
 * <p>
 * The first request with a given key executes the method and persists its return value. Retries with the same key
 * return the persisted value without executing the method again. Exceptions thrown by the method are not persisted,
 * so a retry executes the method again.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {
    /**
     * Action type, defaults to {@code SimpleClassName.methodName}
     */
    String type() default "";

    /**
     * Whether requests without an idempotency key are rejected with {@code 400 Bad Request}.
     * When {@code false}, such requests execute the method without idempotency guarantees.
     */
    boolean required() default true;
}
