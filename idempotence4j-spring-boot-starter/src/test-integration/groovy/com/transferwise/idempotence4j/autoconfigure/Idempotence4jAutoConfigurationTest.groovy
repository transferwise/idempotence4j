package com.transferwise.idempotence4j.autoconfigure

import com.transferwise.idempotence4j.autoconfigure.service.TestApplication
import com.transferwise.idempotence4j.autoconfigure.service.configuration.JsonConfiguration
import com.transferwise.idempotence4j.core.DefaultIdempotenceService
import com.transferwise.idempotence4j.core.IdempotenceService
import com.transferwise.idempotence4j.core.retention.RetentionService
import com.transferwise.idempotence4j.metrics.micrometer.MicrometerMetricsPublisher
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [TestApplication, JsonConfiguration, Idempotence4jAutoConfiguration])
class Idempotence4jAutoConfigurationTest extends Specification {
    @Autowired(required = false)
    IdempotenceService idempotenceService
    @Autowired(required = false)
    RetentionService retentionService

    def "should autowire idempotence service"() {
        expect:
            idempotenceService
            idempotenceService instanceof DefaultIdempotenceService
            idempotenceService.metricsPublisher instanceof MicrometerMetricsPublisher
    }

    def "should autowire retention service"() {
        expect:
            retentionService
            retentionService.scheduler.schedulerState.isStarted()
    }
}
