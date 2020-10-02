package com.transferwise.idempotence4j.benchmarks.postgres;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.f4b6a3.uuid.UuidCreator;
import com.transferwise.idempotence4j.benchmarks.domain.model.Operation;
import com.transferwise.idempotence4j.benchmarks.domain.model.OperationId;
import com.transferwise.idempotence4j.benchmarks.domain.model.OperationRepository;
import com.transferwise.idempotence4j.benchmarks.infrastructure.JdbcOperationRepository;
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
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Measurement(iterations = 10, time = 1)
@Warmup(iterations = 10, time = 1)
@Fork(2)
@BenchmarkMode({Mode.Throughput, Mode.SampleTime})
@OutputTimeUnit(TimeUnit.SECONDS)
@Threads(Threads.MAX)
@Microbenchmark
public class PostgresIdempotenceServiceBenchmarkTest {

    /**
     * Remember baseline is just another test
     */
    @Benchmark
    public void baseline(BenchmarkContext context, Blackhole blackhole) {
        blackhole.consume(exec(context));
    }

    @Benchmark
    public void transactionalBaseline(BenchmarkContext context, Blackhole blackhole) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(context.transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        Operation operation = transactionTemplate.execute(status -> exec(context));
        blackhole.consume(operation);
    }

    @Benchmark
    public void executeIdempotentAction(BenchmarkContext context, Blackhole blackhole) {
        ActionId actionId = new ActionId(UuidCreator.getTimeOrdered(), "ADD_ACTION", "idempotence4j");
        Operation result = context.idempotenceService.execute(actionId, id -> new Operation(id), () -> {
            return exec(context);
        }, Operation::getOperationId, new TypeReference<OperationId>(){});
        blackhole.consume(result);
    }

    private Operation exec(BenchmarkContext context) {
        Blackhole.consumeCPU(100);
        Operation operation = new Operation(OperationId.nextId());
        context.operationRepository.save(operation);
        return operation;
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder().include(".*" + PostgresIdempotenceServiceBenchmarkTest.class.getSimpleName() + ".*")
            .resultFormat(ResultFormatType.JSON)
            .forks(2)
            .build();

        new Runner(opt).run();
    }

    @State(Scope.Benchmark)
    public static class BenchmarkContext {

        volatile IdempotenceService idempotenceService;
        volatile OperationRepository operationRepository;
        volatile DataSource dataSource;
        volatile PlatformTransactionManager transactionManager;
        volatile Flyway flyway;

        @Param({"1000", "100000", "1000000", "5000000"})
        static int dbSize;

        @Setup
        public void setup() throws IOException, ExecutionException, InterruptedException {
            this.dataSource = getDataSource(PropertiesLoader.loadProperties("datasource.properties"));
            this.transactionManager = new DataSourceTransactionManager(dataSource);
            this.flyway = getFlyway(dataSource);
            this.idempotenceService = getIdempotenceService(dataSource, transactionManager);
            this.operationRepository = new JdbcOperationRepository(new JdbcTemplate(dataSource));

            this.flyway.migrate();
            PsqlDataGenerator.generateActions(dataSource, dbSize);
        }

        @TearDown
        public void clean() {
            this.flyway.clean();
            ((HikariDataSource)this.dataSource).close();
        }

        private IdempotenceService getIdempotenceService(DataSource dataSource, PlatformTransactionManager transactionManager) {
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
                .locations("classpath:db/migration","classpath:db/idempotence4j/postgres");

            return new Flyway(configuration);
        }
    }

    private static class MetricsVoidPublisher implements MetricsPublisher {

        @Override
        public void publish(Metrics metrics) {

        }
    }
}
