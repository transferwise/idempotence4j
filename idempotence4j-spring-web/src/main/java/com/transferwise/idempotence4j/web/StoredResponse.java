package com.transferwise.idempotence4j.web;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.ResponseEntity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Persisted form of a {@link ResponseEntity} returned by an idempotent handler method
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StoredResponse<T> {
    private int status;
    private Map<String, List<String>> headers;
    private T body;

    public static <T> StoredResponse<T> of(ResponseEntity<T> responseEntity) {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        // headerSet() exists with the same signature in Spring 6 and 7, HttpHeaders is no longer a Map in Spring 7
        responseEntity.getHeaders().headerSet().forEach(header -> headers.put(header.getKey(), List.copyOf(header.getValue())));
        return new StoredResponse<>(responseEntity.getStatusCode().value(), headers, responseEntity.getBody());
    }

    public ResponseEntity<T> toResponseEntity() {
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(status);
        if (headers != null) {
            headers.forEach((name, values) -> builder.header(name, values.toArray(new String[0])));
        }
        return builder.body(body);
    }
}
