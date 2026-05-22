package org.tavall.dependency.internal;

import java.util.Arrays;

/**
 * Result container for resolved generic type arguments.
 */
public final class DependencyTypeResult {
    private final Class<?> sourceType;
    private final Class<?> targetGenericType;
    private final Class<?>[] typeArguments;

    public DependencyTypeResult(Class<?> sourceType, Class<?> targetGenericType, Class<?>[] typeArguments) {
        this.sourceType = sourceType;
        this.targetGenericType = targetGenericType;
        this.typeArguments = typeArguments == null ? new Class<?>[0] : Arrays.copyOf(typeArguments, typeArguments.length);
    }

    public Class<?> getSourceType() {
        return sourceType;
    }

    public Class<?> getTargetGenericType() {
        return targetGenericType;
    }

    public Class<?>[] getTypeArguments() {
        return Arrays.copyOf(typeArguments, typeArguments.length);
    }

    public Class<?> getTypeArgument(int typeArgumentIndex) {
        if (typeArgumentIndex < 0 || typeArgumentIndex >= typeArguments.length) {
            return null;
        }
        return typeArguments[typeArgumentIndex];
    }

    public boolean hasTypeArgument(int typeArgumentIndex) {
        return getTypeArgument(typeArgumentIndex) != null;
    }
}
