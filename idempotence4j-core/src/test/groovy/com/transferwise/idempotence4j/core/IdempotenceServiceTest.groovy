package com.transferwise.idempotence4j.core

import com.fasterxml.jackson.core.type.TypeReference
import spock.lang.Specification

import java.lang.reflect.Type
import java.util.function.Function
import java.util.function.Supplier

import static com.transferwise.idempotence4j.factory.ActionTestFactory.anActionId

class IdempotenceServiceTest extends Specification {

    def "should reject fingerprinted execution for implementations that do not support it"() {
        given:
            IdempotenceService service = new IdempotenceService() {
                @Override
                def <S, R> R execute(ActionId actionId, Function<S, R> onRetry, Supplier<R> procedure, Function<R, S> toRecord, Type recordType) {
                    return procedure.get()
                }
            }
        when:
            service.execute(anActionId(), "fp", { -> "result" }, new TypeReference<String>() {})
        then:
            thrown(UnsupportedOperationException)
    }
}
