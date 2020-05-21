package com.transferwise.idempotence4j.postgres;

import com.transferwise.idempotence4j.core.Lock;

public class PostgresRowLock implements Lock {

    @Override
    public void release() {
        //no-op
    }
}
