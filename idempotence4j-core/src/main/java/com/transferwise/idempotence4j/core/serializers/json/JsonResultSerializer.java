package com.transferwise.idempotence4j.core.serializers.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transferwise.idempotence4j.core.ResultSerializer;

import java.io.IOException;

public class JsonResultSerializer implements ResultSerializer {
    private final ObjectMapper objectMapper;

    public JsonResultSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public <T> byte[] serialize(T result) throws IOException {
        return objectMapper.writer().writeValueAsBytes(result);
    }

    @Override
    public <T> T deserialize(byte[] payload) throws IOException {
        return objectMapper.readValue(payload, new TypeReference<T>() {});
    }

    @Override
    public String getType() {
        return "json";
    }
}
