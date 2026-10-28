package com.transferwise.idempotence4j.core

import com.fasterxml.jackson.core.type.TypeReference
import com.transferwise.idempotence4j.core.exception.ActionPayloadMismatchException
import com.transferwise.idempotence4j.core.exception.ConflictingActionException
import com.transferwise.idempotence4j.core.exception.ResultSerializationException
import com.transferwise.idempotence4j.core.metrics.Metrics
import com.transferwise.idempotence4j.core.metrics.MetricsPublisher
import org.springframework.transaction.PlatformTransactionManager
import com.transferwise.idempotence4j.factory.ActionTestFactory.TestResult
import spock.lang.Specification
import spock.lang.Subject

import java.lang.reflect.Type
import java.util.function.Function
import java.util.function.Supplier

import static com.transferwise.idempotence4j.core.metrics.Metrics.Outcome.CONFLICT
import static com.transferwise.idempotence4j.core.metrics.Metrics.Outcome.ERROR
import static com.transferwise.idempotence4j.core.metrics.Metrics.Outcome.MISMATCH
import static com.transferwise.idempotence4j.core.metrics.Metrics.Outcome.SUCCESS
import static com.transferwise.idempotence4j.factory.ActionTestFactory.aResult
import static com.transferwise.idempotence4j.factory.ActionTestFactory.anAction
import static com.transferwise.idempotence4j.factory.ActionTestFactory.anActionId

class DefaultIdempotenceServiceTest extends Specification {
    def platformTransactionManager = Mock(PlatformTransactionManager)
    def lockProvider = Mock(LockProvider)
    def actionRepository = Mock(ActionRepository)
    def resultSerializer = Mock(ResultSerializer)
    def metricsPublisher = Mock(MetricsPublisher)

    @Subject
    def service = new DefaultIdempotenceService(platformTransactionManager, lockProvider, actionRepository, resultSerializer, metricsPublisher)

    def "should successfully execute first time submitted action"() {
        given:
            def actionId = anActionId()
            def action = anAction(actionId: actionId)
        and:
            def function = Mock(Function)
            def result = aResult()
            def persistedResult = result.name
        and:
            resultSerializer.serialize(persistedResult) >> persistedResult.getBytes()
            resultSerializer.getType() >> "json"
        when:
            def output = service.execute(actionId, function, {
                return result
            }, { it ->
                return persistedResult
            }, new TypeReference<String>() {})
        then:
            output == result
        and:
            1 * actionRepository.insertOrGet({
                it.actionId == actionId
            } as Action) >> action
            1 * lockProvider.lock(actionId) >> Optional.of(new NoOpLock(action))
            0 * function.apply(_ as String)
            1 * actionRepository.update({
                it.actionId == actionId
                it.lastRunAt.get()
                verifyAll(it.result.get()) {
                    content == persistedResult.getBytes()
                    type == "json"
                }
            } as Action) >> action
        and:
            1 * metricsPublisher.publish({
                it.outcome == SUCCESS
            } as Metrics)
    }

    def "should skip execution if concurrent action completed it first"() {
        given:
            def actionId = anActionId()
            def action = anAction(actionId: actionId)
            def completedAction = anAction(actionId: actionId, isCompleted: true)
            def persistedResult = aResult()
            def typeRef = new TypeReference<TestResult>() {}
        and:
            def onRetry = Mock(Function)
            def procedure = Mock(Supplier)
            def toRecord = Mock(Function)
        and:
            resultSerializer.deserialize(_ as byte[], _ as Type) >> persistedResult
        when:
            def output = service.execute(actionId, onRetry, procedure, toRecord, typeRef)
        then:
            1 * actionRepository.insertOrGet({
                it.actionId == actionId
            } as Action) >> action
            1 * lockProvider.lock(actionId) >> Optional.of(new NoOpLock(completedAction))
            0 * procedure.get()
            0 * toRecord.apply(*_)
    }

