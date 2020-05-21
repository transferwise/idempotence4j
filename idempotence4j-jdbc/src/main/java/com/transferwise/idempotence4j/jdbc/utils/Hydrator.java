package com.transferwise.idempotence4j.jdbc.utils;

import org.springframework.objenesis.Objenesis;
import org.springframework.objenesis.ObjenesisStd;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;

public class Hydrator {
	private static Objenesis objenesis = new ObjenesisStd();

	public static <T> T instantiate(Class<T> type) {
		return objenesis.getInstantiatorOf(type).newInstance();
	}

	public static void hydrateField(String fieldName, Object object, Object value) {
		Field field = ReflectionUtils.findField(object.getClass(), fieldName);
		Assert.notNull(field, "Field must not be null");
		ReflectionUtils.makeAccessible(field);
		ReflectionUtils.setField(field, object, value);
	}

	@SuppressWarnings("unchecked")
	public static <T> T getFieldValue(String fieldName, Object object) {
		Field field = ReflectionUtils.findField(object.getClass(), fieldName);
		Assert.notNull(field, "Field must not be null");
		ReflectionUtils.makeAccessible(field);
		return (T) ReflectionUtils.getField(field, object);
	}
}
