package com.transferwise.idempotence4j.core.retention

import com.transferwise.idempotence4j.core.ActionRepository
import com.transferwise.idempotence4j.core.ClockKeeper
import spock.lang.Specification

import java.time.Clock
import java.time.Instant

import static java.time.ZoneOffset.UTC

class PurgeJobTest extends Specification {
    def repository = Mock(ActionRepository)
    def configuration = new RetentionPolicy.PurgeJobConfiguration("0 1 1 * * ?", 10)

    def cleanup() {
        ClockKeeper.set(Clock.systemDefaultZone())
    }

    def "should submit correctly calculate deletion cutoff date and submit removal request"() {
        setup:
            def policy = new RetentionPolicy("P0Y0M10D", null, configuration)
            def purgeJob = new PurgeJob(repository, policy)
            ClockKeeper.set(Clock.fixed(Instant.parse("2019-11-20T12:00:00Z"), UTC))
        when:
            purgeJob.execute(null, null)
        then:
            1 * repository.deleteOlderThan(Instant.parse("2019-11-10T12:00:00Z"), configuration.getBatchSize())
    }

    def "should submit correctly calculate deletion cutoff date and submit removal request with time"() {
        setup:
            def policy = new RetentionPolicy(null, "PT0H15M", configuration)
            def purgeJob = new PurgeJob(repository, policy)
            ClockKeeper.set(Clock.fixed(Instant.parse("2019-11-20T12:00:00Z"), UTC))
        when:
            purgeJob.execute(null, null)
        then:
            1 * repository.deleteOlderThan(Instant.parse("2019-11-20T11:45:00Z"), configuration.getBatchSize())
    }
}