    def "should return persisted result on subsequent retry calls"() {
        given:
            def actionId = anActionId()
            def action = anAction(actionId: actionId, isCompleted: true)
            def result = new Object()
            def persistedResult = aResult()
            def typeRef = new TypeReference<TestResult>() {}
        and:
            def onRetry = Mock(Function)
            def procedure = Mock(Supplier)
            def toRecord = Mock(Function)
        and:
            resultSerializer.deserialize(_ as byte[], _ as Type) >> persistedResult
        when:
            def output = service.execute(actionId, onRetry, procedure, toRecord, typeRef)
        then:
            1 * actionRepository.insertOrGet({
                it.actionId == actionId
            } as Action) >> action
            0 * lockProvider.lock(actionId)
            0 * procedure.get()
            0 * toRecord.apply(*_)
        and:
            result == output
            1 * onRetry.apply({
                it == persistedResult
            } as TestResult) >> result
        and:
            1 * metricsPublisher.publish({
                it.outcome == SUCCESS
                it.isRetry
            } as Metrics)
    }

    def "should fail with conflicting exception for concurrent action"() {
        given:
            def actionId = anActionId()
        and:
            actionRepository.insertOrGet({
                it.actionId == actionId
            } as Action) >> anAction(actionId: actionId)
        and:
            lockProvider.lock(actionId) >> Optional.empty()
        when:
            service.execute(actionId, Mock(Function), Mock(Supplier), Mock(Function), Mock(Type))
        then:
            1 * metricsPublisher.publish({
                it.outcome == CONFLICT
            } as Metrics)
            thrown(ConflictingActionException)
    }

    def "should fail with serialization exception if there is an exception while serializing result"() {
        given:
            def actionId = anActionId()
            def action = anAction(actionId: actionId)
            def result = aResult()
            def persistedResult = result.name
        and:
            actionRepository.insertOrGet({
                it.actionId == actionId
            } as Action) >> action
            actionRepository.find(actionId) >> Optional.of(action)
            lockProvider.lock(actionId) >> Optional.of(new NoOpLock(action))
        and:
            resultSerializer.serialize(persistedResult) >> { throw new IOException() }
        when:
            service.execute(actionId, Mock(Function), {
                return result
            }, { it ->
                return persistedResult
            }, new TypeReference<String>() {})
        then:
            1 * metricsPublisher.publish({
                it.outcome == ERROR
            } as Metrics)
            thrown(ResultSerializationException)
    }

    def "should fail with serialization exception if there is an exception while de-serializing persisted result"() {
        given:
            def actionId = anActionId()
        and:
            actionRepository.insertOrGet({
                it.actionId == actionId
            } as Action) >> anAction(actionId: actionId, isCompleted: true)
        and:
            resultSerializer.deserialize(_ as byte[], _ as Type) >> {throw new IOException()}
        when:
            service.execute(actionId, Mock(Function), Mock(Supplier), Mock(Function), Mock(Type))
        then:
            1 * metricsPublisher.publish(_ as Metrics)
            thrown(ResultSerializationException)
    }

    def "should persist fingerprint when submitting a new action"() {
        given:
            def actionId = anActionId()
            def action = anAction(actionId: actionId, fingerprint: "fp-1")
        and:
            resultSerializer.serialize(_) >> "result".getBytes()
            resultSerializer.getType() >> "json"
        when:
            service.execute(actionId, "fp-1", { -> aResult() }, new TypeReference<TestResult>() {})
        then:
            1 * actionRepository.insertOrGet({
                it.actionId == actionId && it.fingerprint.get() == "fp-1"
            } as Action) >> action
            1 * lockProvider.lock(actionId) >> Optional.of(new NoOpLock(action))
            1 * actionRepository.update(_ as Action) >> action
    }

