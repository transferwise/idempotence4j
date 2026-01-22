# ADR-0001: Use Low-Level JDBC for Lock Conflict Detection

## Status

Accepted

## Context

The `JdbcMariaDbLockProvider` needs to detect when a row lock cannot be acquired (MariaDB error 1205 - lock wait timeout) and return `Optional.empty()` instead of throwing an exception.

### The Problem with Spring's Exception Translation

Spring Framework 6.0 changed its default SQL exception translator from `SQLErrorCodeSQLExceptionTranslator` to `SQLExceptionSubclassTranslator`. The new translator uses JDBC 4 exception subclasses and SQL state codes instead of vendor-specific error codes.

This change caused MariaDB error 1205 to be translated to `UncategorizedSQLException` instead of `CannotAcquireLockException`, breaking lock conflict detection.

### Previous Solution

We initially fixed this by explicitly configuring `SQLErrorCodeSQLExceptionTranslator` on the `JdbcTemplate`:

```java
JdbcTemplate localJdbcTemplate = new JdbcTemplate(dataSource);
localJdbcTemplate.setExceptionTranslator(new SQLErrorCodeSQLExceptionTranslator(dataSource));
```

This worked but coupled us to Spring's internal exception mapping behavior.

### Alternative Approaches

1. **Keep using Spring's exception translator** - Requires configuration and couples to Spring internals
2. **Use low-level JDBC with direct error code checking** - Used by payin-service and tw-tasks at Wise
3. **Catch base exception classes** - Less specific, may catch unintended errors

## Decision

Use low-level JDBC with direct error code checking, similar to the approach used in:
- `payin-service/core/src/main/java/com/transferwise/payin/internal/persistence/BaseJdbcRepository.java`
- tw-tasks

```java
private static final int LOCK_WAIT_TIMEOUT_ERROR = 1205;

try (PreparedStatement ps = connection.prepareStatement(LOCK_SQL)) {
    // ...
} catch (SQLException e) {
    if (e.getErrorCode() == LOCK_WAIT_TIMEOUT_ERROR) {
        return Optional.empty();
    }
    throw new RuntimeException("Failed to acquire lock", e);
}
```

## Consequences

### Positive

- **Reduced Spring coupling** - No dependency on Spring's internal exception mapping
- **Explicit error handling** - Error code 1205 is documented directly in the code
- **Future-proof** - Won't break if Spring changes exception translation again
- **Simpler constructor** - Takes `DataSource` directly instead of `JdbcTemplate`
- **Consistent with Wise patterns** - Aligns with payin-service and tw-tasks approaches

### Negative

- **Breaking API change** - Constructor now takes `DataSource` instead of `JdbcTemplate`
- **Database-specific code** - Error code 1205 is MariaDB/MySQL specific (but this is already a MariaDB-specific module)

### Neutral

- **Similar code complexity** - About the same lines of code as the previous solution

## References

- [Spring Framework 6.0 Release Notes - Data Access](https://github.com/spring-projects/spring-framework/wiki/Spring-Framework-6.0-Release-Notes#data-access-and-transactions)
- [MariaDB Error Codes](https://mariadb.com/kb/en/mariadb-error-codes/)
- payin-service BaseJdbcRepository pattern
