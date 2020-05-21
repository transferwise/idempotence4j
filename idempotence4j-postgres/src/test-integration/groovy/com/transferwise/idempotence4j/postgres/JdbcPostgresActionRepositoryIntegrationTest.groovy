package com.transferwise.idempotence4j.postgres

import com.transferwise.idempotence4j.core.Action
import org.springframework.jdbc.core.JdbcTemplate
import spock.lang.Subject

import static com.transferwise.idempotence4j.factory.ActionTestFactory.anActionId
import static com.transferwise.idempotence4j.factory.ActionTestFactory.anActionResult

class JdbcPostgresActionRepositoryIntegrationTest extends IntegrationTest {
    def jdbcTemplate = new JdbcTemplate(dataSource)
    @Subject
    def repository = new JdbcPostgresActionRepository(jdbcTemplate)

    def "should successfully insert new action"() {
        given:
            def actionId = anActionId()
        when:
            repository.insertOrGet(new Action(actionId))
        then:
            repository.find(actionId).isPresent()
    }

    def "should look up existing action on ID conflict"() {
        given:
            def actionId = anActionId()
        when:
            repository.insertOrGet(new Action(actionId))
            def action = repository.find(actionId).get()
            def retryAction = repository.insertOrGet(new Action(actionId))
        then:
            retryAction.createdAt == action.createdAt
            countActions() == 1
    }

    def "should only update result values on update"() {
        given:
            def actionId = anActionId()
            def action = new Action(actionId)
        and:
            repository.insertOrGet(new Action(actionId))
        when:
            action.started()
            action.completed(anActionResult())
        and:
            repository.update(action)
        then:
            verifyAll(repository.find(actionId).get()) {
                lastRunAt.isPresent()
                completedAt.isPresent()
                hasResult()
            }
    }

    private countActions() {
        sql.firstRow('select count(*) as cnt from idempotent_action').cnt
    }
}
