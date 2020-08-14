# 1. Idempotence service.

Date: 2020-05-12

## Status

Approved

## Context

In computing, an **idempotent operation** is one that has no additional effect if it is called more than once with the same input parameters.
A vast majority of critical flows of a web services we're running is required to provide this guarantee for their clients.
Thus, it makes sense to extract this functionality into a library with support for common use-cases.

There a two common approaches in the industry when it comes to implementing support for idempotency:
1) distributed **in-memory** data stores (i.e. Redis)
2) **database** - master node to avoid any issues caused by possible replication lag

### Client
Typically, the only additional requirement for the clients to enable idempotency is to add a unique identifier for the request
that is only expected to have a side effect once. Sometimes APIs make this a mandatory property of the request.
From a security considerations and to lower a chance of collisions it's recommended to use UUIDv4 as a unique request identifier.

### Service
Critical requirements for the processing service:
1) **persisted result** for a successful and failed with non-retriable error request
2) **non-blocking** - any concurrent request with a same identifier should be rejected with a transient error

Result persistence aims to avoid any heavy computations on a re-try or duplicate requests if original request completed with a **success** or **non-retriable** error.
Clients should have a freedom of choice on what exactly is being persisted and which format to use.

Quick failure in case of the aggressive retry policy on the client side ensures that no server resource is blocked and wasted for such concurrent requests with a same identifier.

3) request processing procedure and persistence of its result has to be transactional
We require request execution logic and idempotence status update to happen in a same transaction.
That requirement comes at a cost of the client flexibility. Allowing clients to control transactional context
introduces edge-cases such as a failure to persist idempotency status whereas action has been completed and side effect taken place.

## Decision

Provide code idempotence service based on a database approach, specific database integration is pluggable.

Both **in-memory** and **database** approaches have their own pros and cons, we're not going to go in depth comparision here
mostly due to the lack of adoption of Redis in our current infrastructure. That said we should definitely consider providing such support in the future.


We require request execution logic and idempotence status update to happen in a **same transaction**.
That requirement comes at a cost of the client flexibility, but allowing clients to have full control over transaction context
introduces edge-cases such as a **failure** to update imdepotency action status after **side effect** already took place.
That would require clients to verify if side effect has taken place on each execution - which we consider suboptimal for most common cases.

If that approach proves to be limiting for certain flows we can extend core service interface to allow clients to choose different execution strategies and have better control over transactional context.

## Consequences

This should guarantee strong data consistency, fast lookup and near non-blocking execution.
