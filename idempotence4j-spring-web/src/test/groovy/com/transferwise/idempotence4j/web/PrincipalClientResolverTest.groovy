package com.transferwise.idempotence4j.web

import org.springframework.mock.web.MockHttpServletRequest
import spock.lang.Specification
import spock.lang.Subject

import java.security.Principal

class PrincipalClientResolverTest extends Specification {
    @Subject
    def resolver = new PrincipalClientResolver()

    def "should use the authenticated principal name as client"() {
        given:
            def request = new MockHttpServletRequest()
            request.userPrincipal = { "alice" } as Principal
        expect:
            resolver.resolve(request) == "alice"
    }

    def "should use anonymous client for unauthenticated requests"() {
        expect:
            resolver.resolve(new MockHttpServletRequest()) == PrincipalClientResolver.ANONYMOUS
    }
}
