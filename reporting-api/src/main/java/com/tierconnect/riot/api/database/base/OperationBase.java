package com.tierconnect.riot.api.database.base;

import com.tierconnect.riot.api.database.base.annotations.ClassesAllowed;
import com.tierconnect.riot.api.database.exception.ValueNotPermittedException;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Created by vealaro on 12/29/16.
 */
public abstract class OperationBase {

    protected static Map<String, Method> getMethodList(Class<?> clazz) {
        Map<String, Method> mapMethod = new LinkedHashMap<>();
        for (Method method : clazz.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers()) && Modifier.isStatic(method.getModifiers())) {
                mapMethod.put(method.getName(), method);
            }
        }
        return Collections.unmodifiableMap(mapMethod);
    }

    protected static Object twoValues(Object startValue, Object endValue) {
        List<Object> values = new ArrayList<>(2);
        values.add(startValue);
        values.add(endValue);
        return values;
    }

    protected static void checkValuePermitted(String key, Object value, Map<String, Method> methodList) throws ValueNotPermittedException {
        String methodName = Thread.currentThread().getStackTrace()[2].getMethodName();
        if (methodList.get(methodName) != null && methodList.get(methodName).isAnnotationPresent(ClassesAllowed.class)) {
            ClassesAllowed allowed = methodList.get(methodName).getAnnotation(ClassesAllowed.class);
            if (!(Arrays.asList(allowed.listClass()).contains(value.getClass()))) {
                throw new ValueNotPermittedException(String.format("this is object [%s] of class [%s] not permitted in method [%s] with key [%s]", value, value.getClass(), methodName, key));
            }
        }
    }
}
