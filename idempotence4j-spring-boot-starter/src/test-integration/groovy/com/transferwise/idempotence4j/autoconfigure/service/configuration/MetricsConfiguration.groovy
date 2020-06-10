package com.transferwise.idempotence4j.autoconfigure.service.configuration

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MetricsConfiguration {

    @Bean
    MeterRegistry meterRegistry() {
        new SimpleMeterRegistry()
    }
}
