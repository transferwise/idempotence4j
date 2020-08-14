package com.transferwise.idempotence4j.postgres

import com.transferwise.idempotence4j.core.Action
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import spock.lang.Subject

import static com.transferwise.idempotence4j.factory.ActionTestFactory.anActionId

class JdbcPostgresLockProviderIntegrationTest extends IntegrationTest {
    def jdbcTemplate = new JdbcTemplate(dataSource)
    def transactionTemplate = new TransactionTemplate(new DataSourceTransactionManager(dataSource))
    def repository = new JdbcPostgresActionRepository(jdbcTemplate)
    @Subject
    def lockProvider = new JdbcPostgresLockProvider(jdbcTemplate)

    def "should successfully acquire lock and retrieve locked action"() {
        given:
            def actionId = anActionId()
        and:
            repository.insertOrGet(new Action(actionId))
        when:
            def lock = transactionTemplate.execute({ lockProvider.lock(actionId) } )
        then:
            lock.isPresent()
            lock.get().getLockedAction().actionId == actionId
    }
}
