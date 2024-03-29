package com.transferwise.idempotence4j.factory

import com.transferwise.idempotence4j.core.Action
import com.transferwise.idempotence4j.core.ActionId
import com.transferwise.idempotence4j.core.ClockKeeper
import com.transferwise.idempotence4j.core.Result
import groovy.json.JsonOutput
import groovy.transform.EqualsAndHashCode
import groovy.transform.TupleConstructor

import java.nio.charset.StandardCharsets
import java.time.Instant

class ActionTestFactory {

    static final String TYPE = "ADD"
    static final String CLIENT = "aService"

    static anActionId(
        Map args = [:],
        String key = UUID.randomUUID().toString(),
        String type = TYPE,
        String client = CLIENT
    ) {
        new ActionId(
            args.key as String ?: key,
            args.type as String ?: type,
            args.client as String ?: client,
        )
    }

    static anAction(
        Map args = [:],
        ActionId actionId = anActionId(),
        Boolean isCompleted = false
    ) {
        Action action = new Action(args.actionId as ActionId ?: actionId)

        if(args.containsKey('isCompleted') ? args.isCompleted as Boolean : isCompleted) {
            action.started()
            action.completed(
                args.result as Result ?: anActionResult()
            )
        }
        return action
    }

    static anActionResult(
        Map args = [:],
        byte[] content = JsonOutput.toJson(aResult()).getBytes(StandardCharsets.UTF_8),
        String type = "json"
    ) {
        new Result(
            args.content as String ?: content,
            args.type as String ?: type
        )
    }

    static aResult() {
        new TestResult("name", 30, ClockKeeper.now())
    }

    @TupleConstructor()
    @EqualsAndHashCode
    static class TestResult {
        String name
        Integer age
        Instant timestamp
    }
}
