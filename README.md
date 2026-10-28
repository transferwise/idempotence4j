<a href="https://img.shields.io/badge/release-2.1.0-orange">
        <img src="https://img.shields.io/badge/release-2.1.0-orange"
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
- **idempotence4j-spring-web** - `@Idempotent` support for Spring MVC handler methods driven by an `Idempotency-Key` request header
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

### Payload fingerprint

A client may reuse an action id for a different request by mistake. By default the stored result of the first request is returned.
To detect this, pass a fingerprint of the request payload as the second argument:

```java
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

private static String fingerprint(byte[] payload) {
    try {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(payload));
    } catch (NoSuchAlgorithmException ex) {
        throw new IllegalStateException("SHA-256 is not available", ex);
    }
}

Transfer transfer = idempotenceService.execute(
    actionId,
    fingerprint(payloadBytes),
    transferRepository::get,
    () -> transferRepository.save(Transfer.create(command)),
    Transfer::getId,
    new TypeReference<TransferId>() {}
);
```

- The fingerprint is stored with the action when it is first submitted. It must not be longer than 255 characters,
  otherwise `IllegalArgumentException` is thrown. A hex encoded SHA-256 digest is 64 characters.
- If the same action id is submitted again with a different fingerprint, `ActionPayloadMismatchException` is thrown
  instead of executing or replaying the action, whether the first request completed or is still in progress.
  Map it to a non-retryable response, for example HTTP `422 Unprocessable Entity`.
- The check is skipped when either side has no fingerprint, so actions stored before this feature, or calls without a fingerprint, behave as before.
- Compute the fingerprint only from the fields that define the request, not from values that change between retries such as timestamps.

### Spring MVC

With the starter in a servlet web application, annotate a handler method with `@Idempotent`:

```java
@RestController
public class TransferController {

    @Idempotent
    @PostMapping("/transfers")
    public ResponseEntity<Transfer> create(@RequestBody CreateTransferRequest request) {
        Transfer transfer = transferService.create(request);
        return ResponseEntity.created(URI.create("/transfers/" + transfer.getId())).body(transfer);
    }
}
```

- The key is read from the `Idempotency-Key` header, configurable with `idempotence4j.web.key-header`.
- The first request executes the method and persists its return value. A `ResponseEntity` is persisted with its status, headers and body.
  A retry with the same key returns the persisted response without executing the method, with an `Idempotent-Replayed: true` header.
- The method runs in the same transaction as the action record, so its database writes and the persisted response commit together.
- Exceptions thrown by the method are not persisted. They reach your exception handlers unchanged, and a retry executes the method again.
  A non 2xx `ResponseEntity` returned normally is persisted like any other response.
- The action id is made of the key, an action type (`SimpleClassName.methodName` by default, or `@Idempotent(type = "...")`)
  and a client. The client is the authenticated principal name, or `anonymous`. Provide your own `IdempotentClientResolver` bean
  to use for example a tenant or API client id, so different clients can never share a key.
- The request fingerprint covers the HTTP method, path, request parameters and the deserialized `@RequestBody`,
  serialized with the application's Jackson `ObjectMapper` bean when there is a single one.

| Situation | Response |
| --- | --- |
| Missing key (unless `@Idempotent(required = false)`), or key longer than 255 characters | `400 Bad Request` |
| Another request with the same key is in progress | `409 Conflict` |
| Same key reused for a different request | `422 Unprocessable Entity` |

These are `IdempotentRequestException`, a `ResponseStatusException`, so your exception handlers can customize them.
Methods returning asynchronous, streaming or reactive types (`DeferredResult`, `CompletableFuture`, `SseEmitter`,
`StreamingResponseBody`, `Resource`, `Publisher`, ...) are not supported and fail the application startup.
The annotation is applied through Spring AOP proxies, so it requires Spring Boot's AOP auto configuration (`spring.aop.auto`, enabled by default).

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

> :exclamation: **_Upgrading:_** new library versions may add migrations, for example `V20261028120000__add_idempotent_action_fingerprint.sql` in `2.1.0`.
> Apply them before deploying the new version. If you keep copies of the migrations, copy the new ones too.

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

- idempotence4j.executions - **counter** with a set of tags {`type`, `client`, `outcome`}, where `outcome` is one of `SUCCESS`, `ERROR`, `CONFLICT` or `MISMATCH`
- idempotence4j.executions.retries - **counter** with a set of tags {`type`, `client`}
- idempotence4j.execution.latency - **timer** with a set of tags {`type`, `client`, `outcome`}

We also provide a common `grafana` dashboard [component](https://github.com/transferwise/grafana-dashboards/blob/master/dashboards/src/components/idempotence4j/actions.libsonnet) that you can include into your provisioned dashboard.

### Roadmap

- Non-transactional execution strategy
- Payload schema evolution
- Result payload compression
- Redis module
