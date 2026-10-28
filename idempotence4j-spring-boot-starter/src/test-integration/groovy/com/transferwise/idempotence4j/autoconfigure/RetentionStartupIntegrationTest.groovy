package com.transferwise.idempotence4j.autoconfigure

import com.transferwise.idempotence4j.autoconfigure.service.TestApplication
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import spock.lang.Specification

import javax.sql.DataSource

@SpringBootTest(classes = TestApplication)
class RetentionStartupIntegrationTest extends Specification {
    // a schema of its own, so the context always starts on a database without the idempotence4j tables
    static final String SCHEMA = "retention_" + UUID.randomUUID().toString().replace("-", "")

    @DynamicPropertySource
    static void freshSchema(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.hikari.schema", () -> SCHEMA)
        registry.add("spring.flyway.default-schema", () -> SCHEMA)
    }

    @Autowired
    DataSource dataSource

    def "should register the purge job when the migrations run on application start"() {
        expect:
            new JdbcTemplate(dataSource).queryForList("SELECT task_name FROM idempotence4j_scheduled_tasks", String) == ["idempotent-action-purge-job"]
    }
}
