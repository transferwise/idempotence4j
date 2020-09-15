package com.transferwise.idempotence4j.core.retention;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.transferwise.idempotence4j.core.ActionRepository;
import lombok.NonNull;

import javax.sql.DataSource;

public class RetentionService {
    private static final String SCHEDULER_TASK_NAME = "idempotence4j_scheduled_tasks";

    private final DataSource dataSource;
    private final Scheduler scheduler;

    public RetentionService(
        @NonNull DataSource dataSource,
        @NonNull ActionRepository actionRepository,
        @NonNull RetentionPolicy retentionPolicy
    ) {
        this.dataSource = dataSource;
        this.scheduler = Scheduler
            .create(dataSource)
            .tableName(SCHEDULER_TASK_NAME)
            .startTasks(new PurgeJob(actionRepository, retentionPolicy.getPurgeJobConfiguration(), retentionPolicy.getPeriod()))
            .threads(5)
            .build();
    }

    public void initialize() {
        scheduler.start();
    }

    public void shutdown() {
        scheduler.stop();
    }
}
