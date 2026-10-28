package com.transferwise.idempotence4j.core

import spock.lang.Specification

import static com.transferwise.idempotence4j.factory.ActionTestFactory.anActionId

class ActionTest extends Specification {

    def "should only report a different fingerprint when both fingerprints are present and differ"() {
        given:
            def action = new Action(anActionId(), storedFingerprint)
        expect:
            action.hasDifferentFingerprint(incomingFingerprint) == isDifferent
        where:
            storedFingerprint | incomingFingerprint || isDifferent
            "fp-1"            | "fp-2"              || true
            "fp-1"            | "fp-1"              || false
            "fp-1"            | null                || false
            null              | "fp-2"              || false
            null              | null                || false
    }

    def "should not have a fingerprint when created without one"() {
        when:
            def action = new Action(anActionId())
        then:
            action.fingerprint.isEmpty()
    }

    def "should accept a fingerprint up to the maximum length"() {
        when:
            def action = new Action(anActionId(), "a" * Action.MAX_FINGERPRINT_LENGTH)
        then:
            action.fingerprint.get().length() == Action.MAX_FINGERPRINT_LENGTH
    }

    def "should reject a fingerprint longer than the maximum length"() {
        when:
            new Action(anActionId(), "a" * (Action.MAX_FINGERPRINT_LENGTH + 1))
        then:
            def ex = thrown(IllegalArgumentException)
            ex.message == "Fingerprint must not be longer than 255 characters"
    }
}
