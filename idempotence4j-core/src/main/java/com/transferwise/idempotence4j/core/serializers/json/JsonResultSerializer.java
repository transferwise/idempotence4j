package com.transferwise.idempotence4j.core.serializers.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transferwise.idempotence4j.core.ResultSerializer;

import java.io.IOException;
import java.lang.reflect.Type;

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
    public <T> T deserialize(byte[] payload, Type type) throws IOException {
        return objectMapper.readValue(payload, objectMapper.constructType(type));
    }

    @Override
    public String getType() {
        return "json";
    }
}
