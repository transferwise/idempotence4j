package com.transferwise.idempotence4j.jdbc.mapper

import spock.lang.Specification
import spock.lang.Subject

import java.sql.ResultSet
import java.sql.Timestamp

import static com.transferwise.idempotence4j.factory.ActionTestFactory.anAction

class ActionSqlMapperTest extends Specification {
    @Subject
    def mapper = new ActionSqlMapper()

    def "should properly convert action into sql parameter map"() {
        given:
            def action = anAction(isCompleted: true)

        when:
            def parameterMap = mapper.toSql(action)

        then:
            parameterMap.getValue('key') == action.actionId.key
            parameterMap.getValue('type') == action.actionId.type
            parameterMap.getValue('client') == action.actionId.client
            parameterMap.getValue('createdAt').getTime() == action.createdAt.toEpochMilli()
            parameterMap.getValue('lastRunAt').getTime() == action.lastRunAt.get().toEpochMilli()
            parameterMap.getValue('completedAt').getTime() == action.completedAt.get().toEpochMilli()
            parameterMap.getValue('result') == action.result.get().content
            parameterMap.getValue('resultType') == action.result.get().type
    }

    def "should properly map to action from result set"() {
        given:
            def action = anAction(isCompleted: true)

        and:
            def resultSet = Mock(ResultSet)
            resultSet.getString("key") >> action.actionId.key
            resultSet.getString("type") >> action.actionId.type
            resultSet.getString("client") >> action.actionId.client
            resultSet.getTimestamp("created_at") >> new Timestamp(action.createdAt.toEpochMilli())
            resultSet.getTimestamp("last_run_at") >> new Timestamp(action.lastRunAt.get().toEpochMilli())
            resultSet.getTimestamp("completed_at") >> new Timestamp(action.completedAt.get().toEpochMilli())
            resultSet.getBytes("result") >> action.result.get().content
            resultSet.getString("result_type") >> action.result.get().type


        when:
            def entity = mapper.toEntity(resultSet)

        then:
            entity.actionId == action.actionId
            entity.createdAt.toEpochMilli() == action.createdAt.toEpochMilli()
            entity.lastRunAt.get().toEpochMilli() == action.lastRunAt.get().toEpochMilli()
            entity.completedAt.get().toEpochMilli() == action.completedAt.get().toEpochMilli()
            entity.result.get() == action.result.get()
    }
}
