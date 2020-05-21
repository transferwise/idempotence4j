package com.transferwise.idempotence4j.utils

import groovy.sql.Sql

class DbUtils {
    static truncateAllTables(Sql sql) {
        sql.eachRow("""
            SELECT table_name
            FROM information_schema.tables
            WHERE table_schema='public'
            AND table_name NOT IN ('flyway_schema_history')
            """) { row ->
            sql.executeUpdate('TRUNCATE TABLE "' + row['table_name'] + '"')
        }
    }
}
