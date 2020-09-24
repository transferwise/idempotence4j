package com.transferwise.idempotence4j.mariadb;

import com.transferwise.idempotence4j.core.Action;
import com.transferwise.idempotence4j.core.Lock;
import lombok.NonNull;

public class MaridDbRowLock extends Lock {

    public MaridDbRowLock(@NonNull Action action) {
        super(action);
    }

    @Override
    public void release() {
        //no-op
    }
}
