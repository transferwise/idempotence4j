CREATE INDEX IF NOT EXISTS IDX_IDEMPOTENT_ACTION_TYPE_CLIENT
    ON idempotent_action (type, client);
