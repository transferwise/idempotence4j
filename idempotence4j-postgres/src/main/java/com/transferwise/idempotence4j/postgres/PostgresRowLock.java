package com.transferwise.idempotence4j.postgres;

import com.transferwise.idempotence4j.core.Action;
import com.transferwise.idempotence4j.core.Lock;
import lombok.NonNull;

public class PostgresRowLock extends Lock {

    public PostgresRowLock(@NonNull Action action) {
        super(action);
    }

    @Override
    public void release() {
        //no-op
    }
}
