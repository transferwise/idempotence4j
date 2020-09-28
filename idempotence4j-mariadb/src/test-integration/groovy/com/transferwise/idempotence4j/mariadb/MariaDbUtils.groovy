package com.transferwise.idempotence4j.mariadb

import groovy.sql.Sql

class MariaDbUtils {
    static truncateAllTables(Sql sql) {
        sql.eachRow("""
            SELECT table_name
            FROM information_schema.tables
            WHERE table_schema='idempotence4j_db'
            AND table_name NOT IN ('flyway_schema_history')
            """) { row ->
            sql.executeUpdate('TRUNCATE TABLE ' + row['table_name'])
        }
    }
}
