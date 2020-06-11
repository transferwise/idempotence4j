# idempotence4j

idempotence4j is a lightweight library that provides support for managing idempotence actions.

All actions are persisted to a database and it's expected that client's only going to use database master node
 to avoid any possible issues caused by the replication lag.

## Overview

idempotence4j provides several modules:

- **idempotence4j-core** - exposes main API and defines action execution strategy
- **idempotence4j-postgres** - `PostgreSQL` integration, defines flyway migrations, contains implementation for action repository, locking
- **idempotence4j-metrics** - publishes metrics to `io.micrometer` registry
- **idempotence4j-spring-boot-starter** - auto configuration for `IdempotenceService` with automatic discovery of DB implementation module


## Usage

When invoking idempotence service clients are expected to provide:

- `actionId` - unique identifier for the current action
- `onRetry` - function that maps persisted result of previously successful action to client result
- `procedure` - supplier that executes the action processing logic and returns result to client
- `toRecord` - function that maps client result to the one being persisted in the database
- `recordType` - type of the stored record required for a later de-serialisation

```java

@AllArgsConstructor
public class ApplicationService {
    private final IdempotenceService idempotenceService;
    private final ResultRepository repository;

    public Result execute(ActionId actionId, String args[]) {
        Result result = idempotenceService.execute(actionId, this::byId, () -> {
            //execution logic
            return new Result();
        }, Result::getId, new TypeReference<ResultId>(){});
        return result;
    }

    private Result byId(ResultId resultId) {
        return repository.get(resultId);
    }
}

```

## Adding idempotence4j to your build

idempotence4j's Maven group ID is `com.transferwise.idempotence4j`, and its artifact ID is `idempotence4j`

If you're using **Spring Boot** here is a quick way of to add a dependency on **idempotence4j**

```gradle
dependencies {
  implementation("com.transferwise.idempotence4j:idempotence4j-spring-boot-starter:1.1.0")
  implementation("com.transferwise.idempotence4j:idempotence4j-postgres:1.1.0")
  implementation("com.transferwise.idempotence4j:idempotence4j-metrics:1.1.0")
}
```
Spring boot starter provides auto-discovery for known implementation modules
and autowires service `Beans`. Otherwise, you can add a dependency on each module individually with core module defined as:

```gradle
dependencies {
  implementation("com.transferwise.idempotence4j:idempotence4j-core:1.1.0")
}
```

## Database modules

idempotence4j comes with a set of database-specific implementations.
For now, the library supports `PostgresSQL` only, `MariaDB` is the next on the roadmap.

### PostgresSQL Module

To add PostgresSQL module to your build using Gradle, use the following:

```gradle
dependencies {
  implementation("com.transferwise.idempotence4j:idempotence4j-postgres:1.1.0")
}
```

### Flyway

`Postgres` module contains Flyway migration definitions to keep required tables schemas up-to-date. Both `yaml` and `java` configuration examples provided below:

```yaml

  flyway:
    table: flyway_schema
    password: ${DATASOURCE_FLYWAY_PASSWORD}
    user: ${DATASOURCE_FLYWAY_USERNAME}
    url: ${DATASOURCE_URL}
    locations: classpath:db/migration, classpath:db/idempotence4j/postgres

```


```java

@Bean
public Flyway getFlyway(dataSource) {
    var configuration = new FluentConfiguration()
        .dataSource(dataSource)
        .locations("classpath:db/migration, classpath:db/idempotence4j/postgres")

    return new Flyway(configuration)
}

```
