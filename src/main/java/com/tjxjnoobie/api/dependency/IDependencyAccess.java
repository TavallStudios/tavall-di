/*
 * TJVD License (TJ Valentine’s Discretionary License) — Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package com.tjxjnoobie.api.dependency;

import com.tjxjnoobie.api.dependency.metadata.interfaces.IDependencyMetaData;

import java.util.function.Supplier;

/**
 * Shared loader-style DI lookup behavior that can be mixed into wrappers and metadata types.
 */
public interface IDependencyAccess {

    /**
     * Returns the global dependency loader used for token-based lookups.
     *
     * @return the shared dependency loader
     */
    default DependencyLoader getDependencyLoader() {
        return DependencyLoader.getDependencyLoader();
    }

    /**
     * Returns the dependency loader for the supplied logical scope.
     *
     * @param scopeName the DI scope to resolve
     * @return the scoped dependency loader
     */
    default DependencyLoader getDependencyLoader(String scopeName) {
        return DependencyLoader.getDependencyLoader(scopeName);
    }

    /**
     * Resolves metadata for the supplied dependency token.
     *
     * @param dependencyType the interface token used for lookup
     * @param <T> the dependency token type
     * @return metadata for the token, or {@code null} when nothing is registered
     */
    default <T> IDependencyMetaData<?, ?> findMetaData(Class<T> dependencyType) {
        if (dependencyType == null) {
            return null;
        }

        IDependencyMetaData<?, ?> localMetaData = resolveLocalMetaData(dependencyType);
        if (localMetaData != null) {
            return localMetaData;
        }

        return getDependencyLoader().findMetaData(dependencyType);
    }

    /**
     * Checks whether a dependency token is registered in the global DI runtime.
     *
     * @param dependencyType the interface token to inspect
     * @return {@code true} when the token is registered
     */
    default boolean isInstanceRegistered(Class<?> dependencyType) {
        return dependencyType != null && getDependencyLoader().isInstanceRegistered(dependencyType);
    }

    /**
     * Resolves a dependency from local state first and then from the global DI runtime.
     *
     * @param dependencyType the interface token to resolve
     * @param <T> the dependency token type
     * @return the resolved dependency instance, or {@code null} when unavailable
     */
    default <T> T findInstance(Class<T> dependencyType) {
        if (dependencyType == null) {
            return null;
        }

        Object localDependency = resolveLocalInstance(dependencyType);
        if (dependencyType.isInstance(localDependency)) {
            return dependencyType.cast(localDependency);
        }

        return getDependencyLoader().findInstance(dependencyType);
    }

    /**
     * Resolves a dependency and fails fast when the token is missing.
     *
     * @param dependencyType the interface token to resolve
     * @param <T> the dependency token type
     * @return the resolved dependency instance
     */
    default <T> T requireInstance(Class<T> dependencyType) {
        T dependency = findInstance(dependencyType);
        if (dependency != null) {
            return dependency;
        }

        throw new IllegalStateException("No dependency registered for " + dependencyType.getName());
    }

    /**
     * Replaces an already-registered dependency instance.
     *
     * @param dependencyType the interface token to replace
     * @param supplier the supplier that creates the replacement instance
     * @param <T> the dependency token type
     * @return the replacement instance
     */
    default <T> T replaceInstance(Class<T> dependencyType, Supplier<? extends T> supplier) {
        return getDependencyLoader().replaceInstance(dependencyType, supplier);
    }

    /**
     * Registers an already-constructed dependency instance in the default loader.
     *
     * @param dependencyType the interface token to bind
     * @param dependencyInstance the instance to register
     * @param <T> the dependency token type
     * @return the registered instance
     */
    default <T> T registerInstance(Class<T> dependencyType, T dependencyInstance) {
        return getDependencyLoader().registerInstance(dependencyType, dependencyInstance);
    }

    /**
     * Registers a dependency instance in the default loader using the supplied supplier.
     *
     * @param dependencyType the interface token to bind
     * @param supplier the factory that creates the instance
     * @param <T> the dependency token type
     * @return the registered instance
     */
    default <T> T registerInstance(Class<T> dependencyType, Supplier<? extends T> supplier) {
        return getDependencyLoader().registerInstance(dependencyType, supplier);
    }

    /**
     * Resolves a dependency from the current object before the DI runtime is queried.
     *
     * @param dependencyType the interface token being resolved
     * @return the locally available dependency, or {@code null} when this object cannot satisfy it
     */
    default Object resolveLocalInstance(Class<?> dependencyType) {
        return null;
    }

    /**
     * Resolves metadata from the current object before the DI map is queried.
     *
     * @param dependencyType the interface token being resolved
     * @return local metadata, or {@code null} when this object has none
     */
    default IDependencyMetaData<?, ?> resolveLocalMetaData(Class<?> dependencyType) {
        return null;
    }
}
