package com.transferwise.idempotence4j.autoconfigure

import com.fasterxml.jackson.core.type.TypeReference
import com.transferwise.idempotence4j.autoconfigure.service.TestApplication
import com.transferwise.idempotence4j.core.ActionId
import com.transferwise.idempotence4j.core.ActionRepository
import com.transferwise.idempotence4j.core.IdempotenceService
import com.transferwise.idempotence4j.core.LockProvider
import com.transferwise.idempotence4j.mariadb.JdbcMariaDbActionRepository
import com.transferwise.idempotence4j.mariadb.JdbcMariaDbLockProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

@SpringBootTest(classes = TestApplication, properties = [
    "spring.datasource.url=jdbc:mariadb://localhost:3306/idempotence4j_db",
    "spring.datasource.driver-class-name=org.mariadb.jdbc.Driver",
    "spring.flyway.locations=classpath:db/idempotence4j/mariadb",
    "idempotence4j.retention.enabled=false"
])
class DatabaseSelectionMariaDbIntegrationTest extends Specification {
    @Autowired
    ActionRepository actionRepository
    @Autowired
    LockProvider lockProvider
    @Autowired
    IdempotenceService idempotenceService

    def "should wire the MariaDB implementation for a MariaDB datasource"() {
        expect:
            actionRepository instanceof JdbcMariaDbActionRepository
            lockProvider instanceof JdbcMariaDbLockProvider
    }

    def "should execute actions against MariaDB"() {
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
