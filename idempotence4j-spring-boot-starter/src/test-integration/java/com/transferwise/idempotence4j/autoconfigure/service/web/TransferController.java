package com.transferwise.idempotence4j.autoconfigure.service.web;

import com.transferwise.idempotence4j.web.Idempotent;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
public class TransferController {
    // state is exposed through methods, fields of the AOP proxy are not the ones of the target bean
    private final AtomicInteger executions = new AtomicInteger();
    private final AtomicBoolean failNext = new AtomicBoolean();
    private volatile CountDownLatch started;
    private volatile CountDownLatch release;

    private final JdbcTemplate jdbcTemplate;

    public TransferController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Idempotent
    @PostMapping("/transfers")
    public ResponseEntity<Transfer> create(@RequestBody Transfer transfer) throws InterruptedException {
        int id = executions.incrementAndGet();
        jdbcTemplate.update("INSERT INTO web_test_transfer(name) VALUES (?)", transfer.getName());

        if (started != null) {
            started.countDown();
            release.await();
        }
        if (failNext.getAndSet(false)) {
            throw new DownstreamException();
        }
        return ResponseEntity.created(URI.create("/transfers/" + id)).body(new Transfer(transfer.getName(), id));
    }

    public AtomicInteger executions() {
        return executions;
    }

    public void failNext() {
        failNext.set(true);
    }

    public void blockUntilReleased(CountDownLatch started, CountDownLatch release) {
        this.started = started;
        this.release = release;
    }

    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public static class DownstreamException extends RuntimeException {
    }
}
