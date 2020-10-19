package com.transferwise.idempotence4j.benchmarks.presentation;

import com.transferwise.idempotence4j.benchmarks.application.CreateIdempotentOperation;
import com.transferwise.idempotence4j.benchmarks.application.CreateOperation;
import com.transferwise.idempotence4j.benchmarks.domain.model.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.UUID;

@Controller
@RequestMapping(
    value = "/v1/operations",
    produces = MediaType.APPLICATION_JSON_VALUE)
public class OperationController {
    @Autowired
    CreateIdempotentOperation createIdempotentOperation;
    @Autowired
    CreateOperation createOperation;

    @RequestMapping(
        method = RequestMethod.POST,
        consumes = MediaType.APPLICATION_JSON_VALUE,
        headers = "X-idempotence-uuid"
    )
    @ResponseBody
    public Operation create(@RequestHeader("X-idempotence-uuid") UUID idempotenceUUID) {
        return createIdempotentOperation.execute(idempotenceUUID);
    }

    @RequestMapping(
        method = RequestMethod.POST,
        consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseBody
    public Operation create() {
        return createOperation.execute();
    }
}
