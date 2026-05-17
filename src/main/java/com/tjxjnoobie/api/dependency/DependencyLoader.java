/*
 * TJVD License (TJ Valentineâ€™s Discretionary License) â€” Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package com.tjxjnoobie.api.dependency;

import com.tjxjnoobie.api.dependency.maps.DependencyMap;
import com.tjxjnoobie.api.dependency.maps.interfaces.IDependencyMap;
import com.tjxjnoobie.api.dependency.metadata.interfaces.IDependencyMetaData;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Runtime-facing loader API for resolving and replacing DI-backed instances.
 */
public final class DependencyLoader {
    private static final DependencyLoader DEPENDENCY_LOADER =
            new DependencyLoader("default", DependencyMap.getDependencyMap());
    private static final ConcurrentHashMap<String, DependencyLoader> NAMED_LOADERS = new ConcurrentHashMap<>();

    private final String scopeName;
    private final IDependencyMap dependencyMap;

    private DependencyLoader(String scopeName, IDependencyMap dependencyMap) {
        this.scopeName = scopeName;
        this.dependencyMap = dependencyMap;
    }

    /**
     * Returns the shared dependency loader instance.
     *
     * @return the singleton loader
     */
    public static DependencyLoader getDependencyLoader() {
        return DEPENDENCY_LOADER;
    }

    /**
     * Returns the loader for the supplied scope, creating it on first access.
     *
     * @param scopeName the logical DI scope name
     * @return the scoped loader
     */
    public static DependencyLoader getDependencyLoader(String scopeName) {
        if (scopeName == null || scopeName.isBlank() || "default".equals(scopeName)) {
            return DEPENDENCY_LOADER;
        }
        return NAMED_LOADERS.computeIfAbsent(scopeName,
                ignored -> new DependencyLoader(scopeName, new DependencyMap()));
    }

    /**
     * Clears all non-default named loaders.
     */
    public static void clearNamedLoaders() {
        NAMED_LOADERS.values().forEach(DependencyLoader::clear);
        NAMED_LOADERS.clear();
    }

    /**
     * Returns the logical scope name backing this loader.
     *
     * @return the loader scope name
     */
    public String getScopeName() {
        return scopeName;
    }

    /**
     * Resolves an instance for the supplied interface token.
     *
     * @param dependencyType the interface token to resolve
     * @param <T> the dependency token type
     * @return the resolved instance, or {@code null} when unavailable
     */
    public <T> T findInstance(Class<T> dependencyType) {
        return dependencyMap.findInstance(dependencyType);
    }

    /**
     * Resolves metadata for the supplied interface token.
     *
     * @param dependencyType the interface token to inspect
     * @param <T> the dependency token type
     * @return the stored metadata, or {@code null} when missing
     */
    public <T> IDependencyMetaData<?, ?> findMetaData(Class<T> dependencyType) {
        return dependencyMap.findMetaData(dependencyType);
    }

    /**
     * Resolves an instance and fails fast when the token is missing.
     *
     * @param dependencyType the interface token to resolve
     * @param <T> the dependency token type
     * @return the resolved instance
     */
    public <T> T requireInstance(Class<T> dependencyType) {
        T instance = findInstance(dependencyType);
        if (instance != null) {
            return instance;
        }
        throw new IllegalStateException("No dependency registered for " + dependencyType.getName());
    }

    /**
     * Checks whether an interface token currently resolves to an instance.
     *
     * @param dependencyType the interface token to inspect
     * @return {@code true} when an instance is registered
     */
    public boolean isInstanceRegistered(Class<?> dependencyType) {
        return dependencyMap.isInstanceRegistered(dependencyType);
    }

    /**
     * Registers an already-constructed dependency instance.
     *
     * @param dependencyType the interface token to bind
     * @param dependencyInstance the concrete instance to register
     * @param <T> the dependency token type
     * @return the registered instance
     */
    public <T> T registerInstance(Class<T> dependencyType, T dependencyInstance) {
        return dependencyMap.registerInstance(dependencyType, dependencyInstance);
    }

    /**
     * Registers a dependency instance using the supplied factory.
     *
     * @param dependencyType the interface token to bind
     * @param supplier the supplier that creates the dependency instance
     * @param <T> the dependency token type
     * @return the registered instance
     */
    public <T> T registerInstance(Class<T> dependencyType, Supplier<? extends T> supplier) {
        return dependencyMap.registerInstance(dependencyType, supplier);
    }

    /**
     * Replaces an already-registered dependency instance.
     *
     * @param dependencyType the interface token to replace
     * @param supplier the supplier that builds the replacement instance
     * @param <T> the dependency token type
     * @return the replacement instance that was stored
     */
    public <T> T replaceInstance(Class<T> dependencyType, Supplier<? extends T> supplier) {
        return dependencyMap.replaceInstance(dependencyType, supplier);
    }

    /**
     * Clears every registration stored in this loader.
     */
    public void clear() {
        if (dependencyMap instanceof DependencyMap concreteMap) {
            concreteMap.clear();
            return;
        }
        throw new IllegalStateException("Unsupported dependency map implementation for scope " + scopeName);
    }

    @Override
    public String toString() {
        return "DependencyLoader[" + scopeName + "]";
    }
}
