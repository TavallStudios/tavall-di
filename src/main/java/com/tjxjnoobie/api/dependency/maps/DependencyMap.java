/*
 * TJVD License (TJ Valentine’s Discretionary License) — Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package com.tjxjnoobie.api.dependency.maps;

import com.tjxjnoobie.api.dependency.IDependencyInjectableConcrete;
import com.tjxjnoobie.api.dependency.IDependencyInjectableInterface;
import com.tjxjnoobie.api.dependency.maps.interfaces.IDependencyMap;
import com.tjxjnoobie.api.dependency.metadata.DependencyMetaData;
import com.tjxjnoobie.api.dependency.metadata.interfaces.IDependencyMetaData;
import com.tjxjnoobie.api.dependency.metadata.wrappers.DependencyInstance;
import com.tjxjnoobie.api.dependency.metadata.wrappers.DependencyInterface;
import com.tjxjnoobie.api.platform.global.console.Log;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Global registry that maps interface tokens to dependency metadata.
 */
public class DependencyMap extends ConcurrentHashMap<Class<?>, IDependencyMetaData<?, ?>> implements IDependencyMap {
    private static final DependencyMap DEPENDENCY_MAP = new DependencyMap();

    /**
     * Returns the singleton dependency map used by the API module.
     *
     * @return the shared dependency map
     */
    public static DependencyMap getDependencyMap() {
        return DEPENDENCY_MAP;
    }

