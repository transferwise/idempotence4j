package com.transferwise.idempotence4j.core.executor;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
public class MeasuredExecutor {
    private final Map<Class<? extends Exception>, BiConsumer<Duration, Throwable>> onError;
    private BiConsumer<Duration, Throwable> onUnexpectedError;
    private Consumer<Duration> onSuccess;

    public MeasuredExecutor() {
        this.onError = new HashMap<>();
    }

    public MeasuredExecutor onSuccess(@NonNull Consumer<Duration> onSuccess) {
        this.onSuccess = onSuccess;
        return this;
    }

    public MeasuredExecutor onError(@NonNull Class<? extends Exception> clazz, @NonNull BiConsumer<Duration, Throwable> consumer) {
        this.onError.put(clazz, consumer);
        return this;
    }

    public MeasuredExecutor onUnexpectedError(@NonNull BiConsumer<Duration, Throwable> consumer) {
        this.onUnexpectedError = consumer;
        return this;
    }

    public <R> R submit(@NonNull Supplier<R> supplier) {
        Instant start = Instant.now();
        try {
            R result = supplier.get();
            runOnSuccess(Duration.between(start, Instant.now()));

            return result;
        } catch (Throwable ex) {
            Duration duration = Duration.between(start, Instant.now());
            if(onError.containsKey(ex.getClass())) {
                runOnError(onError.get(ex.getClass()), ex, duration);
            } else {
                runOnError(onUnexpectedError, ex, duration);
            }

            throw ex;
        }
    }

    private void runOnSuccess(Duration duration) {
        if (null != onSuccess) {
            try {
                onSuccess.accept(duration);
            } catch (Exception ex) {
                log.warn("Failed to execute onComplete. Suppressing error", ex);
            }
        }
    }

    private void runOnError(BiConsumer<Duration, Throwable> consumer, Throwable throwable, Duration duration) {
        if (null != consumer) {
            try {
                consumer.accept(duration, throwable);
            } catch (Exception ex) {
                log.warn("Failed to execute onError. Suppressing error", ex);
            }
        }
    }

}
