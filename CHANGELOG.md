# Changelog

All notable changes to this project will be documented in this file, in reverse chronological order by release.

## 2.1.0 - 2026-10-28

### Upgrade notes
- **Database migration required before deploying:** `V20261028120000__add_idempotent_action_fingerprint.sql` adds a nullable `fingerprint` column to `idempotent_action`.
  It is a metadata-only change on PostgreSQL 11+ and MariaDB 10.3+. If you keep copies of the migrations in your project, copy this one too.

### Added
- Optional payload fingerprint: `IdempotenceService.execute(actionId, fingerprint, ...)` overloads store a digest of the request with the action
  and throw `ActionPayloadMismatchException` when the same action id is submitted again with a different fingerprint
- `MISMATCH` metrics outcome
- `EphemeralIdempotenceService` supports fingerprints

### Changed
- `IdempotenceService` fingerprint methods are default methods throwing `UnsupportedOperationException`, so existing custom implementations keep compiling

## 2.0.0 - 2026-01-12

### Breaking Changes
- Java 11 → 17
- Spring Boot 2.2.4 → 3.5.7 (also tested with 4.0.1)
- Spring Framework 5.2.3 → 6.x (managed by Spring Boot BOM)
- Jakarta EE namespace migration (javax.* → jakarta.*)
- `JdbcMariaDbLockProvider` constructor now takes `DataSource` instead of `JdbcTemplate`

### Fixed
- **MariaDB lock conflict detection on Spring 6** - Spring 6 changed the default SQL exception translator, breaking lock conflict detection. Fixed by using low-level JDBC with direct error code checking (error 1205) instead of relying on Spring's exception translation. See `docs/adr/0001-low-level-jdbc-for-lock-detection.md`.

### Added
- Spring Boot 4.x compatibility
- CI matrix testing for Spring Boot 3.5.7 and 4.0.1
- Auto-configuration registration via `AutoConfiguration.imports` (Spring Boot 3+ style)

### Changed
- Gradle 5.6 → 8.14.3
- Groovy 2.5 → 4.x
- Spock 1.3 → 2.4-M5
- Spring JDBC and JUnit Platform versions now managed by Spring Boot BOM

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
