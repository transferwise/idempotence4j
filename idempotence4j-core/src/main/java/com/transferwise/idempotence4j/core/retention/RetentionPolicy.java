package com.transferwise.idempotence4j.core.retention;

import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;
import com.cronutils.utils.Preconditions;
import com.github.kagkarlsson.scheduler.task.schedule.Schedule;
import com.github.kagkarlsson.scheduler.task.schedule.Schedules;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Period;
import java.time.temporal.TemporalAmount;

@Getter
public class RetentionPolicy {
    private final TemporalAmount temporalAmount;
    private final PurgeJobConfiguration purgeJobConfiguration;

    public RetentionPolicy(
        String period,
        String duration,
        @NonNull PurgeJobConfiguration purgeJobConfiguration
    ) {
        if (period != null && duration == null) {
            Period p = Period.parse(period);
            this.temporalAmount = p;
            Preconditions.checkArgument(!(p.isNegative() || p.isZero()), "Retention period has to be positive or null");
        } else if (period == null && duration != null) {
            Duration d = Duration.parse(duration);
            this.temporalAmount = d;
            Preconditions.checkArgument(!(d.isNegative() || d.isZero()), "Retention duration has to be positive or null");
        } else {
            throw new IllegalArgumentException("Either period or duration must be specified");
        }

        this.purgeJobConfiguration = purgeJobConfiguration;
    }

    public Schedule getSchedule() {
        return Schedules.cron(getPurgeJobConfiguration().getSchedule());
    }

    @Getter
    @Slf4j
    public static class PurgeJobConfiguration {
        private final String schedule;
        private final Integer batchSize;

        public PurgeJobConfiguration(
            @NonNull String schedule,
            @NonNull Integer batchSize
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
