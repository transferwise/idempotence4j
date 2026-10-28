package com.transferwise.idempotence4j.autoconfigure

import com.transferwise.idempotence4j.autoconfigure.service.TestApplication
import com.transferwise.idempotence4j.autoconfigure.service.web.TransferController
import com.transferwise.idempotence4j.web.IdempotentClientResolver
import com.transferwise.idempotence4j.web.IdempotentMethodInterceptor
import com.transferwise.idempotence4j.web.PrincipalClientResolver
import org.flywaydb.core.Flyway
import org.springframework.aop.support.AopUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import spock.lang.Specification

import javax.sql.DataSource
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post

@SpringBootTest(classes = TestApplication)
class IdempotentWebIntegrationTest extends Specification {
    @Autowired
    WebApplicationContext context
    @Autowired
    DataSource dataSource
    @Autowired
    TransferController controller
    @Autowired
    IdempotentClientResolver clientResolver

    MockMvc mockMvc
    JdbcTemplate jdbcTemplate

    def setup() {
        Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/idempotence4j/postgres")
            .load()
            .migrate()
        jdbcTemplate = new JdbcTemplate(dataSource)
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS web_test_transfer(name TEXT)")
        jdbcTemplate.execute("TRUNCATE TABLE web_test_transfer")

        mockMvc = MockMvcBuilders.webAppContextSetup(context).build()
        controller.executions().set(0)
        controller.blockUntilReleased(null, null)
    }

    def "should auto configure idempotent handler methods"() {
        expect:
            AopUtils.isAopProxy(controller)
            clientResolver instanceof PrincipalClientResolver
    }

    def "should persist the response and replay it on retry"() {
        given:
            def key = UUID.randomUUID().toString()
        when:
            def first = createTransfer(key, "rent")
            def retry = createTransfer(key, "rent")
        then:
            first.status == 201
            first.getHeader("Location") == "/transfers/1"
            first.getHeader(IdempotentMethodInterceptor.REPLAYED_HEADER) == null
            retry.status == 201
            retry.getHeader("Location") == "/transfers/1"
            retry.getHeader(IdempotentMethodInterceptor.REPLAYED_HEADER) == "true"
            retry.contentAsString == first.contentAsString
        and:
            controller.executions().get() == 1
            transferCount() == 1
            storedAction(key).completed_at != null
            storedAction(key).fingerprint ==~ /[0-9a-f]{64}/
    }

    def "should reject the same key reused with a different body"() {
        given:
            def key = UUID.randomUUID().toString()
            createTransfer(key, "rent")
        when:
            def response = createTransfer(key, "groceries")
        then:
            response.status == 422
            controller.executions().get() == 1
    }

    def "should reject a request without idempotency key"() {
        when:
            def response = createTransfer(null, "rent")
        then:
            response.status == 400
            controller.executions().get() == 0
    }

    def "should roll back handler writes and execute again on retry when the handler throws"() {
        given:
            def key = UUID.randomUUID().toString()
            controller.failNext()
        when:
            def failed = createTransfer(key, "rent")
        then:
            failed.status == 502
            transferCount() == 0
            storedAction(key).completed_at == null
        when:
            def retry = createTransfer(key, "rent")
        then:
            retry.status == 201
            retry.getHeader(IdempotentMethodInterceptor.REPLAYED_HEADER) == null
            controller.executions().get() == 2
            transferCount() == 1
    }

    def "should reject a request while another request with the same key is in progress"() {
        given:
            def key = UUID.randomUUID().toString()
            def started = new CountDownLatch(1)
            def release = new CountDownLatch(1)
            controller.blockUntilReleased(started, release)
            def pool = Executors.newSingleThreadExecutor()
        and:
            def original = pool.submit({ createTransfer(key, "rent") } as Callable<MockHttpServletResponse>)
            started.await(5, TimeUnit.SECONDS)
        when:
            def concurrent = createTransfer(key, "rent")
        then:
            concurrent.status == 409
        when:
            release.countDown()
        then:
            original.get(5, TimeUnit.SECONDS).status == 201
            controller.executions().get() == 1
        cleanup:
            release.countDown()
            pool.shutdown()
    }

    private MockHttpServletResponse createTransfer(String key, String name) {
        def request = post("/transfers").contentType(MediaType.APPLICATION_JSON).content("""{"name":"${name}"}""")
        if (key != null) {
            request.header("Idempotency-Key", key)
        }
        mockMvc.perform(request).andReturn().response
    }

    private int transferCount() {
        jdbcTemplate.queryForObject("SELECT count(*) FROM web_test_transfer", Integer)
    }

    private Map<String, Object> storedAction(String key) {
        jdbcTemplate.queryForMap("SELECT completed_at, fingerprint FROM idempotent_action WHERE key = ?", key)
    }
}
