/*
 * TJVD License (TJ Valentine’s Discretionary License) — Version 1.0 (2025)
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

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Authoritative dependency token-to-metadata map.
 */
public class DependencyMap
        extends ConcurrentHashMap<Class<?>, IDependencyMetaData<?, ?>>
        implements IDependencyMap {
    private static final DependencyMap DEPENDENCY_MAP = new DependencyMap();

    public static DependencyMap getDependencyMap() {
        return DEPENDENCY_MAP;
    }

    @Override
    public void registerDependency(
            Class<?> dependencyType,
            IDependencyMetaData<?, ?> dependencyMetaData) {
        if (dependencyType == null || dependencyMetaData == null) {
            throw new IllegalArgumentException("[DependencyMap] dependency key and metadata are required");
        }
        put(dependencyType, dependencyMetaData);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> T registerInstance(Class<T> dependencyType, T dependencyInstance) {
        validateRegistration(dependencyType, dependencyInstance);

        Class concreteType = dependencyInstance.getClass();
        DependencyMetaData metaData = new DependencyMetaData();
        metaData.bindDependencyInstance(
                dependencyType,
                concreteType,
                new DependencyInterface(dependencyType),
                new DependencyInstance(concreteType),
                dependencyInstance,
                () -> dependencyInstance);
        registerDependency(dependencyType, metaData);
        metaData.initializeDependencyInstance();
        return dependencyType.cast(dependencyInstance);
    }

    @Override
    public <T> T registerInstance(Class<T> dependencyType, Supplier<? extends T> supplier) {
        if (dependencyType == null) {
            throw new IllegalArgumentException("[DependencyMap] dependency key is required");
        }
        if (supplier == null) {
            throw new IllegalArgumentException("[DependencyMap] supplier is required");
        }

        T instance = supplier.get();
        validateRegistration(dependencyType, instance);
        return registerSuppliedInstance(dependencyType, instance, supplier);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> T registerSuppliedInstance(
            Class<T> dependencyType,
            T instance,
            Supplier<? extends T> supplier) {
        Class concreteType = instance.getClass();
        DependencyMetaData metaData = new DependencyMetaData();
        metaData.bindDependencyInstance(
                dependencyType,
                concreteType,
                new DependencyInterface(dependencyType),
                new DependencyInstance(concreteType),
                instance,
                (Supplier) supplier);
        registerDependency(dependencyType, metaData);
        metaData.initializeDependencyInstance();
        return dependencyType.cast(instance);
    }

    private <T> void validateRegistration(Class<T> dependencyType, T dependencyInstance) {
        if (dependencyType == null) {
            throw new IllegalArgumentException("[DependencyMap] dependency key is required");
        }
        if (dependencyInstance == null) {
            throw new IllegalArgumentException("[DependencyMap] dependency instance is required");
        }
        if (!dependencyType.isInstance(dependencyInstance)) {
            throw new IllegalArgumentException("[DependencyMap] dependency instance must satisfy "
                    + dependencyType.getName());
        }
    }

    @Override
    public <T> IDependencyMetaData<?, ?> findMetaData(Class<T> dependencyType) {
        return dependencyType == null ? null : get(dependencyType);
    }

    @Override
    public <T> T findInstance(Class<T> dependencyType) {
        if (dependencyType == null) {
            return null;
        }
        IDependencyMetaData<?, ?> metaData = findMetaData(dependencyType);
        return metaData == null ? null : metaData.findInstance(dependencyType);
    }

    @Override
    public <T> T getInstance(Class<T> dependencyType) {
        if (dependencyType == null) {
            throw new IllegalArgumentException("dependencyType is required");
        }

        IDependencyMetaData<?, ?> metaData = findMetaData(dependencyType);
        if (metaData == null) {
            throw new IllegalStateException("No dependency registered for " + dependencyType.getName());
        }
        return metaData.getInstance(dependencyType);
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public <T> T replaceInstance(Class<T> dependencyType, Supplier<? extends T> supplier) {
        if (dependencyType == null) {
            throw new IllegalArgumentException("[DependencyMap] dependency key is required");
        }
        if (supplier == null) {
            throw new IllegalArgumentException("[DependencyMap] replacement supplier is required");
        }

        IDependencyMetaData<?, ?> metaData = findMetaData(dependencyType);
        if (metaData == null) {
            throw new IllegalStateException("No dependency registered for " + dependencyType.getName());
        }

        T replacement = supplier.get();
        if (replacement == null) {
            throw new IllegalStateException("Replacement supplier returned null for " + dependencyType.getName());
        }

        List<Class<?>> mappedTypes = entrySet().stream()
                .filter(entry -> entry.getValue() == metaData)
                .map(Entry::getKey)
                .toList();
        for (Class<?> mappedType : mappedTypes) {
            if (!mappedType.isInstance(replacement)) {
                throw new IllegalArgumentException("Replacement " + replacement.getClass().getName()
                        + " cannot satisfy mapped dependency token " + mappedType.getName());
            }
        }

        ((IDependencyMetaData) metaData).replaceDependencyInstance(() -> replacement);
        return metaData.getInstance(dependencyType);
    }

    @Override
    public void removeDependency(Class<?> dependencyType) {
        if (dependencyType != null) {
            remove(dependencyType);
        }
    }

    @Override
    public boolean isInstanceRegistered(Class<?> dependencyType) {
        return dependencyType != null && containsKey(dependencyType);
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
