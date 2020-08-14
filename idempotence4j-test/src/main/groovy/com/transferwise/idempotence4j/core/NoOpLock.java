package com.transferwise.idempotence4j.core;

import lombok.NonNull;

public class NoOpLock extends Lock {

    public NoOpLock(@NonNull Action lockedAction) {
        super(lockedAction);
    }

    @Override
    public void release() {
        //no op
    }
}
