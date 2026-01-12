# Changelog

All notable changes to this project will be documented in this file, in reverse chronological order by release.

## 2.0.0 - 2026-01-12

### Breaking Changes
- Java 11 → 17
- Spring Boot 2.2.4 → 3.5.7
- Spring Framework 5.2.3 → 6.2.12
- Jakarta EE namespace migration (javax.* → jakarta.*)

### Fixed
- **MariaDB lock conflict detection on Spring 6** - Spring 6 changed the default SQL exception translator, causing MariaDB error 1205 (lock wait timeout) to be translated to `UncategorizedSQLException` instead of `CannotAcquireLockException`. This broke lock conflict detection in `JdbcMariaDbLockProvider`. Fixed by explicitly using `SQLErrorCodeSQLExceptionTranslator`.

### Changed
- Gradle 5.6 → 8.14.3
- Groovy 2.5 → 4.x
- Spock 1.3 → 2.4-M5

## 1.7.2 - 2022-06-08
bump db-scheduler version to 11.0

## 1.7.1 - 2022-05-03
bump cron-utils to fix avd.aquasec.com/nvd/cve-2021-41269
bump jackson-datatype to fix https://cve.mitre.org/cgi-bin/cvename.cgi?name=cve-2018-1000873

## 1.7.0 - 2022-04-19
- `deleteOlderThan` method on `ActionRepository` now forces use of a primitive for batch size.
- Begin returning the number of rows deleted from deletion methods in `ActionRepository`
- Exposed a `deleteByTypeAndClient` method on `ActionRepository`
- Deletion of old action IDs is now more performant on MariaDB

## 1.6.0 - 2022-04-08

expose ActionRepository deleteByIds method

## 1.4.0 - 2020-09-25

MariaDb integration

## 1.3.0 - 2020-09-15

Action retention policy

## 1.0.0 - 2020-05-19

Initial release.
