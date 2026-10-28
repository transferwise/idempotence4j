package com.transferwise.idempotence4j.web

import org.springframework.http.HttpEntity
import org.springframework.http.ResponseEntity
import org.springframework.web.context.request.async.DeferredResult
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import spock.lang.Specification
import spock.lang.Subject

import java.util.concurrent.CompletableFuture

class IdempotentPointcutTest extends Specification {
    @Subject
    def pointcut = new IdempotentPointcut()

    def "should match annotated methods with supported return types"() {
        expect:
            pointcut.matches(Handlers.getMethod(method), Handlers) == matches
        where:
            method        || matches
            "item"        || true
            "entity"      || true
            "entityList"  || true
            "none"        || true
            "notAnnotated"|| false
    }

    def "should fail on annotated methods whose response cannot be persisted"() {
        when:
            pointcut.matches(Handlers.getMethod(method), Handlers)
        then:
            def ex = thrown(IllegalStateException)
            ex.message.contains(method)
        where:
            method << ["deferred", "future", "emitter", "streamingEntity", "rawEntity"]
    }

    static class Handlers {
        @Idempotent TestItem item() { null }
        @Idempotent ResponseEntity<TestItem> entity() { null }
        @Idempotent ResponseEntity<List<TestItem>> entityList() { null }
        @Idempotent void none() {}
        TestItem notAnnotated() { null }
        @Idempotent DeferredResult<TestItem> deferred() { null }
        @Idempotent CompletableFuture<TestItem> future() { null }
        @Idempotent SseEmitter emitter() { null }
        @Idempotent ResponseEntity<StreamingResponseBody> streamingEntity() { null }
        @Idempotent HttpEntity<TestItem> rawEntity() { null }
    }
}
