package com.transferwise.idempotence4j.core;

import lombok.NonNull;
import lombok.Value;

import java.util.UUID;

@Value
public class ActionId {
	private String key;
	private String type;
	private String client;

	public ActionId(@NonNull String key, @NonNull String type, @NonNull String client) {
		this.key = key;
		this.type = type;
		this.client = client;
	}

	public ActionId(@NonNull UUID uuid, String type, String client) {
		this(uuid.toString(), type, client);
	}

	public ActionId(@NonNull Long id, String type, String client) {
		this(Long.toString(id), type, client);
	}
}
