package com.transferwise.idempotence4j.core

import com.fasterxml.jackson.core.type.TypeReference
import com.transferwise.idempotence4j.core.exception.ActionPayloadMismatchException
import spock.lang.Specification
import spock.lang.Subject

import static com.transferwise.idempotence4j.factory.ActionTestFactory.anActionId

class EphemeralIdempotenceServiceTest extends Specification {
    @Subject
    def service = new EphemeralIdempotenceService()

    def "should execute procedure once and return stored result on retry"() {
        given:
            def actionId = anActionId()
            def executions = 0
            def procedure = { -> ++executions }
        when:
            def first = service.execute(actionId, procedure, new TypeReference<Integer>() {})
            def retry = service.execute(actionId, procedure, new TypeReference<Integer>() {})
        then:
            first == 1
            retry == 1
            executions == 1
    }

    def "should reject retry with a different fingerprint"() {
        given:
            def actionId = anActionId()
        and:
            service.execute(actionId, "fp-1", { -> "result" }, new TypeReference<String>() {})
        when:
            service.execute(actionId, "fp-2", { -> "other" }, new TypeReference<String>() {})
        then:
            thrown(ActionPayloadMismatchException)
    }

    def "should return stored result when fingerprint matches or is missing on either side"() {
        given:
            def actionId = anActionId()
        and:
            service.execute(actionId, storedFingerprint, { -> "result" }, new TypeReference<String>() {})
        when:
            def retry = service.execute(actionId, incomingFingerprint, { -> "other" }, new TypeReference<String>() {})
        then:
            retry == "result"
        where:
            storedFingerprint | incomingFingerprint
            "fp-1"            | "fp-1"
            "fp-1"            | null
            null              | "fp-2"
    }
}
