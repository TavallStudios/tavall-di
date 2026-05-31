package org.tavall.dependency.internal;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Resolves the raw classes behind generic type declarations.
 */
public final class DependencyTypeResolver {

    private DependencyTypeResolver() {
    }

    public static DependencyTypeResult resolveTypeParameters(Class<?> sourceType, Class<?> targetGenericType) {
        if (sourceType == null) {
            throw new IllegalArgumentException("sourceType is required");
        }
        if (targetGenericType == null) {
            throw new IllegalArgumentException("targetGenericType is required");
        }

        Class<?>[] resolved = resolveFromType(sourceType, targetGenericType);
        return new DependencyTypeResult(sourceType, targetGenericType, resolved);
    }

    private static Class<?>[] resolveFromType(Type type, Class<?> targetGenericType) {
        if (type == null) {
            return new Class<?>[0];
        }

        Class<?> rawClass = resolveRawClass(type);
        if (rawClass == null) {
            return new Class<?>[0];
        }

        if (type instanceof ParameterizedType parameterizedType
                && targetGenericType.equals(resolveRawClass(parameterizedType.getRawType()))) {
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            Class<?>[] resolvedArguments = new Class<?>[actualTypeArguments.length];
            for (int i = 0; i < actualTypeArguments.length; i++) {
                resolvedArguments[i] = resolveRawClass(actualTypeArguments[i]);
            }
            return resolvedArguments;
        }

        for (Type interfaceType : rawClass.getGenericInterfaces()) {
            Class<?>[] resolvedFromInterface = resolveFromType(interfaceType, targetGenericType);
            if (resolvedFromInterface.length > 0) {
                return resolvedFromInterface;
            }
        }

        return resolveFromType(rawClass.getGenericSuperclass(), targetGenericType);
    }

    private static Class<?> resolveRawClass(Type type) {
        if (type instanceof Class<?> rawClass) {
            return rawClass;
        }

        if (type instanceof ParameterizedType parameterizedType
                && parameterizedType.getRawType() instanceof Class<?> rawClass) {
            return rawClass;
        }

        return null;
    }
}
