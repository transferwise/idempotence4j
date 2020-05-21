package com.transferwise.idempotence4j.postgres

import com.fasterxml.jackson.databind.ObjectMapper
import com.transferwise.idempotence4j.core.Action
import com.transferwise.idempotence4j.core.IdempotenceService
import com.transferwise.idempotence4j.core.exception.ConflictingActionException
import com.transferwise.idempotence4j.core.serializers.json.JsonResultSerializer
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.transaction.support.TransactionSynchronizationManager
import spock.lang.Subject

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

import static com.transferwise.idempotence4j.factory.ActionTestFactory.aResult
import static com.transferwise.idempotence4j.factory.ActionTestFactory.anAction
import static com.transferwise.idempotence4j.factory.ActionTestFactory.anActionId

class IdempotenceServicePostgresIntegrationTest extends IntegrationTest {
    def transactionManager = new DataSourceTransactionManager(dataSource)
    def jdbcTemplate = new JdbcTemplate(dataSource)
    def lockProvider = Spy(new JdbcPostgresLockProvider(jdbcTemplate))
    def repository = Spy(new JdbcPostgresActionRepository(jdbcTemplate))
    def resultSerializer = new JsonResultSerializer(new ObjectMapper())

    @Subject
    def service = new IdempotenceService(transactionManager, lockProvider, repository, resultSerializer)

    def "under a load of concurrent requests action body should only be executed once"() {
        given:
            def actionId = anActionId()
        and:
            def numberOfRequests = 100
            def pool = Executors.newFixedThreadPool(numberOfRequests)
            def executionCount = new AtomicInteger()
        and:
            def procedure = {
                executionCount.incrementAndGet()
                aResult()
            }
        when:
            numberOfRequests.times {
                pool.submit({
                    service.execute(actionId, procedure)
                })
            }
        then:
            await.within(5) {
                executionCount.get() == 1
            }
    }

    def "should only allow single side-effect for a given action"() {
        given:
            def actionId = anActionId()
            def result = aResult()
        and:
            def numberOfRetryRequests = 100
            def latch = new CountDownLatch(numberOfRetryRequests)
            def barrier = new CountDownLatch(1)
            def pool = Executors.newFixedThreadPool(numberOfRetryRequests + 1)
        and:
            def conflictErrorCount = new AtomicInteger()
        and:
            def original = {
                try {
                    service.execute(actionId, { ->
                        latch.await()
                        return result
                    })
                } finally {
                    barrier.countDown()
                }

            }
        and:
            def retry = {
                try {
                    service.execute(actionId, { -> null})
                } catch(ConflictingActionException ex) {
                    conflictErrorCount.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        when:
            pool.submit(original)
            sleep(200)
            numberOfRetryRequests.times {
                pool.submit(retry)
            }
        and:
            barrier.await()
        then:
            conflictErrorCount.get() == numberOfRetryRequests
            verifyAll(repository.find(actionId).get()) {
                hasCompleted()
                hasResult()
            }
    }

    def "should return existing result if has completed before"() {
        given:
            def actionId = anActionId()
            def action = anAction(actionId: actionId, isCompleted: true)
        and:
            repository.insertOrGet(action)
        when:
            service.execute(actionId, { -> null})
        then:
            1 * repository.insertOrGet({ it ->
                it.actionId == actionId
            } as Action)
            0 * lockProvider.lock(actionId)
            0 * repository.update(action)
    }

    def "should execute action procedure in a transaction context"() {
        given:
            def actionId = anActionId()
        when:
            service.execute(actionId, {
                assert TransactionSynchronizationManager.isActualTransactionActive()
                return null
            })
        then:
            noExceptionThrown()
    }
}
