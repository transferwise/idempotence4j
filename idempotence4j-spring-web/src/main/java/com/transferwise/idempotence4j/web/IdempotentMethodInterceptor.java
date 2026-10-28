package com.transferwise.idempotence4j.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.transferwise.idempotence4j.core.ActionId;
import com.transferwise.idempotence4j.core.IdempotenceService;
import com.transferwise.idempotence4j.core.exception.ActionPayloadMismatchException;
import com.transferwise.idempotence4j.core.exception.ConflictingActionException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

/**
 * Executes {@link Idempotent} handler methods through {@link IdempotenceService}. The method return value is the
 * persisted record, a {@link ResponseEntity} is persisted with its status and headers as {@link StoredResponse}.
 */
public class IdempotentMethodInterceptor implements MethodInterceptor {
    public static final String REPLAYED_HEADER = "Idempotent-Replayed";
    private static final int MAX_KEY_LENGTH = 255;
    private static final ObjectMapper DEFAULT_OBJECT_MAPPER = JsonMapper.builder().findAndAddModules().build();

    // resolved on first use, the advisor holding this interceptor is created before regular beans
    private final Supplier<IdempotenceService> idempotenceService;
    private final Supplier<IdempotentClientResolver> clientResolver;
    private final Supplier<String> keyHeader;
    private final Supplier<ObjectMapper> objectMapper;

    private final Map<Method, List<Integer>> requestBodyIndexes = new ConcurrentHashMap<>();

    public IdempotentMethodInterceptor(
        Supplier<IdempotenceService> idempotenceService,
        Supplier<IdempotentClientResolver> clientResolver,
        Supplier<String> keyHeader,
        Supplier<ObjectMapper> objectMapper
    ) {
        this.idempotenceService = idempotenceService;
        this.clientResolver = clientResolver;
        this.keyHeader = keyHeader;
        this.objectMapper = objectMapper;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (!(requestAttributes instanceof ServletRequestAttributes attributes)) {
            return invocation.proceed();
        }

        Method method = AopUtils.getMostSpecificMethod(invocation.getMethod(), AopUtils.getTargetClass(invocation.getThis()));
        Idempotent idempotent = AnnotatedElementUtils.findMergedAnnotation(method, Idempotent.class);
        if (idempotent == null) {
            return invocation.proceed();
        }

        HttpServletRequest request = attributes.getRequest();
        String headerName = keyHeader.get();
        String key = request.getHeader(headerName);
        if (!StringUtils.hasText(key)) {
            if (idempotent.required()) {
                throw new IdempotentRequestException(BAD_REQUEST, "Missing " + headerName + " header");
            }
            return invocation.proceed();
        }
        if (key.length() > MAX_KEY_LENGTH) {
            throw new IdempotentRequestException(BAD_REQUEST, headerName + " header must not be longer than " + MAX_KEY_LENGTH + " characters");
        }

        ActionId actionId = new ActionId(key, actionType(idempotent, method), clientResolver.get().resolve(request));
        // the application's mapper when there is one, so request bodies are fingerprinted with its modules
        ObjectMapper mapper = Objects.requireNonNullElse(objectMapper.get(), DEFAULT_OBJECT_MAPPER);
        String fingerprint = RequestFingerprint.of(request, requestBodies(invocation, method), mapper);

        try {
            return execute(invocation, method, actionId, fingerprint, attributes.getResponse());
        } catch (ProcedureFailure failure) {
            throw failure.getCause();
        } catch (ConflictingActionException ex) {
            throw new IdempotentRequestException(CONFLICT, "A request with the same idempotency key is in progress", ex);
        } catch (ActionPayloadMismatchException ex) {
            throw new IdempotentRequestException(UNPROCESSABLE_ENTITY, "Idempotency key was already used for a different request", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private Object execute(MethodInvocation invocation, Method method, ActionId actionId, String fingerprint, HttpServletResponse response) {
        ResolvableType returnType = ResolvableType.forMethodReturnType(method);

        if (ResponseEntity.class.isAssignableFrom(returnType.toClass())) {
            Type recordType = ResolvableType.forClassWithGenerics(StoredResponse.class, bodyType(returnType)).getType();
            return idempotenceService.get().execute(
                actionId,
                fingerprint,
                (StoredResponse<Object> stored) -> replayed(response, stored.toResponseEntity()),
                () -> proceed(invocation),
                result -> StoredResponse.of((ResponseEntity<Object>) result),
                recordType
            );
        }

        Type recordType = returnType.toClass() == void.class ? Object.class : returnType.getType();
        return idempotenceService.get().execute(
            actionId,
            fingerprint,
            stored -> replayed(response, stored),
            () -> proceed(invocation),
            Function.identity(),
            recordType
        );
    }

    private ResolvableType bodyType(ResolvableType returnType) {
        ResolvableType bodyType = returnType.as(ResponseEntity.class).getGeneric(0);
        Type type = bodyType.getType();
        // wildcards and type variables are not deserializable, fall back to their resolved class
        if (type instanceof Class || type instanceof ParameterizedType) {
            return bodyType;
        }
        return ResolvableType.forClass(bodyType.toClass());
    }

    private static Object replayed(HttpServletResponse response, Object result) {
        if (response != null) {
            response.setHeader(REPLAYED_HEADER, "true");
        }
        return result;
    }

    private static Object proceed(MethodInvocation invocation) {
        try {
            return invocation.proceed();
        } catch (RuntimeException | Error ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new ProcedureFailure(ex);
        }
    }

    private List<Object> requestBodies(MethodInvocation invocation, Method method) {
        List<Integer> indexes = requestBodyIndexes.computeIfAbsent(method, key -> {
            // HandlerMethod parameters also see @RequestBody declared on interface methods
            MethodParameter[] parameters = new HandlerMethod(invocation.getThis(), key).getMethodParameters();
            List<Integer> bodyIndexes = new ArrayList<>();
            for (MethodParameter parameter : parameters) {
                if (parameter.hasParameterAnnotation(RequestBody.class)) {
                    bodyIndexes.add(parameter.getParameterIndex());
                }
            }
            return bodyIndexes;
        });

        List<Object> bodies = new ArrayList<>();
        for (Integer index : indexes) {
            bodies.add(invocation.getArguments()[index]);
        }
        return bodies;
    }

    private static String actionType(Idempotent idempotent, Method method) {
        if (StringUtils.hasText(idempotent.type())) {
            return idempotent.type();
        }
        return method.getDeclaringClass().getSimpleName() + "." + method.getName();
    }

    /**
     * Carries checked exceptions thrown by the handler method through the {@link Supplier} based procedure
     */
    private static class ProcedureFailure extends RuntimeException {
        ProcedureFailure(Throwable cause) {
            super(cause);
        }
    }
}
