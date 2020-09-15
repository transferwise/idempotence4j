package com.transferwise.idempotence4j.postgres

import com.transferwise.idempotence4j.core.Action
import com.transferwise.idempotence4j.core.ClockKeeper
import org.springframework.jdbc.core.JdbcTemplate
import spock.lang.Subject

import java.time.Clock
import java.time.Instant

import static com.transferwise.idempotence4j.factory.ActionTestFactory.anAction
import static com.transferwise.idempotence4j.factory.ActionTestFactory.anActionId
import static com.transferwise.idempotence4j.factory.ActionTestFactory.anActionResult
import static java.time.ZoneOffset.UTC
import static java.util.concurrent.ThreadLocalRandom.current

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

    def "should remove old tasks regardless of the status in batches"() {
        given:
            def actions = []
            def purged = []
        and:
            ClockKeeper.set(Clock.fixed(Instant.parse("2019-11-11T00:00:00Z"), UTC))
            10.times {
                def action = anAction(isCompleted: current().nextBoolean())
                actions << action
                purged << action
            }
        and:
            ClockKeeper.set(Clock.systemUTC())
            10.times {
                actions << anAction()
            }
        and:
            actions.each { it -> repository.insertOrGet(it) }
        when:
            repository.deleteOlderThan(Instant.parse("2019-12-11T00:00:00Z"), 5)
        then:
            countActions() == 15
        when:
            repository.deleteOlderThan(Instant.parse("2019-12-11T00:00:00Z"), 5)
        then:
            countActions() == 10
        and:
            purged.each { Action it ->
                assert repository.find(it.actionId).isEmpty()
            }
    }

    private countActions() {
        sql.firstRow('select count(*) as cnt from idempotent_action').cnt
    }
}
