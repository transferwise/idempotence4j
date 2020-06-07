package com.transferwise.idempotence4j.autoconfigure

import com.transferwise.idempotence4j.autoconfigure.service.TestApplication
import com.transferwise.idempotence4j.autoconfigure.service.configuration.JsonConfiguration
import com.transferwise.idempotence4j.core.IdempotenceService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [TestApplication, JsonConfiguration, Idempotence4jAutoConfiguration])
class Idempotence4jAutoConfigurationTest extends Specification {
    @Autowired(required = false)
    IdempotenceService idempotenceService

    def "should autowire idempotence service"() {
        expect:
            idempotenceService
    }
}
