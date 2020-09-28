CREATE TABLE idempotent_action (
    key             TEXT        NOT NULL,
    type            TEXT        NOT NULL,
    client          TEXT        NOT NULL,
    created_at      TIMESTAMP   NOT NULL,
    last_run_at     TIMESTAMP,
    completed_at    TIMESTAMP,
    result          BYTEA,
    result_type     VARCHAR(255),

    PRIMARY KEY(key, type, client)
);
