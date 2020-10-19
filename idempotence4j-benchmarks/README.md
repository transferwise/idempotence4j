# Benchmarking

### Basic Considerations

This module is meant to be a starting point for a library benchmarking and profiling.
We've configured JMH benchmark tests to validate initial hypothesis about performance, i.e. degradation over time as DB table and index sizes grow.

We also define a sample Spring Boot web application that exposes an endpoint to execute a sample idempotent operation.
Actuator `prometheus` endpoint is enabled for collecting metrics from JVM, Tomcat and Hikari pool.


### Runtime environment

TBA: Describe in details the environment we've been running load tests in.
(Number of k8s workers, worker instance types, cpu/memory request, envoy configs, RDS instance, etc)

TBA: Describe in details load tests results

