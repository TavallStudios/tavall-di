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
 * Shared dependency access contract backed by the authoritative dependency map.
 *
 * <p>Instance and metadata lookup first gives an access implementation the opportunity to resolve
 * dependency-local state through {@link #resolveLocalInstance(Class)} or
 * {@link #resolveLocalMetaData(Class)}. When no compatible local value exists, lookup falls back to
 * {@link #getDependencyMap()}.</p>
 */
public interface IDependencyAccess {

    /**
     * Returns the dependency map used for fallback lookup and registration.
     *
     * @return authoritative shared dependency map
     */
    default IDependencyMap getDependencyMap() {
        return DependencyMap.getDependencyMap();
    }

    /**
     * Returns the global compatibility loader.
     *
     * @return compatibility loader backed by the default dependency map
     * @deprecated Normal production lookup should use {@link #getDependencyMap()} or the direct
     *             access methods on this interface.
     */
    @Deprecated(forRemoval = false)
    default DependencyLoader getDependencyLoader() {
        return DependencyLoader.getDependencyLoader();
    }

    /**
     * Returns the compatibility loader for a named dependency scope.
     *
     * @param scopeName logical scope name; null, blank, and {@code default} resolve to the shared
     *                  default loader
     * @return loader associated with the requested scope
     */
    default DependencyLoader getDependencyLoader(String scopeName) {
        return DependencyLoader.getDependencyLoader(scopeName);
    }

    /**
     * Finds metadata for a dependency token, preferring access-local metadata over the shared map.
     *
     * @param dependencyType token to inspect
     * @param <T> dependency token type
     * @return matching metadata, or {@code null} when the token is null or unavailable
     */
    default <T> IDependencyMetaData<?, ?> findMetaData(Class<T> dependencyType) {
        if (dependencyType == null) {
            return null;
        }

        IDependencyMetaData<?, ?> localMetaData = resolveLocalMetaData(dependencyType);
        return localMetaData != null
                ? localMetaData
                : getDependencyMap().findMetaData(dependencyType);
    }

    /**
     * Tests whether the shared dependency map contains a registration for a token.
     *
     * <p>This check reflects map registration only; access-local resolution hooks are not consulted.</p>
     *
     * @param dependencyType token to inspect
     * @return {@code true} when the non-null token is registered in the dependency map
     */
    default boolean isInstanceRegistered(Class<?> dependencyType) {
        return dependencyType != null && getDependencyMap().isInstanceRegistered(dependencyType);
    }

    /**
     * Finds an instance for a token without requiring that it exist.
     *
     * <p>A compatible access-local instance wins over the shared map.</p>
     *
     * @param dependencyType token to resolve
     * @param <T> dependency token type
     * @return matching instance, or {@code null} when the token is null or unavailable
     */
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
     * Resolves a required dependency from access-local state or the owning map.
     *
     * @param dependencyType token to resolve
     * @param <T> dependency token type
     * @return compatible local instance when available, otherwise the map-owned instance
     * @throws IllegalArgumentException when {@code dependencyType} is {@code null}
     * @throws IllegalStateException when no compatible mapped instance is registered
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
     * Resolves a required dependency.
     *
     * @param dependencyType token to resolve
     * @param <T> dependency token type
     * @return the resolved dependency instance
     * @deprecated Use {@link #getInstance(Class)}.
     */
    @Deprecated(forRemoval = false)
    default <T> T requireInstance(Class<T> dependencyType) {
        return getInstance(dependencyType);
    }

    /**
     * Replaces the map-owned instance for a dependency token.
     *
     * @param dependencyType token whose metadata should receive the replacement
     * @param supplier factory for the replacement instance
     * @param <T> dependency token type
     * @return replacement instance exposed by the dependency map
     */
    default <T> T replaceInstance(Class<T> dependencyType, Supplier<? extends T> supplier) {
        return getDependencyMap().replaceInstance(dependencyType, supplier);
    }

    /**
     * Registers an already-created instance in the shared dependency map.
     *
     * @param dependencyType token the instance must satisfy
     * @param dependencyInstance concrete instance to register
     * @param <T> dependency token type
     * @return the registered instance
     */
    default <T> T registerInstance(Class<T> dependencyType, T dependencyInstance) {
        return getDependencyMap().registerInstance(dependencyType, dependencyInstance);
    }

    /**
     * Creates and registers an instance in the shared dependency map.
     *
     * @param dependencyType token the supplied instance must satisfy
     * @param supplier factory used to create the dependency instance
     * @param <T> dependency token type
     * @return the supplied instance after registration
     */
    default <T> T registerInstance(Class<T> dependencyType, Supplier<? extends T> supplier) {
        return getDependencyMap().registerInstance(dependencyType, supplier);
    }

    /**
     * Hook for access implementations that expose a dependency without consulting the shared map.
     *
     * @param dependencyType token being resolved
     * @return locally owned dependency candidate, or {@code null} when local resolution does not
     *         apply
     */
    default Object resolveLocalInstance(Class<?> dependencyType) {
        return null;
    }

    /**
     * Hook for access implementations that expose dependency metadata without consulting the
     * shared map.
     *
     * @param dependencyType token being inspected
     * @return locally owned metadata, or {@code null} when local resolution does not apply
     */
    default IDependencyMetaData<?, ?> resolveLocalMetaData(Class<?> dependencyType) {
        return null;
    }
}