    @Override
    public void registerDependency(
            Class<? extends IDependencyInjectableInterface> rawDependencyInterface,
            IDependencyMetaData<?, ?> dependencyMetaData) {
        if (rawDependencyInterface == null || dependencyMetaData == null) {
            throw new IllegalArgumentException("[DependencyMap] interface key and metadata are required");
        }
        if (!rawDependencyInterface.isInterface()) {
            throw new IllegalArgumentException("[DependencyMap] dependency key must be an interface: "
                    + rawDependencyInterface.getName());
        }

        IDependencyMetaData<?, ?> existing = get(rawDependencyInterface);
        if (existing != null && existing != dependencyMetaData) {
            throw new IllegalStateException("[DependencyMap] duplicate registration for "
                    + rawDependencyInterface.getName()
                    + "; use replaceInstance(...) to swap an existing binding");
        }

        put(rawDependencyInterface, dependencyMetaData);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> T registerInstance(Class<T> dependencyInterface, T dependencyInstance) {
        if (dependencyInterface == null) {
            throw new IllegalArgumentException("[DependencyMap] dependency key is required");
        }
        if (dependencyInstance == null) {
            throw new IllegalArgumentException("[DependencyMap] dependency instance is required");
        }
        if (!dependencyInterface.isInterface()) {
            throw new IllegalArgumentException("[DependencyMap] dependency key must be an interface: "
                    + dependencyInterface.getName());
        }
        if (!(dependencyInstance instanceof IDependencyInjectableConcrete concreteInstance)) {
            throw new IllegalArgumentException("[DependencyMap] dependency instance must implement IDependencyInjectableConcrete: "
                    + dependencyInstance.getClass().getName());
        }
        if (!dependencyInterface.isInstance(dependencyInstance)) {
            throw new IllegalArgumentException("[DependencyMap] dependency instance must implement "
                    + dependencyInterface.getName());
        }

        Class concreteType = dependencyInstance.getClass();
        DependencyMetaData metaData = new DependencyMetaData();
        metaData.bindDependencyInstance(
                dependencyInterface.asSubclass(IDependencyInjectableInterface.class),
                concreteType.asSubclass(IDependencyInjectableConcrete.class),
                new DependencyInterface(dependencyInterface.asSubclass(IDependencyInjectableInterface.class)),
                new DependencyInstance(concreteType.asSubclass(IDependencyInjectableConcrete.class)),
                concreteInstance,
                () -> concreteInstance);
        registerDependency(dependencyInterface.asSubclass(IDependencyInjectableInterface.class), metaData);
        metaData.initializeDependencyInstance();
        return dependencyInterface.cast(dependencyInstance);
    }

    @Override
    public <T> T registerInstance(Class<T> dependencyInterface, Supplier<? extends T> supplier) {
        if (supplier == null) {
            throw new IllegalArgumentException("[DependencyMap] supplier is required");
        }
        T instance = supplier.get();
        if (instance == null) {
            throw new IllegalStateException("[DependencyMap] supplier returned null for " + dependencyInterface.getName());
        }
        if (!(instance instanceof IDependencyInjectableConcrete concreteInstance)) {
            throw new IllegalArgumentException("[DependencyMap] dependency instance must implement IDependencyInjectableConcrete: "
                    + instance.getClass().getName());
        }
        if (!dependencyInterface.isInterface()) {
            throw new IllegalArgumentException("[DependencyMap] dependency key must be an interface: "
                    + dependencyInterface.getName());
        }
        if (!dependencyInterface.isInstance(instance)) {
            throw new IllegalArgumentException("[DependencyMap] dependency instance must implement "
                    + dependencyInterface.getName());
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        Class concreteType = instance.getClass();
        @SuppressWarnings({"unchecked", "rawtypes"})
        DependencyMetaData metaData = new DependencyMetaData();
        metaData.bindDependencyInstance(
                dependencyInterface.asSubclass(IDependencyInjectableInterface.class),
                concreteType.asSubclass(IDependencyInjectableConcrete.class),
                new DependencyInterface(dependencyInterface.asSubclass(IDependencyInjectableInterface.class)),
                new DependencyInstance(concreteType.asSubclass(IDependencyInjectableConcrete.class)),
                concreteInstance,
                (Supplier) supplier);
        registerDependency(dependencyInterface.asSubclass(IDependencyInjectableInterface.class), metaData);
        metaData.initializeDependencyInstance();
        return dependencyInterface.cast(instance);
    }

    @Override
    public <T> IDependencyMetaData<?, ?> findMetaData(Class<T> dependencyInterface) {
        if (dependencyInterface == null) {
            return null;
        }
        return get(dependencyInterface);
    }

    @Override
    public <T> T findInstance(Class<T> dependencyInterface) {
        if (dependencyInterface == null) {
            return null;
        }

        IDependencyMetaData<?, ?> metaData = findMetaData(dependencyInterface);
        if (metaData == null) {
            return null;
        }

        Object resolvedDependency = metaData.resolveLocalInstance(dependencyInterface);
        if (dependencyInterface.isInstance(resolvedDependency)) {
            return dependencyInterface.cast(resolvedDependency);
        }

        return null;
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public <T> T replaceInstance(Class<T> dependencyInterface, Supplier<? extends T> supplier) {
        if (dependencyInterface == null) {
            throw new IllegalArgumentException("[DependencyMap] dependency key is required");
        }
        if (supplier == null) {
            throw new IllegalArgumentException("[DependencyMap] replacement supplier is required");
        }

        IDependencyMetaData<?, ?> metaData = findMetaData(dependencyInterface);
        if (metaData == null) {
            throw new IllegalStateException("replaceInstance: instance not registered: " + dependencyInterface.getName());
        }

        Object existing = metaData.resolveLocalInstance(dependencyInterface);
        if (!dependencyInterface.isInstance(existing)) {
            throw new IllegalStateException("replaceInstance: instance not registered: " + dependencyInterface.getName());
        }

        ((IDependencyMetaData) metaData).replaceDependencyInstance((Supplier) supplier);
        Object replacement = metaData.resolveLocalInstance(dependencyInterface);
        if (!dependencyInterface.isInstance(replacement)) {
            throw new IllegalStateException("replaceInstance: replacement type mismatch for " + dependencyInterface.getName());
        }

        return dependencyInterface.cast(replacement);
    }

    @Override
    public void removeDependency(Class<?> dependencyInterface) {
        if (dependencyInterface == null) {
            return;
        }
        remove(dependencyInterface);
    }

    @Override
    public boolean isInstanceRegistered(Class<?> dependencyInterface) {
        return dependencyInterface != null && containsKey(dependencyInterface);
    }

    @Override
    public int getDependencyMapSize() {
        return size();
    }

    @Override
    public boolean isDependencyMapEmpty() {
        return isEmpty();
    }

    @Override
    public void clear() {
        super.clear();
        Log.info("[DependencyMap] Cleared dependency registrations");
    }
}
