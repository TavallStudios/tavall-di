/*
 * TJVD License (TJ Valentine’s Discretionary License) — Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package com.tjxjnoobie.api.dependency.maps.interfaces;

import com.tjxjnoobie.api.dependency.IDependencyInjectableInterface;
import com.tjxjnoobie.api.dependency.metadata.interfaces.IDependencyMetaData;

import java.util.function.Supplier;

/**
 * Contract for the shared DI registration map.
 */
public interface IDependencyMap {

    /**
     * Checks whether a dependency token is already registered.
     *
     * @param dependencyInterface the interface token to inspect
     * @return {@code true} when the token has metadata in the map
     */
    boolean isInstanceRegistered(Class<?> dependencyInterface);

    /**
     * Stores metadata under the supplied interface token.
     *
     * @param rawDependencyInterface the interface token to register
     * @param dependencyMetaData the metadata that owns the token binding
     */
    void registerDependency(
            Class<? extends IDependencyInjectableInterface> rawDependencyInterface,
            IDependencyMetaData<?, ?> dependencyMetaData);

    /**
     * Registers an already-constructed dependency instance without reflective instantiation.
     *
     * @param dependencyInterface the interface token to register
     * @param dependencyInstance the concrete instance to bind
     * @param <T> the token type
     * @return the registered instance
     */
    <T> T registerInstance(Class<T> dependencyInterface, T dependencyInstance);

    /**
     * Registers a dependency instance using the supplied factory without reflective instantiation.
     *
     * @param dependencyInterface the interface token to register
     * @param supplier the supplier that creates the instance to bind
     * @param <T> the token type
     * @return the registered instance
     */
    <T> T registerInstance(Class<T> dependencyInterface, Supplier<? extends T> supplier);

    /**
     * Returns metadata for the supplied interface token.
     *
     * @param dependencyInterface the token to inspect
     * @param <T> the token type
     * @return the stored metadata, or {@code null} when missing
     */
    <T> IDependencyMetaData<?, ?> findMetaData(Class<T> dependencyInterface);

    /**
     * Resolves an instance for the supplied interface token.
     *
     * @param dependencyInterface the token to resolve
     * @param <T> the token type
     * @return the resolved dependency, or {@code null} when unavailable
     */
    <T> T findInstance(Class<T> dependencyInterface);

    /**
     * Replaces an already-registered instance for the supplied interface token.
     *
     * @param dependencyInterface the token to replace
     * @param supplier the supplier that builds the replacement instance
     * @param <T> the token type
     * @return the replacement instance that was stored
     */
    <T> T replaceInstance(Class<T> dependencyInterface, Supplier<? extends T> supplier);

    /**
     * Removes metadata registered for the supplied interface token.
     *
     * @param dependencyInterface the token to remove
     */
    void removeDependency(Class<?> dependencyInterface);

    /**
     * Returns the number of registered interface tokens.
     *
     * @return the registration count
     */
    int getDependencyMapSize();

    /**
     * Checks whether the DI map is empty.
     *
     * @return {@code true} when no tokens are registered
     */
    boolean isDependencyMapEmpty();
}
