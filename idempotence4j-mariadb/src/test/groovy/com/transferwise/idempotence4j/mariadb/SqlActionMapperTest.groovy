package com.transferwise.idempotence4j.mariadb

import spock.lang.Specification
import spock.lang.Subject

import java.sql.ResultSet
import java.sql.Timestamp

import static com.transferwise.idempotence4j.factory.ActionTestFactory.anAction

class SqlActionMapperTest extends Specification {
    @Subject
    def mapper = new SqlActionMapper()

    def "should properly convert action into sql parameter map"() {
        given:
            def action = anAction(isCompleted: true, fingerprint: "fp-1")

        when:
            def parameterMap = mapper.toSql(SqlAction.of(action))

        then:
            parameterMap.getValue('key') == action.actionId.key
            parameterMap.getValue('type') == action.actionId.type
            parameterMap.getValue('client') == action.actionId.client
            parameterMap.getValue('createdAt').getTime() == action.createdAt.toEpochMilli()
            parameterMap.getValue('fingerprint') == "fp-1"
            parameterMap.getValue('lastRunAt').getTime() == action.lastRunAt.get().toEpochMilli()
            parameterMap.getValue('completedAt').getTime() == action.completedAt.get().toEpochMilli()
            parameterMap.getValue('result') == action.result.get().content
            parameterMap.getValue('resultType') == action.result.get().type
    }

    def "should properly map to action from result set"() {
        given:
            def action = anAction(isCompleted: true, fingerprint: "fp-1")

        and:
            def resultSet = Mock(ResultSet)
            resultSet.getLong("seq_id") >> 42L
            resultSet.getString("key") >> action.actionId.key
            resultSet.getString("type") >> action.actionId.type
            resultSet.getString("client") >> action.actionId.client
            resultSet.getTimestamp("created_at") >> new Timestamp(action.createdAt.toEpochMilli())
            resultSet.getString("fingerprint") >> "fp-1"
            resultSet.getTimestamp("last_run_at") >> new Timestamp(action.lastRunAt.get().toEpochMilli())
            resultSet.getTimestamp("completed_at") >> new Timestamp(action.completedAt.get().toEpochMilli())
            resultSet.getBytes("result") >> action.result.get().content
            resultSet.getString("result_type") >> action.result.get().type


        when:
            def entity = mapper.toEntity(resultSet)

        then:
            entity.actionId == action.actionId
            entity.sequentialId.get() == 42L
            entity.createdAt.toEpochMilli() == action.createdAt.toEpochMilli()
            entity.fingerprint.get() == "fp-1"
            entity.lastRunAt.get().toEpochMilli() == action.lastRunAt.get().toEpochMilli()
            entity.completedAt.get().toEpochMilli() == action.completedAt.get().toEpochMilli()
            entity.result.get() == action.result.get()
    }

    def "should map action without fingerprint"() {
        given:
            def resultSet = Mock(ResultSet)
            resultSet.getString("key") >> "key"
            resultSet.getString("type") >> "type"
            resultSet.getString("client") >> "client"
            resultSet.getString("fingerprint") >> null
            resultSet.wasNull() >> true

        when:
            def entity = mapper.toEntity(resultSet)
            def parameterMap = mapper.toSql(SqlAction.of(anAction()))

        then:
            entity.fingerprint.isEmpty()
            parameterMap.getValue('fingerprint') == null
    }
}
