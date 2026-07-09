/*
 * TJVD License (TJ ValentineÃ¢â‚¬â„¢s Discretionary License) Ã¢â‚¬â€ Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package org.tavall.dependency.maps;

import org.tavall.dependency.maps.interfaces.IDependencyMap;
import org.tavall.dependency.metadata.DependencyMetaData;
import org.tavall.dependency.metadata.interfaces.IDependencyMetaData;
import org.tavall.dependency.metadata.wrappers.DependencyInstance;
import org.tavall.dependency.metadata.wrappers.DependencyInterface;
import org.tavall.logging.Log;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Global registry that maps interface tokens to dependency metadata.
 */
public class DependencyMap extends ConcurrentHashMap<Class<?>, IDependencyMetaData<?, ?>> implements IDependencyMap {
    private static final DependencyMap DEPENDENCY_MAP = new DependencyMap();

    /**
     * Returns the singleton dependency map used by the dependency module.
     *
     * @return the shared dependency map
     */
    public static DependencyMap getDependencyMap() {
        return DEPENDENCY_MAP;
    }

    @Override
    public void registerDependency(
            Class<?> rawDependencyInterface,
            IDependencyMetaData<?, ?> dependencyMetaData) {
        if (rawDependencyInterface == null || dependencyMetaData == null) {
            throw new IllegalArgumentException("[DependencyMap] dependency key and metadata are required");
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
        if (!dependencyInterface.isInstance(dependencyInstance)) {
            throw new IllegalArgumentException("[DependencyMap] dependency instance must implement "
                    + dependencyInterface.getName());
        }

        Class concreteType = dependencyInstance.getClass();
        DependencyMetaData metaData = new DependencyMetaData();
        Supplier directSupplier = () -> dependencyInstance;
        ((DependencyMetaData) metaData).bindDependencyInstance(
                (Class) dependencyInterface,
                concreteType,
                new DependencyInterface((Class) dependencyInterface),
                new DependencyInstance(concreteType),
                dependencyInstance,
                directSupplier);
        registerDependency(dependencyInterface, metaData);
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
        if (!dependencyInterface.isInstance(instance)) {
            throw new IllegalArgumentException("[DependencyMap] dependency instance must implement "
                    + dependencyInterface.getName());
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        Class concreteType = instance.getClass();
        @SuppressWarnings({"unchecked", "rawtypes"})
        DependencyMetaData metaData = new DependencyMetaData();
        @SuppressWarnings("rawtypes")
        Supplier rawSupplier = (Supplier) supplier;
        ((DependencyMetaData) metaData).bindDependencyInstance(
                (Class) dependencyInterface,
                concreteType,
                new DependencyInterface((Class) dependencyInterface),
                new DependencyInstance(concreteType),
                instance,
                rawSupplier);
        registerDependency(dependencyInterface, metaData);
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
