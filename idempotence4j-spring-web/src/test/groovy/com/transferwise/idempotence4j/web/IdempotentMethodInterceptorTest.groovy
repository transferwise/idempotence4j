package com.transferwise.idempotence4j.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.transferwise.idempotence4j.core.ActionId
import com.transferwise.idempotence4j.core.EphemeralIdempotenceService
import com.transferwise.idempotence4j.core.IdempotenceService
import com.transferwise.idempotence4j.core.exception.ConflictingActionException
import org.springframework.aop.framework.ProxyFactory
import org.springframework.aop.support.DefaultPointcutAdvisor
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import spock.lang.Specification

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post

class IdempotentMethodInterceptorTest extends Specification {
    static final String KEY_HEADER = "Idempotency-Key"

    def objectMapper = new ObjectMapper()
    def controller = new TestController()
    IdempotenceService idempotenceService = new EphemeralIdempotenceService()
    MockMvc mockMvc

    def setup() {
        mockMvc = mockMvcFor(idempotenceService)
    }

    def "should execute handler once and replay persisted response on retry"() {
        when:
            def first = postItem("/items", "key-1", "book")
            def retry = postItem("/items", "key-1", "book")
        then:
            first.andReturn().response.status == 200
            first.andReturn().response.getHeader(IdempotentMethodInterceptor.REPLAYED_HEADER) == null
            retry.andReturn().response.status == 200
            retry.andReturn().response.getHeader(IdempotentMethodInterceptor.REPLAYED_HEADER) == "true"
            bodyOf(retry) == bodyOf(first)
            bodyOf(first) == new TestItem("book", 1)
            controller.executions.get() == 1
    }

    def "should execute handler for each distinct key"() {
        when:
            postItem("/items", "key-1", "book")
            postItem("/items", "key-2", "book")
        then:
            controller.executions.get() == 2
    }

    def "should replay status, headers and body of a response entity"() {
        when:
            def first = postItem("/entities", "key-1", "book").andReturn().response
            def retry = postItem("/entities", "key-1", "book").andReturn().response
        then:
            first.status == 201
            retry.status == 201
            retry.getHeader("Location") == "/entities/1"
            retry.getHeaders("X-Custom") == ["first", "second"]
            retry.contentAsString == first.contentAsString
            controller.executions.get() == 1
    }

    def "should replay a void handler with its response status"() {
        when:
            def first = mockMvc.perform(delete("/items/7").header(KEY_HEADER, "key-1")).andReturn().response
            def retry = mockMvc.perform(delete("/items/7").header(KEY_HEADER, "key-1")).andReturn().response
        then:
            first.status == 204
            retry.status == 204
            retry.getHeader(IdempotentMethodInterceptor.REPLAYED_HEADER) == "true"
            controller.executions.get() == 1
    }

    def "should reject request without idempotency key when it is required"() {
        when:
            def response = postItem("/items", null, "book").andReturn().response
        then:
            response.status == 400
            response.errorMessage == "Missing Idempotency-Key header"
            controller.executions.get() == 0
    }

    def "should execute handler without idempotency when key is optional and missing"() {
        when:
            postItem("/optional", null, "book")
            postItem("/optional", null, "book")
        then:
            controller.executions.get() == 2
    }

    def "should reject idempotency key longer than 255 characters"() {
        when:
            def response = postItem("/items", "k" * 256, "book").andReturn().response
        then:
            response.status == 400
            controller.executions.get() == 0
    }

    def "should reject the same key reused with a different body"() {
        given:
            postItem("/items", "key-1", "book")
        when:
            def response = postItem("/items", "key-1", "pen").andReturn().response
        then:
            response.status == 422
            controller.executions.get() == 1
    }

    def "should reject the same key reused for a different path"() {
        given:
            mockMvc.perform(delete("/items/1").header(KEY_HEADER, "key-1"))
        when:
            def response = mockMvc.perform(delete("/items/2").header(KEY_HEADER, "key-1")).andReturn().response
        then:
            response.status == 422
            controller.executions.get() == 1
    }

