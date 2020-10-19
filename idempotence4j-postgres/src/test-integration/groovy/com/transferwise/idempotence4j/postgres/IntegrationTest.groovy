package com.transferwise.idempotence4j.postgres

import com.transferwise.idempotence4j.utils.DbUtils
import com.transferwise.idempotence4j.utils.PropertiesLoader
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import groovy.sql.Sql
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.configuration.FluentConfiguration
import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

class IntegrationTest extends Specification {
    @Shared
    def dataSource = getDataSource(PropertiesLoader.loadProperties('datasource.properties'))
    @Shared
    def flyway = getFlyway(dataSource)
    @Shared
    def sql = new Sql(dataSource)

    @Delegate
    PollingConditions await = new PollingConditions(delay: 0.5)

    def setup() {
        DbUtils.truncateAllTables(sql)
        flyway.repair()
        flyway.migrate()
    }

    def getDataSource(Properties properties) {
        def config = new HikariConfig()
        def jdbcUrl = System.getProperty('datasource.url') ?: properties.getProperty('datasource.url')
        config.setJdbcUrl(jdbcUrl)
        config.setUsername(properties.getProperty('datasource.username'))
        config.setPassword(properties.getProperty('datasource.password'))
        config.setDriverClassName('org.postgresql.Driver')

        new HikariDataSource(config)
    }

    def getFlyway(dataSource) {
        def configuration = new FluentConfiguration()
            .dataSource(dataSource)
            .locations("filesystem:src/main/resources/db/idempotence4j/postgres")

        new Flyway(configuration)
    }
}


