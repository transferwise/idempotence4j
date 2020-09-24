CREATE TABLE IF NOT EXISTS idempotence4j_scheduled_tasks (
    task_name               VARCHAR(40) NOT NULL,
    task_instance           VARCHAR(40) NOT NULL,
    task_data               BYTEA,
    execution_time          TIMESTAMP WITH TIME ZONE NOT NULL,
    picked BOOLEAN          NOT NULL,
    picked_by               VARCHAR(50),
    last_success            TIMESTAMP WITH TIME ZONE,
    last_failure            TIMESTAMP WITH TIME ZONE,
    consecutive_failures    INT,
    last_heartbeat          TIMESTAMP WITH TIME ZONE,
    version BIGINT          NOT NULL,

    PRIMARY KEY (task_name, task_instance)
)
