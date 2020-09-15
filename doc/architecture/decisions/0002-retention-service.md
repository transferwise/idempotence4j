# 1. Retention service.

Date: 2020-09-15

## Status

Approved

## Context

Idempotency guarantees for an action are normally provided for a **limited period of time**. That creates an opportunity for a service to periodically do a **cleanup** of stored actions and as a result avoid ever growing storage requirements and performance implications.

We’ve considered few approaches to this problem:
- partition
- periodical purge

Performance wise introducing partitions in idempotent_action table seemed like the best idea. Performance benefits outweighed any complications of facilitating and dropping partitions (i.e. there is no way to automatically create partition in Postgres). The issue comes with an absence of global index across partitions in some databases (i.e. Postgres) that creates a limitation that any idempotent action key has to **uniquely map into a partition**. Since the most natural choice of a partition key is a date, it would mean each `action_id` would need to come along with a date or have a date bit embedded, i.e. **UUIDv2**. So far we’ve tried to avoid imposing any limitations on a key format, hence we put the idea of partitioning on hold.

Periodical purge seemed like the only other feasible approach for a database that is lacking time-to-live (TTL) indexes.
It comes with its own challenges like multi-instance set up and DB performance.
Clients have to be extremely careful with batch size of purging to avoid any negative effect on database performance.

To avoid any need for co-ordination of purge job execution in a multi instance setup or a dependency on Zookeeper/etcd for a leader election we opted for a lightweight database scheduler `db-scheduler` that guarantees single instance execution.

## Decision

Allow clients to configure retention policy for idempotent actions.

Clients can specify:
 - retention period (ISO-8601 format)
 - cron schedule for purge job (spring format)
 - batch-size for removal operation

```yaml
idempotence4j:
  retention:
    enabled: true
    period: P0Y0M20D
    purge:
      schedule: "*/15 * * * * ?"
      batchSize: 150
```

Purge job will only be executed on a single node in a multi instance set up.

## Consequences

Services that are using a database that has a support for TTL indexes won't need to enable retention feature.
Otherwise, retention policy and purge properties should be carefully configured taking DB performance into account.

If single instance purge execution becomes a bottleneck we can iterate on the approach and allow mutli-instance execution.

Long-term if a global index across partitions becomes generally available in a majority of databases we will re-evaluate this decision.
