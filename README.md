# idempotence4j

idempotence4j is a lightweight library that provides support for managing idempotence actions.

All actions are persisted to a database and it's expected that client's only going to use database master node
 to avoid any possible issues caused by replication lag.

## Overview

idempotence4j provides several modules:

- **idempotence4j-core** - exposes main API and defines action execution strategy
- **idempotence4j-postgres** - `PostgreSQL` integration, defines flyway migrations, contains implementation for action repository, locking
- **idempotence4j-spring-boot-starter** - auto configuration for `IdempotenceService` with automatic discovery of DB implementation module


## Usage

When invoking idempotence service clients are expected to provide:

- `actionId` - unique identifier for the current action
- `onRetry` - function that maps persisted result of previously successful action to client result
- `procedure` - supplier that executes the action processing logic and returns result to client
- `toRecord` - function that maps client result to the one being persisted in the database

```java

@AllArgsConstructor
public class ApplicationService {
    private final IdempotenceService idempotenceService;
    private final ResultRepository repository;

    public Result execute(ActionId actionId, String args[]) {
        Result result = idempotenceService.execute(actionId, this::byId, () -> {
            //execution logic
            return new Result();
        }, Result::getId);
        return result;
    }

    private Result byId(ResultId resultId) {
        return repository.get(resultId);
    }
}

```

## Adding idempotence4j to your build

idempotence4j's Maven group ID is `com.transferwise.idempotence4j`, and its artifact ID is `idempotence4j`

To add a dependency on **idempotence4j** using Gradle, use the following:

```gradle
dependencies {
  // Pick one:

  // 1. Use idempotence4j in your implementation only:
  implementation("com.transferwise.idempotence4j:idempotence4j-core:1.0.0")

  // 2. Use idempotence4j types in your public API:
  api("com.transferwise:idempotence4j-core:1.0.0")
}
```

For more information on when to use `api` and when to use `implementation`,
consult the
[Gradle documentation on API and implementation separation](https://docs.gradle.org/current/userguide/java_library_plugin.html#sec:java_library_separation).

## Database modules

idempotence4j comes with a set of database specific implementations.
For now there is only support for `PostgresSQL` but `MariaDB` support is next on the roadmap

### PostgresSQL Module

To add PostgresSQL module to your build using Gradle, use the following:

```gradle
dependencies {
  implementation("com.transferwise.idempotence4j:idempotence4j-postgres:1.0.0")
}
```

### Flyway

`Postgres` module contains Flyway migration definitions to keep requied tables schemas up-to-date

```java

@Bean
public Flyway getFlyway(dataSource) {
    def configuration = new FluentConfiguration()
        .dataSource(dataSource)
        .locations("filesystem:src/main/resources/db/idempotence4j/postgres")

    return new Flyway(configuration)
}

```

## Spring Boot

Spring boot starter module provides auto-discovery for known implementation modules
and configures `Beans` autowiring

To add `spring-boot-starter` module to your build using Gradle, use the following:

```gradle
dependencies {
  implementation("com.transferwise.idempotence4j:idempotence4j-spring-boot-starter:1.0.0")
}
```
