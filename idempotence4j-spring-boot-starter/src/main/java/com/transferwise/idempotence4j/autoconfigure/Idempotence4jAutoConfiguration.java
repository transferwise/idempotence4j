package com.transferwise.idempotence4j.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transferwise.idempotence4j.core.ActionRepository;
import com.transferwise.idempotence4j.core.DefaultIdempotenceService;
import com.transferwise.idempotence4j.core.IdempotenceService;
import com.transferwise.idempotence4j.core.LockProvider;
import com.transferwise.idempotence4j.core.ResultSerializer;
import com.transferwise.idempotence4j.core.metrics.MetricsPublisher;
import com.transferwise.idempotence4j.core.retention.RetentionPolicy;
import com.transferwise.idempotence4j.core.retention.RetentionPolicy.PurgeJobConfiguration;
import com.transferwise.idempotence4j.core.retention.RetentionService;
import com.transferwise.idempotence4j.core.serializers.json.JsonResultSerializer;
import com.transferwise.idempotence4j.mariadb.JdbcMariaDbActionRepository;
import com.transferwise.idempotence4j.mariadb.JdbcMariaDbLockProvider;
import com.transferwise.idempotence4j.metrics.micrometer.MicrometerMetricsPublisher;
import com.transferwise.idempotence4j.postgres.JdbcPostgresActionRepository;
import com.transferwise.idempotence4j.postgres.JdbcPostgresLockProvider;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

import java.time.Period;

import static com.transferwise.idempotence4j.autoconfigure.Idempotence4jAutoConfiguration.MetricsPublisherAutoConfiguration;

@Configuration
@Slf4j
@ConditionalOnClass(value = {JdbcTemplate.class, PlatformTransactionManager.class})
@AutoConfigureAfter(value = {MetricsPublisherAutoConfiguration.class})
public class Idempotence4jAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "com.fasterxml.jackson.databind.ObjectMapper")
    public ResultSerializer jsonResultSerializer(ObjectMapper objectMapper) {
        return new JsonResultSerializer(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean({IdempotenceService.class, MetricsPublisher.class})
    @ConditionalOnBean({ActionRepository.class, LockProvider.class, ResultSerializer.class})
    public IdempotenceService idempotenceService(
        PlatformTransactionManager platformTransactionManager,
        ActionRepository actionRepository,
        LockProvider lockProvider,
        ResultSerializer resultSerializer
    ) {
        return new DefaultIdempotenceService(platformTransactionManager, lockProvider, actionRepository, resultSerializer, m -> {});
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean({ActionRepository.class, LockProvider.class, ResultSerializer.class, MetricsPublisher.class})
    public IdempotenceService idempotenceServiceWithMetrics(
        PlatformTransactionManager platformTransactionManager,
        ActionRepository actionRepository,
        LockProvider lockProvider,
        ResultSerializer resultSerializer,
        MetricsPublisher metricsPublisher
    ) {
        return new DefaultIdempotenceService(platformTransactionManager, lockProvider, actionRepository, resultSerializer, metricsPublisher);
    }

    /**
     * The starter ships both database modules, so the implementation is picked from the datasource rather than the classpath
     */
    @Configuration
    public static class DatabaseAutoConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public ActionRepository actionRepository(DataSource dataSource) {
            return switch (Database.of(dataSource)) {
                case POSTGRES -> new JdbcPostgresActionRepository(new JdbcTemplate(dataSource));
                case MARIADB -> new JdbcMariaDbActionRepository(new JdbcTemplate(dataSource));
            };
        }

        @Bean
        @ConditionalOnMissingBean
        public LockProvider lockProvider(DataSource dataSource) {
            return switch (Database.of(dataSource)) {
                case POSTGRES -> new JdbcPostgresLockProvider(new JdbcTemplate(dataSource));
                case MARIADB -> new JdbcMariaDbLockProvider(dataSource);
            };
        }
    }

    @Configuration
    @ConditionalOnClass({ MicrometerMetricsPublisher.class, MeterRegistry.class })
    public static class MetricsPublisherAutoConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public MetricsPublisher metricPublisher(MeterRegistry meterRegistry) {
            return new MicrometerMetricsPublisher(meterRegistry);
        }
    }

    @Configuration
    @EnableConfigurationProperties
    public static class RetentionAutoConfiguration {
        @Bean
        @ConfigurationProperties(prefix = "idempotence4j.retention")
        public RetentionProperties retentionProperties() {
            return new RetentionProperties();
        }

        @Bean(initMethod = "initialize", destroyMethod = "shutdown")
        @ConditionalOnProperty(name="idempotence4j.retention.enabled", havingValue="true", matchIfMissing = false)
        public RetentionService retentionService(DataSource dataSource, ActionRepository actionRepository, RetentionProperties retentionProperties) {
            RetentionPolicy retentionPolicy = new RetentionPolicy(
                Period.parse(retentionProperties.getPeriod()),
                new PurgeJobConfiguration(
                    retentionProperties.getPurge().getSchedule(),
                    retentionProperties.getPurge().getBatchSize())
            );

            return new RetentionService(dataSource, actionRepository, retentionPolicy);
        }
    }
}
