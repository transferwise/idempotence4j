CREATE TABLE IF NOT EXISTS idempotence4j_scheduled_tasks (
    task_name               VARCHAR(40)     NOT NULL,
    task_instance           VARCHAR(40)     NOT NULL,
    task_data               BLOB,
    execution_time          TIMESTAMP(6)    NOT NULL,
    picked                  BOOLEAN         NOT NULL,
    picked_by               VARCHAR(50),
    last_success            TIMESTAMP(6),
    last_failure            TIMESTAMP(6),
    consecutive_failures    INT,
    last_heartbeat          TIMESTAMP(6),
    version                 BIGINT          NOT NULL,

    PRIMARY KEY (task_name, task_instance)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
