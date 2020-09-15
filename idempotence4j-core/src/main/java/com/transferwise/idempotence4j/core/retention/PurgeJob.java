package com.transferwise.idempotence4j.core.retention;

import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.schedule.Schedules;
import com.transferwise.idempotence4j.core.ActionRepository;
import com.transferwise.idempotence4j.core.ClockKeeper;
import com.transferwise.idempotence4j.core.retention.RetentionPolicy.PurgeJobConfiguration;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;

@Slf4j
public class PurgeJob extends RecurringTask<Void> {
    private static final String JOB_NAME = "idempotent-action-purge-job";

    private final ActionRepository actionRepository;
    private final PurgeJobConfiguration configuration;
    private final Period retentionPeriod;

    public PurgeJob(
        @NonNull ActionRepository actionRepository,
        @NonNull PurgeJobConfiguration configuration,
        @NonNull Period retentionPeriod
    ) {
        super(JOB_NAME, Schedules.cron(configuration.getSchedule()), Void.class);
        this.actionRepository = actionRepository;
        this.configuration = configuration;
        this.retentionPeriod = retentionPeriod;
    }

    @Override
    public void executeRecurringly(TaskInstance<Void> taskInstance, ExecutionContext executionContext) {
        log.info("Running IdempotentAction purge job");
        Instant olderThan = LocalDate.now(ClockKeeper.get())
            .minusDays(retentionPeriod.getDays())
            .atStartOfDay()
            .toInstant(ZoneOffset.UTC);

        actionRepository.deleteOlderThan(olderThan, configuration.getBatchSize());
    }
}
