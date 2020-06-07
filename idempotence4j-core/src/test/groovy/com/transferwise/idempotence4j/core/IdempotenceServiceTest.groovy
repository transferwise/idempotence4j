package com.transferwise.idempotence4j.core

import com.transferwise.idempotence4j.core.exception.ConflictingActionException
import com.transferwise.idempotence4j.core.exception.ResultSerializationException
import org.springframework.transaction.PlatformTransactionManager
import com.transferwise.idempotence4j.factory.ActionTestFactory.TestResult
import spock.lang.Specification
import spock.lang.Subject

import java.util.function.Function
import java.util.function.Supplier

import static com.transferwise.idempotence4j.factory.ActionTestFactory.aResult
import static com.transferwise.idempotence4j.factory.ActionTestFactory.anAction
import static com.transferwise.idempotence4j.factory.ActionTestFactory.anActionId

class IdempotenceServiceTest extends Specification {
    def platformTransactionManager = Mock(PlatformTransactionManager)
    def lockProvider = Mock(LockProvider)
    def actionRepository = Mock(ActionRepository)
    def resultSerializer = Mock(ResultSerializer)
    def lock = Mock(Lock)

    @Subject
    def service = new IdempotenceService(platformTransactionManager, lockProvider, actionRepository, resultSerializer)

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
            })
        then:
            output == result
        and:
            1 * actionRepository.insertOrGet({
                it.actionId == actionId
            } as Action) >> action
            1 * actionRepository.find(actionId) >> Optional.of(action)
            1 * lockProvider.lock(actionId) >> Optional.of(lock)
            0 * function.apply(_ as String)
            1 * actionRepository.update({
                it.actionId == actionId
                it.lastRunAt.get()
                verifyAll(it.result.get()) {
                    content == persistedResult.getBytes()
                    type == "json"
                }
            } as Action) >> action
    }

    def "should return persisted result on subsequent retry calls"() {
        given:
            def actionId = anActionId()
            def action = anAction(actionId: actionId, isCompleted: true)
            def result = new Object()
            def persistedResult = aResult()
        and:
            def onRetry = Mock(Function)
            def procedure = Mock(Supplier)
            def toRecord = Mock(Function)
        and:
            resultSerializer.deserialize(_ as byte[]) >> persistedResult
        when:
            def output = service.execute(actionId, onRetry, procedure, toRecord)
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
            service.execute(actionId, Mock(Function), Mock(Supplier), Mock(Function))
        then:
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
            lockProvider.lock(actionId) >> Optional.of(lock)
        and:
            resultSerializer.serialize(persistedResult) >> { throw new IOException() }
        when:
            service.execute(actionId, Mock(Function), {
                return result
            }, { it ->
                return persistedResult
            })
        then:
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
            resultSerializer.deserialize(_ as byte[]) >> {throw new IOException()}
        when:
            service.execute(actionId, Mock(Function), Mock(Supplier), Mock(Function))
        then:
            thrown(ResultSerializationException)
    }
}
