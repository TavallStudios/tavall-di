/*
 * TJVD License (TJ Valentine’s Discretionary License) — Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package org.tavall.dependency;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Typed dependency access contract after source lowering.
 *
 * @param <ACCESS> an authored dependency token or generated access type
 */
public interface DependencyAccess<ACCESS> extends IDependencyAccess {

    /**
     * Returns the metadata-owned access instance for this declaration.
     */
    default ACCESS getInstance() {
        return getDependencyMap().getInstance(getDependencyAccessType());
    }

    /**
     * Resolves the single runtime access token retained after source lowering.
     */
    @SuppressWarnings("unchecked")
    default Class<ACCESS> getDependencyAccessType() {
        Class<?> accessType = resolveDependencyAccessType(getClass());
        if (accessType == null) {
            throw new IllegalStateException(
                    "Unable to resolve DependencyAccess type for " + getClass().getName());
        }
        return (Class<ACCESS>) accessType;
    }

    private static Class<?> resolveDependencyAccessType(Class<?> concreteType) {
        Class<?> currentType = concreteType;
        while (currentType != null && currentType != Object.class) {
            for (Type genericInterface : currentType.getGenericInterfaces()) {
                Class<?> resolved = resolveDependencyAccessType(genericInterface);
                if (resolved != null) {
                    return resolved;
                }
            }
            currentType = currentType.getSuperclass();
        }
        return null;
    }

    private static Class<?> resolveDependencyAccessType(Type candidateType) {
        if (candidateType instanceof ParameterizedType parameterizedType) {
            Type rawType = parameterizedType.getRawType();
            if (rawType == DependencyAccess.class) {
                Type accessType = parameterizedType.getActualTypeArguments()[0];
                if (accessType instanceof Class<?> rawAccessType) {
                    return rawAccessType;
                }
                if (accessType instanceof ParameterizedType parameterizedAccessType
                        && parameterizedAccessType.getRawType() instanceof Class<?> rawAccessType) {
                    return rawAccessType;
                }
                return null;
            }
            if (rawType instanceof Class<?> rawClass) {
                return resolveDependencyAccessType(rawClass);
            }
        }
        if (candidateType instanceof Class<?> rawClass) {
            return resolveDependencyAccessType(rawClass);
        }
        return null;
    }
}
