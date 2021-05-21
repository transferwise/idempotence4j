package com.transferwise.idempotence4j.core;

import java.time.Clock;
import java.time.Instant;

public class ClockKeeper {
    private static ThreadLocal<Clock> clock =
        ThreadLocal.withInitial(Clock::systemUTC);

    public static void set(Clock clock) {
        ClockKeeper.clock.set(clock);
    }

    public static Clock get() {
        return ClockKeeper.clock.get();
    }

    public static Instant now() {
        return Instant.now(ClockKeeper.clock.get());
    }
}
