package com.transferwise.idempotence4j.core.retention;

import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;
import com.cronutils.utils.Preconditions;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.time.Period;

@Getter
public class RetentionPolicy {
    private final Period period;
    private final PurgeJobConfiguration purgeJobConfiguration;

    public RetentionPolicy(
        @NonNull Period period,
        @NonNull PurgeJobConfiguration purgeJobConfiguration
    ) {
        Preconditions.checkArgument(!(period.isNegative() || period.isZero()), "Retention period has to be positive");

        this.period = period;
        this.purgeJobConfiguration = purgeJobConfiguration;
    }

    @Getter
    @Slf4j
    public static class PurgeJobConfiguration {
        private final String schedule;
        private final Long batchSize;

        public PurgeJobConfiguration(
            @NonNull String schedule,
            @NonNull Long batchSize
        ) {
            Preconditions.checkArgument(isScheduleValid(schedule), "Prune schedule is malformed");
            Preconditions.checkArgument(batchSize > 0, "Prune batch-size has to be a positive number");

            this.schedule = schedule;
            this.batchSize = batchSize;
        }

        public boolean isScheduleValid(String cronSchedule) {
            try {
                CronParser parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.SPRING));
                parser.parse(cronSchedule);
                return true;
            } catch (IllegalArgumentException ex) {
                log.warn("Expression doesn't match cron definition", ex);
                return false;
            }
        }
    }
}
