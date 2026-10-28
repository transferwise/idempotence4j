package com.transferwise.idempotence4j.autoconfigure

import com.fasterxml.jackson.core.type.TypeReference
import com.transferwise.idempotence4j.autoconfigure.service.TestApplication
import com.transferwise.idempotence4j.core.ActionId
import com.transferwise.idempotence4j.core.ActionRepository
import com.transferwise.idempotence4j.core.IdempotenceService
import com.transferwise.idempotence4j.core.LockProvider
import com.transferwise.idempotence4j.postgres.JdbcPostgresActionRepository
import com.transferwise.idempotence4j.postgres.JdbcPostgresLockProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

@SpringBootTest(classes = TestApplication)
class DatabaseSelectionPostgresIntegrationTest extends Specification {
    @Autowired
    ActionRepository actionRepository
    @Autowired
    LockProvider lockProvider
    @Autowired
    IdempotenceService idempotenceService

    def "should wire the Postgres implementation for a Postgres datasource"() {
        expect:
            actionRepository instanceof JdbcPostgresActionRepository
            lockProvider instanceof JdbcPostgresLockProvider
    }

    def "should execute actions against Postgres"() {
        given:
            def actionId = new ActionId(UUID.randomUUID(), "SELECTION", "starter")
            def executions = 0
        when:
            def first = idempotenceService.execute(actionId, { -> ++executions }, new TypeReference<Integer>() {})
            def retry = idempotenceService.execute(actionId, { -> ++executions }, new TypeReference<Integer>() {})
        then:
            first == 1
            retry == 1
            executions == 1
    }
}
