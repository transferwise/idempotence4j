package com.transferwise.idempotence4j.core;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

import java.time.Instant;
import java.util.Optional;

@Getter
@EqualsAndHashCode
public class Action {
	private final ActionId actionId;
	private final Instant createdAt;
	private Instant lastRunAt;
	private Instant completedAt;
	private Result result;

	public Action(@NonNull ActionId actionId) {
		this.actionId = actionId;
		this.createdAt = ClockKeeper.now();
	}

	public void started() {
		this.lastRunAt = ClockKeeper.now();
	}

    public void completed(Result result) {
		this.result = result;
		this.completedAt = ClockKeeper.now();
	}

	public boolean hasCompleted() {
		return completedAt != null;
	}

	public boolean hasResult() {
		return result != null;
	}

	public Optional<Result> getResult() {
		return Optional.ofNullable(result);
	}

	public Optional<Instant> getLastRunAt() {
		return Optional.ofNullable(lastRunAt);
	}

	public Optional<Instant> getCompletedAt() {
		return Optional.ofNullable(completedAt);
	}
}
