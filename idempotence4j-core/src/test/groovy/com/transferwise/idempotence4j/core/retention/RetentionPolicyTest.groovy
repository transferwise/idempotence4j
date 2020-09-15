package com.transferwise.idempotence4j.core.retention

import spock.lang.Specification
import com.transferwise.idempotence4j.core.retention.RetentionPolicy.PurgeJobConfiguration

import java.time.Period

class RetentionPolicyTest extends Specification {

    def "should accept a valid configuration"() {
        when:
            new RetentionPolicy(
                Period.of(0, 0, 2),
                new PurgeJobConfiguration("0 1 1 * * ?", 10))
        then:
            noExceptionThrown()
    }

    def "should only allow positive retention period"() {
        when:
            new RetentionPolicy(
                Period.of(-1, 2, 0),
                new PurgeJobConfiguration("0 1 1 * * ?", 10))
        then:
            thrown(IllegalArgumentException)
    }

    def "should only allow valid purge job cron schedule"() {
        when:
            new RetentionPolicy(
                Period.of(0, 0, 2),
                new PurgeJobConfiguration("0 1 1", 10))
        then:
            thrown(IllegalArgumentException)
    }
}
