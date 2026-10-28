package com.transferwise.idempotence4j.web;

import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.atomic.AtomicInteger;

@RestController
public class TestApiController implements TestApi {
    public final AtomicInteger executions = new AtomicInteger();

    @Override
    public TestItem create(TestItem item) {
        return new TestItem(item.getName(), executions.incrementAndGet());
    }
}
