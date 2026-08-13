/*
 * TJVD License (TJ Valentine’s Discretionary License) — Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package org.tavall.dependency.maps.interfaces;

import org.tavall.dependency.metadata.interfaces.IDependencyMetaData;

import java.util.function.Supplier;

/**
 * Contract for the authoritative mapping between dependency tokens and their runtime metadata.
 *
 * <p>A dependency token is the class callers use for lookup, typically an interface or other API
 * type. Metadata owns the concrete instance behind that token. Registration methods return the
 * instance that becomes visible through subsequent lookups.</p>
 */
public interface IDependencyMap {

    /**
     * Tests whether a non-null dependency token is currently mapped.
     *
     * @param dependencyType dependency token to inspect
     * @return {@code true} when the token has a metadata registration
     */
    boolean isInstanceRegistered(Class<?> dependencyType);

    /**
     * Associates dependency metadata with a token.
     *
     * <p>An existing mapping for the same token may be replaced by the new metadata.</p>
     *
     * @param dependencyType token used for future lookup
     * @param dependencyMetaData metadata that owns the dependency instance
     * @throws IllegalArgumentException if the token or metadata is {@code null}
     */
    void registerDependency(
            Class<?> dependencyType,
            IDependencyMetaData<?, ?> dependencyMetaData);

    /**
     * Registers an already-created instance under a dependency token.
     *
     * @param dependencyType token the instance must satisfy
     * @param dependencyInstance concrete instance to register
     * @param <T> dependency token type
     * @return the registered instance cast to the token type
     * @throws IllegalArgumentException if the token or instance is null, or the instance does not
     *                                  satisfy the token
     */
    <T> T registerInstance(Class<T> dependencyType, T dependencyInstance);

    /**
     * Creates and registers an instance using a supplier.
     *
     * <p>The supplier is evaluated during registration rather than deferred until first lookup.</p>
     *
     * @param dependencyType token the supplied instance must satisfy
     * @param supplier factory used to create the dependency instance
     * @param <T> dependency token type
     * @return the supplied instance after registration
     * @throws IllegalArgumentException if the token or supplier is null, or the supplied instance
     *                                  is null or incompatible with the token
     */
    <T> T registerInstance(Class<T> dependencyType, Supplier<? extends T> supplier);

    /**
     * Finds metadata currently associated with a token without failing when it is absent.
     *
     * @param dependencyType token to inspect
     * @param <T> dependency token type
     * @return mapped metadata, or {@code null} when the token is null or unregistered
     */
    <T> IDependencyMetaData<?, ?> findMetaData(Class<T> dependencyType);

    /**
     * Finds the instance currently exposed through a token without requiring registration.
     *
     * @param dependencyType token to resolve
     * @param <T> dependency token type
     * @return resolved instance, or {@code null} when the token is null or unregistered
     */
    <T> T findInstance(Class<T> dependencyType);

    /**
     * Resolves a required dependency through its mapped metadata.
     *
     * @param dependencyType dependency token to resolve
     * @param <T> dependency token type
     * @return the metadata-owned instance
     * @throws IllegalArgumentException when {@code dependencyType} is {@code null}
     * @throws IllegalStateException when no compatible instance is registered
     */
    <T> T getInstance(Class<T> dependencyType);

    /**
     * Replaces the instance owned by the metadata mapped to a dependency token.
     *
     * <p>The replacement must remain compatible with every token sharing that metadata so aliases
     * cannot be left pointing at an invalid instance.</p>
     *
     * @param dependencyType token whose metadata should receive the replacement
     * @param supplier factory that creates the replacement instance
     * @param <T> dependency token type
     * @return the replacement instance exposed through {@code dependencyType}
     * @throws IllegalArgumentException if the token or supplier is invalid, or the replacement
     *                                  cannot satisfy all mapped tokens
     * @throws IllegalStateException if the token is not registered or the supplier returns null
     */
    <T> T replaceInstance(Class<T> dependencyType, Supplier<? extends T> supplier);

    /**
     * Removes the mapping for a dependency token when present.
     *
     * @param dependencyType token to remove; a null token is treated as a no-op
     */
    void removeDependency(Class<?> dependencyType);

    /**
     * Returns the number of dependency-token mappings currently stored.
     *
     * @return current mapping count
     */
    int getDependencyMapSize();

    /**
     * Tests whether the map currently contains no dependency-token mappings.
     *
     * @return {@code true} when the map is empty
     */
    boolean isDependencyMapEmpty();
}
