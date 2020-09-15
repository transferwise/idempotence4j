package com.transferwise.idempotence4j.core.retention

import com.transferwise.idempotence4j.core.ActionRepository
import com.transferwise.idempotence4j.core.ClockKeeper
import spock.lang.Specification
import spock.lang.Subject

import java.time.Clock
import java.time.Instant
import java.time.Period

import static java.time.ZoneOffset.UTC

class PurgeJobTest extends Specification {
    def repository = Mock(ActionRepository)
    def configuration = new RetentionPolicy.PurgeJobConfiguration("0 1 1 * * ?", 10)
    def retentionPeriod = Period.of(0,0,10)

    @Subject
    def purgeJob = new PurgeJob(repository, configuration, retentionPeriod)

    def "should submit correctly calculate deletion cutoff date and submit removal request"() {
        setup:
            ClockKeeper.set(Clock.fixed(Instant.parse("2019-11-20T12:00:00Z"), UTC))
        when:
            purgeJob.execute(null, null)
        then:
            1 * repository.deleteOlderThan(Instant.parse("2019-11-10T00:00:00Z"), configuration.getBatchSize())
        cleanup:
            ClockKeeper.set(Clock.systemDefaultZone())
    }
}
