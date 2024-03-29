package com.transferwise.idempotence4j.mariadb

import com.transferwise.idempotence4j.core.Action
import com.transferwise.idempotence4j.core.ActionId
import com.transferwise.idempotence4j.core.ClockKeeper
import org.springframework.jdbc.core.JdbcTemplate
import spock.lang.Subject

import java.time.Clock
import java.time.Instant

import static com.transferwise.idempotence4j.factory.ActionTestFactory.CLIENT
import static com.transferwise.idempotence4j.factory.ActionTestFactory.TYPE
import static com.transferwise.idempotence4j.factory.ActionTestFactory.anAction
import static com.transferwise.idempotence4j.factory.ActionTestFactory.anActionId
import static com.transferwise.idempotence4j.factory.ActionTestFactory.anActionResult
import static java.time.ZoneOffset.UTC
import static java.util.concurrent.ThreadLocalRandom.current

class JdbcMariaDbActionRepositoryIntegrationTest extends IntegrationTest {
    def jdbcTemplate = new JdbcTemplate(dataSource)
    @Subject
    def repository = new JdbcMariaDbActionRepository(jdbcTemplate)

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
            int firstDeletionCount = repository.deleteOlderThan(Instant.parse("2019-12-11T00:00:00Z"), 5)
        then:
            firstDeletionCount == 5
            countActions() == 15
        when:
            int secondDeletionCount = repository.deleteOlderThan(Instant.parse("2019-12-11T00:00:00Z"), 5)
        then:
            secondDeletionCount == 5
            countActions() == 10
        and:
            purged.each { Action it ->
                assert repository.find(it.actionId).isEmpty()
            }
    }

    def "should remove actions by type and client"() {
        given:
            def actions = []
            10.times {
                actions << anAction()
            }
        and:
            List<ActionId> actionIds = actions.each { it -> repository.insertOrGet(it) } .collect({ it.actionId })
            def firstHalfActionIds = actionIds.dropRight(actions.size() / 2 as int)
            def lastHalfActionIds = actionIds.drop(actions.size() / 2 as int)
        when:
            int firstDeletionCount = repository.deleteByTypeAndClient(TYPE, CLIENT, 5)
        then:
            firstDeletionCount == 5
            firstHalfActionIds.each { ActionId it -> assert repository.find(it).isEmpty() }
            lastHalfActionIds.each { ActionId it -> assert !repository.find(it).isEmpty() }
        when:
            int secondDeletionCount = repository.deleteByTypeAndClient(TYPE, CLIENT, 5)
        then:
            secondDeletionCount == 5
            actionIds.each { ActionId it ->
                assert repository.find(it).isEmpty()
            }
    }

    def "should remove actions by id"() {
        given:
            def actions = []
            10.times {
                actions << anAction()
            }
        and:
            List<ActionId> actionIds = actions.each { it -> repository.insertOrGet(it) } .collect({ it.actionId })
            def firstHalfActionIds = actionIds.dropRight(actions.size() / 2 as int)
            def lastHalfActionIds = actionIds.drop(actions.size() / 2 as int)
        when:
            int[] firstRowsDeleted = repository.deleteByIds(firstHalfActionIds)
            firstRowsDeleted == [1, 1, 1, 1, 1] as int[]
        then:
            firstHalfActionIds.each { ActionId it -> assert repository.find(it).isEmpty() }
            lastHalfActionIds.each { ActionId it -> assert !repository.find(it).isEmpty() }
        when:
            int[] secondRowsDeleted = repository.deleteByIds(lastHalfActionIds)
            secondRowsDeleted == [1, 1, 1, 1, 1] as int[]
        then:
            actionIds.each { ActionId it ->
                assert repository.find(it).isEmpty()
            }
    }

    private countActions() {
        sql.firstRow('select count(*) as cnt from idempotent_action').cnt
    }
}
