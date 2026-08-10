/*
 * TJVD License (TJ Valentine’s Discretionary License) — Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package org.tavall.dependency;

import org.tavall.dependency.maps.DependencyMap;
import org.tavall.dependency.maps.interfaces.IDependencyMap;
import org.tavall.dependency.metadata.interfaces.IDependencyMetaData;

import java.util.function.Supplier;

/**
 * Shared dependency access backed by the authoritative dependency map.
 */
public interface IDependencyAccess {

    /**
     * Returns the map that owns dependency metadata for this access object.
     */
    default IDependencyMap getDependencyMap() {
        return DependencyMap.getDependencyMap();
    }

    /**
     * Returns the global compatibility loader.
     *
     * @deprecated Normal production lookup should use {@link #getDependencyMap()}.
     */
    @Deprecated(forRemoval = false)
    default DependencyLoader getDependencyLoader() {
        return DependencyLoader.getDependencyLoader();
    }

    /**
     * Returns a named compatibility loader.
     */
    default DependencyLoader getDependencyLoader(String scopeName) {
        return DependencyLoader.getDependencyLoader(scopeName);
    }

    default <T> IDependencyMetaData<?, ?> findMetaData(Class<T> dependencyType) {
        if (dependencyType == null) {
            return null;
        }

        IDependencyMetaData<?, ?> localMetaData = resolveLocalMetaData(dependencyType);
        return localMetaData != null
                ? localMetaData
                : getDependencyMap().findMetaData(dependencyType);
    }

    default boolean isInstanceRegistered(Class<?> dependencyType) {
        return dependencyType != null && getDependencyMap().isInstanceRegistered(dependencyType);
    }

    default <T> T findInstance(Class<T> dependencyType) {
        if (dependencyType == null) {
            return null;
        }

        Object localDependency = resolveLocalInstance(dependencyType);
        if (dependencyType.isInstance(localDependency)) {
            return dependencyType.cast(localDependency);
        }

        return getDependencyMap().findInstance(dependencyType);
    }

    /**
     * Resolves a required dependency from local state or the owning map.
     */
    default <T> T getInstance(Class<T> dependencyType) {
        if (dependencyType == null) {
            throw new IllegalArgumentException("dependencyType is required");
        }

        Object localDependency = resolveLocalInstance(dependencyType);
        if (dependencyType.isInstance(localDependency)) {
            return dependencyType.cast(localDependency);
        }

        return getDependencyMap().getInstance(dependencyType);
    }

    /**
     * @deprecated Use {@link #getInstance(Class)}.
     */
    @Deprecated(forRemoval = false)
    default <T> T requireInstance(Class<T> dependencyType) {
        return getInstance(dependencyType);
    }

    default <T> T replaceInstance(Class<T> dependencyType, Supplier<? extends T> supplier) {
        return getDependencyMap().replaceInstance(dependencyType, supplier);
    }

    default <T> T registerInstance(Class<T> dependencyType, T dependencyInstance) {
        return getDependencyMap().registerInstance(dependencyType, dependencyInstance);
    }

    default <T> T registerInstance(Class<T> dependencyType, Supplier<? extends T> supplier) {
        return getDependencyMap().registerInstance(dependencyType, supplier);
    }

    default Object resolveLocalInstance(Class<?> dependencyType) {
        return null;
    }

    default IDependencyMetaData<?, ?> resolveLocalMetaData(Class<?> dependencyType) {
        return null;
    }
}
