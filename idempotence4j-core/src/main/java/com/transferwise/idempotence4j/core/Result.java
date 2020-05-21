package com.transferwise.idempotence4j.core;

import lombok.NonNull;
import lombok.Value;

@Value
public class Result {
	@NonNull
	private byte[] content;
	@NonNull
	private String type;
}
