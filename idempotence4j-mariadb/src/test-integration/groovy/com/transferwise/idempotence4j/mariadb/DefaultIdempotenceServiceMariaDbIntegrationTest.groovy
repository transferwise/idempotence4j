package com.transferwise.idempotence4j.mariadb

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.transferwise.idempotence4j.core.Action
import com.transferwise.idempotence4j.core.DefaultIdempotenceService
import com.transferwise.idempotence4j.core.exception.ConflictingActionException
import com.transferwise.idempotence4j.core.metrics.MetricsPublisher
import com.transferwise.idempotence4j.core.serializers.json.JsonResultSerializer
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.transaction.support.TransactionSynchronizationManager
import spock.lang.Subject

import java.lang.reflect.Type
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import com.transferwise.idempotence4j.factory.ActionTestFactory.TestResult

import static com.transferwise.idempotence4j.factory.ActionTestFactory.aResult
import static com.transferwise.idempotence4j.factory.ActionTestFactory.anActionId

class DefaultIdempotenceServiceMariaDbIntegrationTest extends IntegrationTest {
    def transactionManager = new DataSourceTransactionManager(dataSource)
    def jdbcTemplate = new JdbcTemplate(dataSource)
    def lockProvider = new JdbcMariaDbLockProvider(jdbcTemplate)
    def repository = new JdbcMariaDbActionRepository(jdbcTemplate)
    def resultSerializer = new JsonResultSerializer(new ObjectMapper().registerModule(new JavaTimeModule()))
    def metricsPublisher = Mock(MetricsPublisher)

    @Subject
    def service = new DefaultIdempotenceService(transactionManager, lockProvider, repository, resultSerializer, metricsPublisher)

    def "under a load of concurrent requests action body should only be executed once"() {
        given:
            def actionId = anActionId()
            def result = aResult()
            def typeRef = new TypeReference<TestResult>() {}
        and:
            def numberOfRequests = 100
            def pool = Executors.newFixedThreadPool(numberOfRequests)
            def executionCount = new AtomicInteger()
        and:
            def procedure = {
                executionCount.incrementAndGet()
                result
            }
        when:
            numberOfRequests.times {
                pool.submit({
                    service.execute(actionId, procedure, typeRef)
                })
            }
        then:
            await.within(50) {
                executionCount.get() == 1
            }
    }

    def "should only allow single side-effect for a given action"() {
        given:
            def actionId = anActionId()
            def result = aResult()
            def typeRef = new TypeReference<TestResult>() {}
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
                    }, typeRef)
                } finally {
                    barrier.countDown()
                }
            }
        and:
            def retry = {
                try {
                    service.execute(actionId, { -> null}, typeRef)
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
            def result =  List.of(aResult())
            def typeRef = new TypeReference<List<TestResult>>() {}
        and:
            repository = Spy(repository)
            resultSerializer = Spy(resultSerializer)
            service = new DefaultIdempotenceService(transactionManager, lockProvider, repository, resultSerializer, metricsPublisher)
        when:
            service.execute(actionId, { List r -> r}, { -> result}, { List r -> r}, typeRef)
        and:
            def retryOutcome = service.execute(actionId, { List r -> r}, { -> result}, { List r -> r}, typeRef)
        then:
            1 * repository.update(_ as Action)
            1 * resultSerializer.deserialize(_ as byte[], _ as Type)
        and:
            retryOutcome[0] == result[0]
    }

    def "should execute action procedure in a transaction context"() {
        given:
            def actionId = anActionId()
        when:
            service.execute(actionId, {
                assert TransactionSynchronizationManager.isActualTransactionActive()
                return aResult()
            }, new TypeReference<TestResult>() {})
        then:
            noExceptionThrown()
    }
}
