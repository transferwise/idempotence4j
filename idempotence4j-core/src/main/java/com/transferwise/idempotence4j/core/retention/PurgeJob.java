package com.transferwise.idempotence4j.core.retention;

import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.transferwise.idempotence4j.core.ActionRepository;
import com.transferwise.idempotence4j.core.ClockKeeper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Slf4j
public class PurgeJob extends RecurringTask<Void> {
    private static final String JOB_NAME = "idempotent-action-purge-job";

    private final ActionRepository actionRepository;
    private final RetentionPolicy retentionPolicy;

    public PurgeJob(
        @NonNull ActionRepository actionRepository,
        @NonNull RetentionPolicy retentionPolicy
    ) {
        super(JOB_NAME, retentionPolicy.getSchedule(), Void.class);
        this.actionRepository = actionRepository;
        this.retentionPolicy = retentionPolicy;
    }

    @Override
    public void executeRecurringly(TaskInstance<Void> taskInstance, ExecutionContext executionContext) {
        log.info("Running IdempotentAction purge job");
        Instant olderThan = LocalDate.now(ClockKeeper.get())
            .minus(retentionPolicy.getTemporalAmount())
            .atStartOfDay()
            .toInstant(ZoneOffset.UTC);

        actionRepository.deleteOlderThan(olderThan, retentionPolicy.getPurgeJobConfiguration().getBatchSize());
    }
}
