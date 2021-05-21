package com.transferwise.idempotence4j.core.retention

import spock.lang.Specification
import com.transferwise.idempotence4j.core.retention.RetentionPolicy.PurgeJobConfiguration

class RetentionPolicyTest extends Specification {

    static configuration = new PurgeJobConfiguration("0 1 1 * * ?", 10)
    def "should accept a valid configuration"() {
        when:
            new RetentionPolicy("P0Y0M2D", null, configuration)
        then:
            noExceptionThrown()
        when:
            new RetentionPolicy(null, "PT0H15M0S", configuration)
        then:
            noExceptionThrown()
    }

    def "should only allow positive retention period"() {
        when:
            new RetentionPolicy("P-1Y0M0D", null, configuration)
        then:
            thrown(IllegalArgumentException)
        when:
            new RetentionPolicy(null, "PT-1H0M0S", configuration)
        then:
            thrown(IllegalArgumentException)
    }

    def "should only allow valid purge job cron schedule"() {
        when:
            new RetentionPolicy("P0Y0M0D", null, configuration)
        then:
            thrown(IllegalArgumentException)
        when:
            new RetentionPolicy(null, "PT0H0M0S", configuration)
        then:
            thrown(IllegalArgumentException)
    }
}
