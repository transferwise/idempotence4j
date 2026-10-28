package com.transferwise.idempotence4j.autoconfigure

import spock.lang.Specification

import javax.sql.DataSource
import java.sql.Connection
import java.sql.DatabaseMetaData

class DatabaseTest extends Specification {

    def "should map database product name to implementation"() {
        expect:
            Database.fromProductName(productName) == database
        where:
            productName  || database
            "PostgreSQL" || Database.POSTGRES
            "MariaDB"    || Database.MARIADB
            "MySQL"      || Database.MARIADB
    }

    def "should fail for unsupported databases"() {
        when:
            Database.fromProductName("Oracle")
        then:
            def ex = thrown(IllegalStateException)
            ex.message.contains("Unsupported database 'Oracle'")
    }

    def "should detect database from datasource metadata"() {
        given:
            def metaData = Mock(DatabaseMetaData) {
                getDatabaseProductName() >> "PostgreSQL"
            }
            def connection = Mock(Connection) {
                getMetaData() >> metaData
            }
            def dataSource = Mock(DataSource) {
                getConnection() >> connection
            }
        expect:
            Database.of(dataSource) == Database.POSTGRES
    }
}
