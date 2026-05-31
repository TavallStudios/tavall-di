/*
 * TJVD License (TJ Valentine’s Discretionary License) — Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package org.tavall.dependency.access;

import org.tavall.dependency.annotations.VariableTypeArguments;

import java.lang.annotation.Annotation;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Validates dependency-access grant metadata.
 */
public final class DependencyAccessValidationHandler {

    /**
     * Validates a granted access interface and its dependency type payload.
     *
     * @param accessType the access interface token
     * @param dependencyTypes the dependency types granted to the access interface
     */
    public void validateGrant(Class<?> accessType, Class<?>[] dependencyTypes) {
        if (accessType == null) {
            throw new IllegalArgumentException("accessType is required");
        }
        if (!accessType.isInterface()) {
            throw new IllegalArgumentException("accessType must be an interface: " + accessType.getName());
        }
        if (!hasVariableTypeArguments(accessType)) {
            throw new IllegalStateException(
                    "accessType must have or inherit @VariableTypeArguments: " + accessType.getName());
        }

        Class<?>[] resolvedDependencyTypes = dependencyTypes == null ? new Class<?>[0] : dependencyTypes;
        Set<Class<?>> seenDependencyTypes = new LinkedHashSet<>();
        for (Class<?> dependencyType : resolvedDependencyTypes) {
            if (dependencyType == null) {
                throw new IllegalArgumentException("dependencyTypes may not contain null values for "
                        + accessType.getName());
            }
            if (!seenDependencyTypes.add(dependencyType)) {
                throw new IllegalArgumentException("duplicate dependency type within access interface "
                        + accessType.getName() + ": " + dependencyType.getName());
            }
        }
    }

    /**
     * Checks whether the supplied access interface or one of its parents is variable-typed.
     *
     * @param accessType the access interface token to inspect
     * @return {@code true} when the interface is valid for expansion
     */
    public boolean hasVariableTypeArguments(Class<?> accessType) {
        if (accessType == null || !accessType.isInterface()) {
            return false;
        }
        if (accessType.isAnnotationPresent(VariableTypeArguments.class)) {
            return true;
        }

        Class<?>[] parentInterfaces = accessType.getInterfaces();
        for (Class<?> parentInterface : parentInterfaces) {
            if (hasVariableTypeArguments(parentInterface)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Ensures the supplied annotation payload is stable and normalized.
     *
     * @param accessType the access interface token
     * @param dependencyTypes the dependency types granted to the access interface
     * @return the validated dependency type set in declaration order
     */
    public Set<Class<?>> normalizeDependencyTypes(Class<?> accessType, Class<?>[] dependencyTypes) {
        validateGrant(accessType, dependencyTypes);

        Set<Class<?>> normalizedDependencyTypes = new LinkedHashSet<>();
        Class<?>[] resolvedDependencyTypes = dependencyTypes == null ? new Class<?>[0] : dependencyTypes;
        for (Class<?> dependencyType : resolvedDependencyTypes) {
            normalizedDependencyTypes.add(Objects.requireNonNull(dependencyType, "dependencyType"));
        }
        return normalizedDependencyTypes;
    }

    /**
     * Determines whether the supplied annotation is a direct grant annotation.
     *
     * @param annotation the annotation to inspect
     * @return {@code true} when the annotation is a grant annotation
     */
    public boolean isGrantedDependencyAccess(Annotation annotation) {
        return annotation != null
                && "org.tavall.dependency.annotations.GrantedDependencyAccess".equals(
                annotation.annotationType().getName());
    }
}
