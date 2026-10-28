package com.transferwise.idempotence4j.web

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.mock.web.MockHttpServletRequest
import spock.lang.Specification

class RequestFingerprintTest extends Specification {
    static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()

    def "should produce the same fingerprint for the same request"() {
        expect:
            RequestFingerprint.of(request("POST", "/items", [a: "1"]), [[name: "book"]], OBJECT_MAPPER) ==
                RequestFingerprint.of(request("POST", "/items", [a: "1"]), [[name: "book"]], OBJECT_MAPPER)
    }

    def "should ignore body field order and parameter order"() {
        given:
            def first = RequestFingerprint.of(request("POST", "/items", [a: "1", b: "2"]), [[name: "book", price: 10]], OBJECT_MAPPER)
            def second = RequestFingerprint.of(request("POST", "/items", [b: "2", a: "1"]), [[price: 10, name: "book"]], OBJECT_MAPPER)
        expect:
            first == second
    }

    def "should produce a different fingerprint when any request part differs"() {
        given:
            def base = RequestFingerprint.of(request("POST", "/items", [a: "1"]), [[name: "book"]], OBJECT_MAPPER)
        expect:
            RequestFingerprint.of(request(method, path, parameters), bodies, OBJECT_MAPPER) != base
        where:
            method | path      | parameters | bodies
            "PUT"  | "/items"  | [a: "1"]   | [[name: "book"]]
            "POST" | "/items/2"| [a: "1"]   | [[name: "book"]]
            "POST" | "/items"  | [a: "2"]   | [[name: "book"]]
            "POST" | "/items"  | [:]        | [[name: "book"]]
            "POST" | "/items"  | [a: "1"]   | [[name: "pen"]]
            "POST" | "/items"  | [a: "1"]   | []
    }

    def "should produce a hex encoded SHA-256 digest"() {
        expect:
            RequestFingerprint.of(request("POST", "/items", [:]), [], OBJECT_MAPPER) ==~ /[0-9a-f]{64}/
    }

    def "should serialize bodies with the given mapper configuration"() {
        given:
            def mapper = new ObjectMapper().setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
        expect:
            RequestFingerprint.of(request("POST", "/items", [:]), [new FieldOnlyBody("book")], mapper) ==
                RequestFingerprint.of(request("POST", "/items", [:]), [[name: "book"]], OBJECT_MAPPER)
            RequestFingerprint.of(request("POST", "/items", [:]), [new FieldOnlyBody("book")], mapper) !=
                RequestFingerprint.of(request("POST", "/items", [:]), [new FieldOnlyBody("pen")], mapper)
    }

    def "should fail when the mapper cannot serialize a body"() {
        when:
            RequestFingerprint.of(request("POST", "/items", [:]), [new FieldOnlyBody("book")], OBJECT_MAPPER)
        then:
            def ex = thrown(IllegalStateException)
            ex.message == "Failed to serialize request body for fingerprinting"
    }

    private static MockHttpServletRequest request(String method, String path, Map<String, String> parameters) {
        def request = new MockHttpServletRequest(method, path)
        parameters.each { name, value -> request.addParameter(name, value) }
        request
    }
}
