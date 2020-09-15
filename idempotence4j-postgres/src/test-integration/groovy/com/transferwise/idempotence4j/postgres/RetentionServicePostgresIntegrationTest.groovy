package com.transferwise.idempotence4j.postgres

import com.transferwise.idempotence4j.core.ClockKeeper
import com.transferwise.idempotence4j.core.retention.RetentionPolicy
import com.transferwise.idempotence4j.core.retention.RetentionPolicy.PurgeJobConfiguration
import com.transferwise.idempotence4j.core.retention.RetentionService
import org.springframework.jdbc.core.JdbcTemplate
import spock.lang.Subject

import java.time.Clock
import java.time.Instant
import java.time.Period

import static com.transferwise.idempotence4j.factory.ActionTestFactory.anAction
import static java.time.ZoneOffset.UTC

class RetentionServicePostgresIntegrationTest extends IntegrationTest {
    def repository = new JdbcPostgresActionRepository(new JdbcTemplate(dataSource))
    def retentionPolicy = new RetentionPolicy(
        Period.of(0, 0, 2),
        new PurgeJobConfiguration("*/5 * * * * ?", 10)
    )

    @Subject
    def service = new RetentionService(dataSource, repository, retentionPolicy)

    def "should schedule and execute purge job according to a retention policy"() {
        given:
            ClockKeeper.set(Clock.fixed(Instant.parse("2019-11-11T00:00:00Z"), UTC))
            10.times {
                repository.insertOrGet(anAction())
            }
            ClockKeeper.set(Clock.systemUTC())
        when:
            service.initialize()
        then:
            await.within(20) {
                countActions() == 0
            }
        cleanup:
            service.shutdown()
    }

    private countActions() {
        sql.firstRow('select count(*) as cnt from idempotent_action').cnt
    }
}
