package com.transferwise.idempotence4j.autoconfigure.service.configuration

import com.transferwise.idempotence4j.core.ActionRepository
import com.transferwise.idempotence4j.core.LockProvider
import com.transferwise.idempotence4j.postgres.JdbcPostgresActionRepository
import com.transferwise.idempotence4j.postgres.JdbcPostgresLockProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate

import javax.sql.DataSource

// the starter may wire the MariaDB implementations on a Postgres datasource (#64), declared explicitly until that is fixed
@Configuration
class RepositoryConfiguration {

    @Bean
    ActionRepository actionRepository(DataSource dataSource) {
        new JdbcPostgresActionRepository(new JdbcTemplate(dataSource))
    }

    @Bean
    LockProvider lockProvider(DataSource dataSource) {
        new JdbcPostgresLockProvider(new JdbcTemplate(dataSource))
    }
}
