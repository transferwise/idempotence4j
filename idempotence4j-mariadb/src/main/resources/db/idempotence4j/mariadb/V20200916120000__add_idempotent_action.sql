CREATE TABLE idempotent_action (
    seq_id          BIGINT          NOT NULL                PRIMARY KEY AUTO_INCREMENT,
    `key`           VARCHAR(255)    CHARACTER SET latin1    NOT NULL,
    type            VARCHAR(255)    CHARACTER SET latin1    NOT NULL,
    client          VARCHAR(255)    CHARACTER SET latin1    NOT NULL,
    created_at      TIMESTAMP       NOT NULL,
    last_run_at     TIMESTAMP       NULL,
    completed_at    TIMESTAMP       NULL,
    result          BLOB,
    result_type     VARCHAR(255)    CHARACTER SET latin1,

    UNIQUE KEY action_key (`key`, type, client)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE INDEX IF NOT EXISTS IDX_IDEMPOTENT_ACTION_CREATED_AT ON idempotent_action (created_at ASC);
