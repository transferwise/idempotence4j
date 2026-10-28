package com.transferwise.idempotence4j.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.transferwise.idempotence4j.core.serializers.json.JsonResultSerializer
import org.springframework.core.ResolvableType
import org.springframework.http.ResponseEntity
import spock.lang.Specification

class StoredResponseTest extends Specification {

    def "should restore status, headers and body of a response entity"() {
        given:
            def entity = ResponseEntity.status(201)
                .header("Location", "/items/1")
                .header("X-Custom", "first", "second")
                .body(new TestItem("book", 1))
        when:
            def restored = StoredResponse.of(entity).toResponseEntity()
        then:
            restored.statusCode.value() == 201
            restored.headers.getFirst("Location") == "/items/1"
            restored.headers.getValuesAsList("X-Custom") == ["first", "second"]
            restored.body == new TestItem("book", 1)
    }

    def "should survive json persistence with a typed body"() {
        given:
            def serializer = new JsonResultSerializer(new ObjectMapper())
            def recordType = ResolvableType.forClassWithGenerics(StoredResponse, TestItem).type
            def stored = StoredResponse.of(ResponseEntity.status(201).header("Location", "/items/1").body(new TestItem("book", 1)))
        when:
            StoredResponse<TestItem> restored = serializer.deserialize(serializer.serialize(stored), recordType)
        then:
            restored.status == 201
            restored.headers == [Location: ["/items/1"]]
            restored.body instanceof TestItem
            restored.body == new TestItem("book", 1)
    }

    def "should restore a response entity without body"() {
        when:
            def restored = StoredResponse.of(ResponseEntity.noContent().build()).toResponseEntity()
        then:
            restored.statusCode.value() == 204
            restored.body == null
    }
}
