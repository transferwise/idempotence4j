package com.transferwise.idempotence4j.benchmarks.postgres;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.transferwise.idempotence4j.core.ActionId;
import com.transferwise.idempotence4j.core.DefaultIdempotenceService;
import com.transferwise.idempotence4j.core.IdempotenceService;
import com.transferwise.idempotence4j.core.LockProvider;
import com.transferwise.idempotence4j.core.metrics.Metrics;
import com.transferwise.idempotence4j.core.metrics.MetricsPublisher;
import com.transferwise.idempotence4j.core.serializers.json.JsonResultSerializer;
import com.transferwise.idempotence4j.postgres.JdbcPostgresActionRepository;
import com.transferwise.idempotence4j.postgres.JdbcPostgresLockProvider;
import com.transferwise.idempotence4j.utils.PropertiesLoader;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jmh.mbr.junit5.Microbenchmark;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;

import java.io.IOException;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Measurement(iterations = 5, time = 1)
@Warmup(iterations = 10, time = 1)
@Fork(2)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Threads(Threads.MAX)
@Microbenchmark
public class PostgresIdempotenceServiceBenchmarkTest {

    @Benchmark
    public void noOp(BenchmarkContext context) throws IOException {
        ActionId actionId = new ActionId(UUID.randomUUID(), "ADD_ACTION", "idempotence4j");
        context.idempotenceService.execute(actionId, () -> {
            return new Result(UUID.randomUUID().toString());
        }, new TypeReference<Result>(){});
    }


    @State(Scope.Benchmark)
    public static class BenchmarkContext {

        volatile IdempotenceService idempotenceService;
        volatile DataSource dataSource;
        volatile Flyway flyway;

        @Setup
        public void setup() throws IOException, ExecutionException, InterruptedException {
            this.dataSource = getDataSource(PropertiesLoader.loadProperties("datasource.properties"));
            this.flyway = getFlyway(dataSource);
            this.idempotenceService = getIdempotenceService(dataSource);

            this.flyway.migrate();
            PsqlDataGenerator.generateActions(dataSource, 1_000_000);
        }

        @TearDown
        public void clean() {
            this.flyway.clean();
        }

        private IdempotenceService getIdempotenceService(DataSource dataSource) {
            DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(dataSource);
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            LockProvider lockProvider = new JdbcPostgresLockProvider(jdbcTemplate);
            JdbcPostgresActionRepository repository = new JdbcPostgresActionRepository(jdbcTemplate);
            JsonResultSerializer resultSerializer = new JsonResultSerializer(new ObjectMapper().registerModule(new JavaTimeModule()));
            MetricsPublisher metricsPublisher = new MetricsVoidPublisher();

            return new DefaultIdempotenceService(transactionManager, lockProvider, repository, resultSerializer, metricsPublisher);
        }

        private DataSource getDataSource(Properties properties) {
            HikariConfig config = new HikariConfig();
            String jdbcUrl = properties.getProperty("datasource.url");
            config.setJdbcUrl(jdbcUrl);
            config.setUsername(properties.getProperty("datasource.username"));
            config.setPassword(properties.getProperty("datasource.password"));
            config.setDriverClassName("org.postgresql.Driver");

            return new HikariDataSource(config);
        }

        private Flyway getFlyway(DataSource dataSource) {
            FluentConfiguration configuration = new FluentConfiguration()
                .dataSource(dataSource)
                .locations("classpath:db/idempotence4j/postgres");

            return new Flyway(configuration);
        }
    }

    private static class Result {
        private String value;

        public Result() {
        }

        public Result(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    private static class MetricsVoidPublisher implements MetricsPublisher {

        @Override
        public void publish(Metrics metrics) {

        }
    }
}
