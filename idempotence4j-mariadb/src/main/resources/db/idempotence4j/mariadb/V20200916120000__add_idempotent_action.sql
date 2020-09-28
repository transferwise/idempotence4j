CREATE TABLE idempotent_action (
    `key`           VARCHAR(255)    CHARACTER SET latin1    NOT NULL,
    type            VARCHAR(255)    CHARACTER SET latin1    NOT NULL,
    client          VARCHAR(255)    CHARACTER SET latin1    NOT NULL,
    created_at      TIMESTAMP       NOT NULL,
    last_run_at     TIMESTAMP       NULL,
    completed_at    TIMESTAMP       NULL,
    result          BLOB,
    result_type     VARCHAR(255)    CHARACTER SET latin1,

    CONSTRAINT idempotent_action_pk PRIMARY KEY(`key`, type, client)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE INDEX IF NOT EXISTS IDX_IDEMPOTENT_ACTION_CREATED_AT ON idempotent_action (created_at ASC);
