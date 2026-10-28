package com.transferwise.idempotence4j.autoconfigure;

import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;

import javax.sql.DataSource;
import java.sql.DatabaseMetaData;

/**
 * Database of the application datasource, used to pick the matching implementation module
 */
enum Database {
    POSTGRES,
    MARIADB;

    static Database of(DataSource dataSource) {
        try {
            return fromProductName(JdbcUtils.extractDatabaseMetaData(dataSource, DatabaseMetaData::getDatabaseProductName));
        } catch (MetaDataAccessException ex) {
            throw new IllegalStateException("Failed to detect the database of the datasource", ex);
        }
    }

    static Database fromProductName(String productName) {
        if ("PostgreSQL".equalsIgnoreCase(productName)) {
            return POSTGRES;
        }
        // the MariaDB module relies on MySQL compatible syntax
        if ("MariaDB".equalsIgnoreCase(productName) || "MySQL".equalsIgnoreCase(productName)) {
            return MARIADB;
        }
        throw new IllegalStateException("Unsupported database '" + productName + "', idempotence4j supports PostgreSQL and MariaDB. "
            + "Define ActionRepository and LockProvider beans to use another implementation");
    }
}
