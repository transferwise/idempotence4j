package com.transferwise.idempotence4j.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import jakarta.servlet.http.HttpServletRequest;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * SHA-256 fingerprint of a request: HTTP method, path, request parameters and deserialized request bodies.
 * Bodies are converted to JSON trees with sorted properties, so formatting and field order of the original payload do not matter.
 */
public class RequestFingerprint {
    // writes only maps, lists and scalars produced by the application's mapper, ordering map entries by key
    private static final ObjectMapper CANONICAL_MAPPER = JsonMapper.builder()
        .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
        .build();

    /**
     * @param objectMapper converts the request bodies, so modules and serializers registered by the application apply
     */
    public static String of(HttpServletRequest request, List<Object> bodies, ObjectMapper objectMapper) {
        Map<String, List<String>> parameters = new TreeMap<>();
        request.getParameterMap().forEach((name, values) -> parameters.put(name, Arrays.asList(values)));

        Map<String, Object> content = new LinkedHashMap<>();
        content.put("method", request.getMethod());
        content.put("path", request.getRequestURI());
        content.put("parameters", parameters);

        try {
            content.put("bodies", objectMapper.convertValue(bodies, Object.class));
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(CANONICAL_MAPPER.writeValueAsBytes(content));
            return HexFormat.of().formatHex(digest);
        } catch (IllegalArgumentException | JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize request body for fingerprinting", ex);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
