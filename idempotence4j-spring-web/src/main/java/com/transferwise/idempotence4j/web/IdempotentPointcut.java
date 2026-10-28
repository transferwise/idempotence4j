package com.transferwise.idempotence4j.web;

import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.request.async.WebAsyncTask;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;
import java.util.stream.Stream;

/**
 * Matches methods annotated with {@link Idempotent}. Matching happens when beans are proxied at startup,
 * so methods returning a type whose response cannot be persisted fail the application startup.
 */
public class IdempotentPointcut extends StaticMethodMatcherPointcut {
    private static final String REACTIVE_PUBLISHER = "org.reactivestreams.Publisher";
    private static final List<Class<?>> UNSUPPORTED_RETURN_TYPES = Stream.concat(
            Stream.of(
                Callable.class,
                Future.class,
                CompletionStage.class,
                DeferredResult.class,
                WebAsyncTask.class,
                ResponseBodyEmitter.class,
                StreamingResponseBody.class,
                Resource.class
            ),
            // reactive streams is optional in a servlet application
            ClassUtils.isPresent(REACTIVE_PUBLISHER, IdempotentPointcut.class.getClassLoader())
                ? Stream.of(ClassUtils.resolveClassName(REACTIVE_PUBLISHER, IdempotentPointcut.class.getClassLoader()))
                : Stream.empty()
        )
        .toList();

    @Override
    public boolean matches(Method method, Class<?> targetClass) {
        if (!AnnotatedElementUtils.hasAnnotation(method, Idempotent.class)) {
            return false;
        }

        verifyReturnType(method);
        return true;
    }

    private void verifyReturnType(Method method) {
        ResolvableType returnType = ResolvableType.forMethodReturnType(method);
        Class<?> responseType = returnType.toClass();
        if (HttpEntity.class.isAssignableFrom(responseType)) {
            if (!ResponseEntity.class.isAssignableFrom(responseType)) {
                throw unsupported(method, responseType);
            }
            responseType = returnType.as(ResponseEntity.class).getGeneric(0).toClass();
        }

        for (Class<?> unsupportedType : UNSUPPORTED_RETURN_TYPES) {
            if (unsupportedType.isAssignableFrom(responseType)) {
                throw unsupported(method, responseType);
            }
        }
    }

    private IllegalStateException unsupported(Method method, Class<?> responseType) {
        return new IllegalStateException("@Idempotent method " + method + " returns " + responseType.getName()
            + ", only synchronous responses that can be serialized are supported");
    }
}
