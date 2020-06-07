package com.transferwise.idempotence4j.autoconfigure.service.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JsonConfiguration {

    @Bean
    ObjectMapper objectMapper() {
        new ObjectMapper()
    }
}
