package com.transferwise.idempotence4j.core.executor

import spock.lang.Specification

import java.time.Duration
import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.function.Supplier

class MeasuredExecutorTest extends Specification {

    def "should treat both onSuccess and onError callbacks as optional"() {
        given:
            def call = Mock(Supplier)
            def outcome = new Object()
        when:
            def result = new MeasuredExecutor().submit(call)
        then:
            1 * call.get() >> outcome
            outcome == result
            noExceptionThrown()
    }

    def "should invoke onSuccess before returning result"() {
        given:
            def call = Mock(Supplier)
            def onSuccess = Mock(Consumer)
        when:
            new MeasuredExecutor()
                .onSuccess(onSuccess)
                .submit(call)
        then:
            1 * onSuccess.accept(_ as Duration)
    }

    def "should invoke onComplete before returning result"() {
        given:
            def call = Mock(Supplier)
            def onComplete = Mock(Runnable)
        when:
            new MeasuredExecutor()
                .onComplete(onComplete)
                .submit(call)
        then:
            1 * onComplete.run()
    }

    def "should invoke onUnexpectedError before returning result and rethrow exception"() {
        given:
            def call = Mock(Supplier)
            def onError = Mock(BiConsumer)
        and:
            call.get() >> { throw new RuntimeException() }
        when:
            new MeasuredExecutor()
                .onUnexpectedError(onError)
                .submit(call)
        then:
            1 * onError.accept(_ as Duration, _ as RuntimeException)
            thrown(RuntimeException)
    }

    def "should invoke onError before returning result and rethrow exception"() {
        given:
            def call = Mock(Supplier)
            def onError = Mock(BiConsumer)
        and:
            call.get() >> { throw new RuntimeException() }
        when:
            new MeasuredExecutor()
                .onError(RuntimeException.class, onError)
                .submit(call)
        then:
            1 * onError.accept(_ as Duration, _ as RuntimeException)
            thrown(RuntimeException)
    }

    def "should invoke onComplete before returning result even if call failed"() {
        given:
            def call = Mock(Supplier)
            def onComplete = Mock(Runnable)
        and:
            call.get() >> { throw new RuntimeException() }
        when:
            new MeasuredExecutor()
                .onComplete(onComplete)
                .submit(call)
        then:
            1 * onComplete.run()
            thrown(RuntimeException)
    }

    def "should suppress any exception coming from success callback"() {
        given:
            def call = Mock(Supplier)
            def onSuccess = Mock(Consumer)
        and:
            onSuccess.accept(_ as Duration) >> { throw new RuntimeException() }
        when:
            new MeasuredExecutor()
                .onSuccess(onSuccess)
                .submit(call)
        then:
            noExceptionThrown()
    }

    def "should suppress any exception coming from complete callback"() {
        given:
            def call = Mock(Supplier)
            def onComplete = Mock(Runnable)
        and:
            onComplete.run() >> { throw new RuntimeException() }
        when:
            new MeasuredExecutor()
                .onComplete(onComplete)
                .submit(call)
        then:
            noExceptionThrown()
    }

    def "should suppress any exception coming from error callback"() {
        given:
            def call = Mock(Supplier)
            def onError = Mock(BiConsumer)
        and:
            call.get() >> { throw new RuntimeException() }
            onError.accept(_ as Duration, _ as RuntimeException) >> {throw new IllegalStateException() }
        when:
            new MeasuredExecutor()
                .onError(RuntimeException.class, onError)
                .submit(call)
        then:
            thrown(RuntimeException)
    }
}
