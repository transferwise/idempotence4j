<a href="https://img.shields.io/badge/release-2.0.0-orange">
        <img src="https://img.shields.io/badge/release-2.0.0-orange"
            alt="Release version"/></a>

# idempotence4j

idempotence4j is a lightweight library that provides support for handling idempotent actions.

All actions are persisted to a database and it's expected that client's only going to use database master node
 to avoid any possible issues caused by the replication lag.

## Requirements

- Java 17+

## Overview

idempotence4j provides several modules:

- **idempotence4j-core** - exposes main API and defines action execution strategy
- **idempotence4j-postgres** - `PostgreSQL` integration, defines flyway migrations, contains PostgreSQL implementation for action repository and locking
- **idempotence4j-mariadb** - `MariaDb` integration, defines flyway migrations, contains MariaDb implementation for action repository and locking
- **idempotence4j-metrics** - publishes metrics to `io.micrometer` registry
- **idempotence4j-spring-boot-starter** - auto configuration for `IdempotenceService` with automatic discovery of DB implementation module


## Usage

When invoking idempotence service clients are expected to provide:

- `actionId` - unique identifier for the current action, made of a `key`, an action `type` and a `client`
- `onRetry` - function that maps persisted result of previously successful action to client result
- `procedure` - supplier that executes the action processing logic and returns result to client
- `toRecord` - function that maps client result to the one being persisted in the database
- `recordType` - type of the stored record required for a later de-serialisation

Two types are involved: the result returned to the caller (`R`) and the record persisted with the action (`S`).
In the example below the caller gets a `Transfer` back, while only its `TransferId` is persisted.
`Transfer`, `TransferId` and `TransferRepository` are application classes, not part of idempotence4j.

```java
import com.fasterxml.jackson.core.type.TypeReference;
import com.transferwise.idempotence4j.core.ActionId;
import com.transferwise.idempotence4j.core.IdempotenceService;

@RequiredArgsConstructor
public class TransferService {
    private final IdempotenceService idempotenceService;
    private final TransferRepository transferRepository;

    public Transfer create(UUID requestId, String clientId, CreateTransferCommand command) {
        ActionId actionId = new ActionId(requestId, "CREATE_TRANSFER", clientId);

        return idempotenceService.execute(
            actionId,
            transferRepository::get,                                 // onRetry: TransferId -> Transfer
            () -> transferRepository.save(Transfer.create(command)), // procedure
            Transfer::getId,                                         // toRecord: Transfer -> TransferId
            new TypeReference<TransferId>() {}                       // recordType
        );
    }
}
```

On the first call the procedure runs and the `TransferId` is stored. A retry with the same `ActionId` skips the procedure
and calls `onRetry` with the stored `TransferId`, so the caller gets the same transfer back.

When the result itself can be persisted, there is a shorter overload:

```java
Transfer transfer = idempotenceService.execute(actionId, () -> createTransfer(command), new TypeReference<Transfer>() {});
```

### Transactions and concurrent requests

- The procedure runs in a transaction together with the update of the action record. If it throws, both are rolled back and the action can be retried.
- If the caller already has a transaction, the procedure joins it.
- While an action is in progress, another request with the same `ActionId` fails fast with `ConflictingActionException`.
  It does not wait for the first request, so map it to a retryable response, for example HTTP `409 Conflict`.

## Adding idempotence4j to your build

idempotence4j's Maven group ID is `com.transferwise.idempotence4j`, and its artifact ID is `idempotence4j`

If you're using **Spring Boot** here is a quick way of to add a dependency on **idempotence4j**

```gradle
dependencies {
  implementation "com.transferwise.idempotence4j:idempotence4j-spring-boot-starter:${project['idempotence4j.version']}"
  implementation "com.transferwise.idempotence4j:idempotence4j-postgres:${project['idempotence4j.version']}"
  implementation "com.transferwise.idempotence4j:idempotence4j-metrics:${project['idempotence4j.version']}"
}
```
Spring boot starter provides auto-discovery for known implementation modules
and autowires service `Beans`. Otherwise, you can add a dependency on each module individually with core module defined as:

```gradle
dependencies {
  implementation "com.transferwise.idempotence4j:idempotence4j-core:${project['idempotence4j.version']}"
}
```

## Database modules

idempotence4j comes with a set of database-specific implementations.
For now, the library supports only `PostgresSQL` and `MariaDB`.

### PostgresSQL Module

To add PostgresSQL module to your build using Gradle, use the following:

```gradle
dependencies {
  implementation "com.transferwise.idempotence4j:idempotence4j-postgres:${project['idempotence4j.version']}"
}
```

### MariaDb Module

To add MariaDb module to your build using Gradle, use the following:

```gradle
dependencies {
  implementation "com.transferwise.idempotence4j:idempotence4j-mariadb:${project['idempotence4j.version']}"
}
```

### Flyway

> :exclamation: **_Important:_**  Flyway by default doesn't allow to apply "out of order" migrations, that means
> if you have already applied a migration with version `3`, adding migration version `1` will cause an error.
>
> Since `idempotence4j` is using a timestamp versions it can cause a number of issues, i.e. if in your project you use incremented numeric versions.

Please **only use the following configuration approach** if your flyway configuration has `flyway.outOfOrder` flag enabled, otherwise please create an exact copy of these migrations in your project flyway module.

`Postgres` and `MariaDb` modules contain Flyway migration definitions to keep required tables schemas up-to-date,
located in `classpath:db/idempotence4j/postgres` and `classpath:db/idempotence4j/mariadb` respectively.
Both `yaml` and `java` configuration examples for `Postgres` are provided below:

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
public Flyway flyway(DataSource dataSource) {
    var configuration = new FluentConfiguration()
        .dataSource(dataSource)
        .locations("classpath:db/migration", "classpath:db/idempotence4j/postgres");

    return new Flyway(configuration);
}

```

### Retention

By default action **retention** is turned off. You can enable and configure it by specifying following parameters:

```yaml
idempotence4j:
  retention:
    enabled: true
    period: P0Y0M20D              # ISO-8601 format
    purge:
      schedule: "*/15 * * * * ?"  # spring cron format
      batchSize: 150
```


> :exclamation: **_Important:_**  Please note, if you didn't enable flyway as described in a previous section you need first to define required for `db-scheduled` tables in your schema

### Metrics

Each action execution collects and publishes metrics. **idempotence4j-metrics** module provides default `io.micrometer` integration and publishes following metrics:

- idempotence4j.executions - **counter** with a set of tags {`type`, `client`, `outcome`}
- idempotence4j.executions.retries - **counter** with a set of tags {`type`, `client`}
- idempotence4j.execution.latency - **timer** with a set of tags {`type`, `client`, `outcome`}

We also provide a common `grafana` dashboard [component](https://github.com/transferwise/grafana-dashboards/blob/master/dashboards/src/components/idempotence4j/actions.libsonnet) that you can include into your provisioned dashboard.

### Roadmap

- Non-transactional execution strategy
- Payload schema evolution
- Result payload compression
- Redis module
