package com.transferwise.idempotence4j.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@RestController
public class TestController {
    public final AtomicInteger executions = new AtomicInteger();
    public final AtomicBoolean failNext = new AtomicBoolean();
    public final AtomicReference<Class<?>> handledException = new AtomicReference<>();

    @Idempotent
    @PostMapping("/items")
    public TestItem create(@RequestBody TestItem item) {
        int id = executions.incrementAndGet();
        if (failNext.getAndSet(false)) {
            throw new DownstreamException();
        }
        return new TestItem(item.getName(), id);
    }

    @Idempotent
    @PostMapping("/entities")
    public ResponseEntity<TestItem> createEntity(@RequestBody TestItem item) {
        int id = executions.incrementAndGet();
        return ResponseEntity.created(URI.create("/entities/" + id))
            .header("X-Custom", "first", "second")
            .body(new TestItem(item.getName(), id));
    }

    @Idempotent(required = false, type = "optional-create")
    @PostMapping("/optional")
    public TestItem createOptional(@RequestBody TestItem item) {
        return new TestItem(item.getName(), executions.incrementAndGet());
    }

    @Idempotent
    @PostMapping("/checked")
    public TestItem createChecked(@RequestBody TestItem item) throws IOException {
        executions.incrementAndGet();
        throw new IOException("unavailable");
    }

    @Idempotent
    @DeleteMapping("/items/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Integer id) {
        executions.incrementAndGet();
    }

    // matches any exception, so a wrapped checked exception would be recorded as the wrapper
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Void> handleException(Exception ex) throws Exception {
        handledException.set(ex.getClass());
        if (ex instanceof IOException) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        throw ex;
    }

    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public static class DownstreamException extends RuntimeException {
    }
}
