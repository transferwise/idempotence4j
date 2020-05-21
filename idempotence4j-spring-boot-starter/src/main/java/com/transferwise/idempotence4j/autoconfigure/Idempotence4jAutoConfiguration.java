package com.transferwise.idempotence4j.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transferwise.idempotence4j.core.ActionRepository;
import com.transferwise.idempotence4j.core.IdempotenceService;
import com.transferwise.idempotence4j.core.LockProvider;
import com.transferwise.idempotence4j.core.ResultSerializer;
import com.transferwise.idempotence4j.core.serializers.json.JsonResultSerializer;
import com.transferwise.idempotence4j.postgres.JdbcPostgresActionRepository;
import com.transferwise.idempotence4j.postgres.JdbcPostgresLockProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;


@Configuration
@Slf4j
@ConditionalOnBean({PlatformTransactionManager.class, DataSource.class})
public class Idempotence4jAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(ObjectMapper.class)
    public ResultSerializer jsonResultSerializer(ObjectMapper objectMapper) {
        return new JsonResultSerializer(objectMapper);
    }

    @Bean
    @ConditionalOnBean({PlatformTransactionManager.class, ActionRepository.class, LockProvider.class, ResultSerializer.class})
    public IdempotenceService idempotenceService(
        PlatformTransactionManager platformTransactionManager,
        ActionRepository actionRepository,
        LockProvider lockProvider,
        ResultSerializer resultSerializer
    ) {
        return new IdempotenceService(platformTransactionManager, actionRepository, lockProvider, resultSerializer);
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
}
