package com.transferwise.idempotence4j.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transferwise.idempotence4j.core.ActionRepository;
import com.transferwise.idempotence4j.core.IdempotenceService;
import com.transferwise.idempotence4j.core.LockProvider;
import com.transferwise.idempotence4j.core.ResultSerializer;
import com.transferwise.idempotence4j.core.metrics.MetricsPublisher;
import com.transferwise.idempotence4j.core.serializers.json.JsonResultSerializer;
import com.transferwise.idempotence4j.metrics.micrometer.MicrometerMetricsPublisher;
import com.transferwise.idempotence4j.postgres.JdbcPostgresActionRepository;
import com.transferwise.idempotence4j.postgres.JdbcPostgresLockProvider;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

import static com.transferwise.idempotence4j.autoconfigure.Idempotence4jAutoConfiguration.PostgresAutoConfiguration;
import static com.transferwise.idempotence4j.autoconfigure.Idempotence4jAutoConfiguration.MetricsPublisherAutoConfiguration;

@Configuration
@Slf4j
@ConditionalOnClass(value = {JdbcTemplate.class, PlatformTransactionManager.class})
@AutoConfigureAfter(value = {PostgresAutoConfiguration.class, MetricsPublisherAutoConfiguration.class})
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
        return new IdempotenceService(platformTransactionManager, lockProvider, actionRepository, resultSerializer, m -> {});
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
        return new IdempotenceService(platformTransactionManager, lockProvider, actionRepository, resultSerializer, metricsPublisher);
    }

    @Configuration
    @ConditionalOnClass({ JdbcPostgresActionRepository.class, JdbcPostgresLockProvider.class })
    public static class PostgresAutoConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public ActionRepository postgresActionRepository(DataSource dataSource) {
            return new JdbcPostgresActionRepository(new JdbcTemplate(dataSource));
        }

        @Bean
        @ConditionalOnMissingBean
        public LockProvider postgresLockProvider(DataSource dataSource) {
            return new JdbcPostgresLockProvider(new JdbcTemplate(dataSource));
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
}