    def "should reject request while another request with the same key is in progress"() {
        given:
            def service = Mock(IdempotenceService) {
                execute(*_) >> { throw new ConflictingActionException("Request already in progress") }
            }
            def mockMvc = mockMvcFor(service)
        when:
            def response = mockMvc.perform(post("/items").header(KEY_HEADER, "key-1")
                .contentType(MediaType.APPLICATION_JSON).content('{"name":"book"}')).andReturn().response
        then:
            response.status == 409
    }

    def "should not persist response when handler throws and execute it again on retry"() {
        given:
            controller.failNext.set(true)
        when:
            def failed = postItem("/items", "key-1", "book").andReturn().response
            def retry = postItem("/items", "key-1", "book").andReturn().response
        then:
            failed.status == 502
            retry.status == 200
            retry.getHeader(IdempotentMethodInterceptor.REPLAYED_HEADER) == null
            controller.executions.get() == 2
    }

    def "should propagate checked exceptions to exception handlers unchanged"() {
        when:
            def response = postItem("/checked", "key-1", "book").andReturn().response
        then:
            response.status == 503
            controller.handledException.get() == IOException
    }

    def "should build action id from key, action type and client"() {
        given:
            def service = Mock(IdempotenceService)
            def mockMvc = mockMvcFor(service)
        when:
            mockMvc.perform(post(path).header(KEY_HEADER, "key-1")
                .contentType(MediaType.APPLICATION_JSON).content('{"name":"book"}'))
        then:
            1 * service.execute({ ActionId it ->
                it.key == "key-1" && it.type == type && it.client == PrincipalClientResolver.ANONYMOUS
            }, { String it -> it.length() == 64 }, *_) >> new TestItem("book", 1)
        where:
            path        || type
            "/items"    || "TestController.create"
            "/optional" || "optional-create"
    }

    def "should execute handler directly outside of a web request"() {
        given:
            def service = Mock(IdempotenceService)
            TestController proxy = proxied(service)
        when:
            def result = proxy.create(new TestItem("book", null))
        then:
            result == new TestItem("book", 1)
            0 * service._
    }

    private ResultActions postItem(String path, String key, String name) {
        def request = post(path).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(new TestItem(name, null)))
        if (key != null) {
            request.header(KEY_HEADER, key)
        }
        mockMvc.perform(request)
    }

    private TestItem bodyOf(ResultActions result) {
        objectMapper.readValue(result.andReturn().response.contentAsString, TestItem)
    }

    def "should apply @Idempotent declared on a controller interface"() {
        given:
            def apiController = new TestApiController()
            def factory = new ProxyFactory(apiController)
            factory.proxyTargetClass = true
            factory.addAdvisor(new DefaultPointcutAdvisor(
                new IdempotentPointcut(),
                new IdempotentMethodInterceptor({ idempotenceService }, { new PrincipalClientResolver() }, { KEY_HEADER }, { objectMapper })
            ))
            def mockMvc = MockMvcBuilders.standaloneSetup(factory.proxy).build()
        when:
            def first = postTo(mockMvc, "/api/items", "key-1", "book")
            def retry = postTo(mockMvc, "/api/items", "key-1", "book")
            def mismatch = postTo(mockMvc, "/api/items", "key-1", "pen")
        then:
            first.status == 200
            retry.getHeader(IdempotentMethodInterceptor.REPLAYED_HEADER) == "true"
            retry.contentAsString == first.contentAsString
            mismatch.status == 422
            apiController.executions.get() == 1
    }

    private MockHttpServletResponse postTo(MockMvc mockMvc, String path, String key, String name) {
        mockMvc.perform(post(path)
            .header(KEY_HEADER, key)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new TestItem(name, null))))
            .andReturn()
            .response
    }

    private MockMvc mockMvcFor(IdempotenceService service) {
        MockMvcBuilders.standaloneSetup(proxied(service)).build()
    }

    private Object proxied(IdempotenceService service) {
        def factory = new ProxyFactory(controller)
        factory.proxyTargetClass = true
        factory.addAdvisor(new DefaultPointcutAdvisor(
            new IdempotentPointcut(),
            new IdempotentMethodInterceptor({ service }, { new PrincipalClientResolver() }, { KEY_HEADER }, { objectMapper })
        ))
        factory.proxy
    }
}