    def "should fail with mismatch exception for a completed action submitted with a different fingerprint"() {
        given:
            def actionId = anActionId()
        and:
            def onRetry = Mock(Function)
            def procedure = Mock(Supplier)
        and:
            actionRepository.insertOrGet(_ as Action) >> anAction(actionId: actionId, isCompleted: true, fingerprint: "fp-1")
        when:
            service.execute(actionId, "fp-2", onRetry, procedure, Mock(Function), new TypeReference<TestResult>() {})
        then:
            thrown(ActionPayloadMismatchException)
            0 * lockProvider.lock(_)
            0 * resultSerializer.deserialize(*_)
            0 * onRetry.apply(_)
            0 * procedure.get()
        and:
            1 * metricsPublisher.publish({
                it.outcome == MISMATCH
                !it.isRetry
            } as Metrics)
    }

    def "should fail with mismatch exception for an in progress action submitted with a different fingerprint"() {
        given:
            def actionId = anActionId()
            def procedure = Mock(Supplier)
        and:
            actionRepository.insertOrGet(_ as Action) >> anAction(actionId: actionId, fingerprint: "fp-1")
        when:
            service.execute(actionId, "fp-2", procedure, new TypeReference<TestResult>() {})
        then:
            thrown(ActionPayloadMismatchException)
            0 * lockProvider.lock(_)
            0 * procedure.get()
            1 * metricsPublisher.publish({
                it.outcome == MISMATCH
            } as Metrics)
    }

    def "should fail with mismatch exception when the locked action has a different fingerprint"() {
        given:
            def actionId = anActionId()
            def procedure = Mock(Supplier)
        and: "a concurrent first submission returned its own action on insert"
            actionRepository.insertOrGet(_ as Action) >> anAction(actionId: actionId, fingerprint: "fp-2")
            lockProvider.lock(actionId) >> Optional.of(new NoOpLock(anAction(actionId: actionId, fingerprint: "fp-1")))
        when:
            service.execute(actionId, "fp-2", procedure, new TypeReference<TestResult>() {})
        then:
            thrown(ActionPayloadMismatchException)
            0 * procedure.get()
            0 * actionRepository.update(_)
            1 * metricsPublisher.publish({
                it.outcome == MISMATCH
            } as Metrics)
    }

    def "should return persisted result on retry with the same fingerprint"() {
        given:
            def actionId = anActionId()
            def persistedResult = aResult()
            def onRetry = Mock(Function)
        and:
            actionRepository.insertOrGet(_ as Action) >> anAction(actionId: actionId, isCompleted: true, fingerprint: "fp-1")
            resultSerializer.deserialize(_ as byte[], _ as Type) >> persistedResult
        when:
            service.execute(actionId, "fp-1", onRetry, Mock(Supplier), Mock(Function), new TypeReference<TestResult>() {})
        then:
            1 * onRetry.apply(persistedResult)
            1 * metricsPublisher.publish({
                it.outcome == SUCCESS
                it.isRetry
            } as Metrics)
    }

    def "should reject a fingerprint longer than the maximum length before persisting the action"() {
        given:
            def procedure = Mock(Supplier)
        when:
            service.execute(anActionId(), "a" * (Action.MAX_FINGERPRINT_LENGTH + 1), procedure, new TypeReference<TestResult>() {})
        then:
            thrown(IllegalArgumentException)
            0 * actionRepository.insertOrGet(_)
            0 * procedure.get()
    }

    def "should skip fingerprint check for an action persisted without a fingerprint"() {
        given:
            def actionId = anActionId()
            def persistedResult = aResult()
            def onRetry = Mock(Function)
        and:
            actionRepository.insertOrGet(_ as Action) >> anAction(actionId: actionId, isCompleted: true)
            resultSerializer.deserialize(_ as byte[], _ as Type) >> persistedResult
        when:
            service.execute(actionId, "fp-2", onRetry, Mock(Supplier), Mock(Function), new TypeReference<TestResult>() {})
        then:
            1 * onRetry.apply(persistedResult)
    }
}
