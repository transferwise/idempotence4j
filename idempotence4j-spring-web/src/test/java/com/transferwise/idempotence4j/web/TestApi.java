package com.transferwise.idempotence4j.web;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface TestApi {
    @Idempotent
    @PostMapping("/api/items")
    TestItem create(@RequestBody TestItem item);
}
