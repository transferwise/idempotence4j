package com.transferwise.idempotence4j.core;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

import java.time.Instant;
import java.util.Optional;

@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class Action {
	public static final int MAX_FINGERPRINT_LENGTH = 255;

	private final ActionId actionId;
	private final Instant createdAt;
	private final String fingerprint;
	private Instant lastRunAt;
	private Instant completedAt;
	private Result result;

	public Action(@NonNull ActionId actionId) {
		this(actionId, null);
	}

	/**
	 * @param fingerprint optional digest of the request payload, used to detect the same action id submitted with a different payload,
	 *                    up to {@value #MAX_FINGERPRINT_LENGTH} characters so it is stored the same way on every database
	 */
	public Action(@NonNull ActionId actionId, String fingerprint) {
		if (fingerprint != null && fingerprint.length() > MAX_FINGERPRINT_LENGTH) {
			throw new IllegalArgumentException("Fingerprint must not be longer than " + MAX_FINGERPRINT_LENGTH + " characters");
		}
		this.actionId = actionId;
		this.createdAt = ClockKeeper.now();
		this.fingerprint = fingerprint;
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

	/**
	 * Fingerprints are only compared when both sides have one, actions persisted without a fingerprint never mismatch.
	 */
	public boolean hasDifferentFingerprint(String fingerprint) {
		return this.fingerprint != null && fingerprint != null && !this.fingerprint.equals(fingerprint);
	}

	public Optional<String> getFingerprint() {
		return Optional.ofNullable(fingerprint);
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
