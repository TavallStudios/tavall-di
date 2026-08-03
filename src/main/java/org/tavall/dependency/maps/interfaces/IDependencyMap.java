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
 * Contract for the shared DI token-to-metadata map.
 */
public interface IDependencyMap {

    boolean isInstanceRegistered(Class<?> dependencyType);

    void registerDependency(
            Class<?> dependencyType,
            IDependencyMetaData<?, ?> dependencyMetaData);

    <T> T registerInstance(Class<T> dependencyType, T dependencyInstance);

    <T> T registerInstance(Class<T> dependencyType, Supplier<? extends T> supplier);

    <T> IDependencyMetaData<?, ?> findMetaData(Class<T> dependencyType);

    <T> T findInstance(Class<T> dependencyType);

    /**
     * Resolves a required dependency through its mapped metadata.
     *
     * @param dependencyType the dependency token to resolve
     * @param <T> the dependency token type
     * @return the metadata-owned instance
     * @throws IllegalStateException when no compatible instance is registered
     */
    <T> T getInstance(Class<T> dependencyType);

    <T> T replaceInstance(Class<T> dependencyType, Supplier<? extends T> supplier);

    void removeDependency(Class<?> dependencyType);

    int getDependencyMapSize();

    boolean isDependencyMapEmpty();
}
