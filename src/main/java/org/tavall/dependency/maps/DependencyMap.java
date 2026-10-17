/*
 * TJVD License (TJ Valentine’s Discretionary License) — Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package org.tavall.dependency.maps;

import org.tavall.dependency.IDependencyFactory;
import org.tavall.dependency.maps.interfaces.IDependencyMap;
import org.tavall.dependency.metadata.DependencyMetaData;
import org.tavall.dependency.metadata.interfaces.IDependencyMetaData;
import org.tavall.dependency.metadata.wrappers.DependencyInstance;
import org.tavall.dependency.metadata.wrappers.DependencyInterface;
import org.tavall.logging.Log;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    public synchronized <T> T registerInstance(Class<T> dependencyType, T dependencyInstance) {
        validateRegistration(dependencyType, dependencyInstance);
        if (dependencyInstance instanceof org.tavall.dependency.GenerationOwnedDependencyAccess owned) {
            return registerGenerationOwned(dependencyType, dependencyInstance, owned, () -> dependencyInstance);
        }

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
    public synchronized <T> T registerInstance(Class<T> dependencyType, Supplier<? extends T> supplier) {
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

    @Override
    public synchronized <T> T registerFactory(
            Class<T> dependencyType,
            IDependencyFactory<? extends T> dependencyFactory,
            Class<?>... aliases) {
        if (dependencyType == null) {
            throw new IllegalArgumentException("[DependencyMap] dependency key is required");
        }
        if (dependencyFactory == null) {
            throw new IllegalArgumentException("[DependencyMap] dependency factory is required");
        }

        boolean registeredFactory = bindFactoryAccess(dependencyFactory);
        T instance;
        try {
            instance = dependencyFactory.create();
            validateRegistration(dependencyType, instance);
        } catch (RuntimeException | Error failure) {
            if (registeredFactory) removeDependency(dependencyFactory.getClass());
            throw failure;
        }

        Set<Class<?>> dependencyTypes = new LinkedHashSet<>();
        dependencyTypes.add(dependencyType);
        if (aliases != null) {
            for (Class<?> alias : aliases) {
                if (alias == null || alias == Void.class) {
                    continue;
                }
                if (!alias.isInstance(instance)) {
                    if (registeredFactory) removeDependency(dependencyFactory.getClass());
                    throw new IllegalArgumentException("[DependencyMap] dependency instance must satisfy alias "
                            + alias.getName());
                }
                dependencyTypes.add(alias);
            }
        }

        return registerFactoryInstance(dependencyType, instance, dependencyFactory, dependencyTypes);
    }

    /** Binds scoped factory inputs through the same existing generation lifecycle as consumers. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean bindFactoryAccess(IDependencyFactory<?> factory) {
        if (factory instanceof org.tavall.dependency.IDependencyAccess access
                && !(factory instanceof org.tavall.dependency.GenerationOwnedDependencyAccess)
                && access.getDependencyMap() != this) {
            throw new IllegalArgumentException("Typed factory must use the owning map; scoped factories extend GenerationOwnedDependencyAccess");
        }
        IDependencyMetaData<?, ?> existing = findMetaData(factory.getClass());
        if (existing != null) {
            if (existing.getDependencyInstance() == factory) return false;
            throw new IllegalStateException("Factory class is already registered; unload it before replacement: " + factory.getClass().getName());
        }
        registerInstance((Class) factory.getClass(), factory);
        return true;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> T registerFactoryInstance(
            Class<T> dependencyType,
            T instance,
            IDependencyFactory<? extends T> dependencyFactory,
            Set<Class<?>> dependencyTypes) {
        Class concreteType = instance.getClass();
        DependencyMetaData metaData = new DependencyMetaData();
        metaData.bindDependencyInstance(
                dependencyType,
                concreteType,
                new DependencyInterface(dependencyType),
                new DependencyInstance(concreteType),
                instance,
                dependencyFactory::create);

        for (Class<?> exposedType : dependencyTypes) {
            registerDependency(exposedType, metaData);
        }
        metaData.initializeDependencyInstance();
        return dependencyType.cast(instance);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> T registerSuppliedInstance(
            Class<T> dependencyType,
            T instance,
            Supplier<? extends T> supplier) {
        if (instance instanceof org.tavall.dependency.GenerationOwnedDependencyAccess owned) {
            return registerGenerationOwned(dependencyType, instance, owned, supplier);
        }
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

    @SuppressWarnings({"rawtypes", "unchecked"})
    private <T> T registerGenerationOwned(Class<T> dependencyType, T instance,
            org.tavall.dependency.GenerationOwnedDependencyAccess owned, Supplier<? extends T> supplier) {
        java.util.Set<Class<?>> tokens = new java.util.LinkedHashSet<>();
        tokens.add(dependencyType);
        tokens.add(instance.getClass());
        org.tavall.dependency.annotations.DelegatesTo declaration =
                instance.getClass().getAnnotation(org.tavall.dependency.annotations.DelegatesTo.class);
        if (declaration != null) tokens.addAll(java.util.List.of(declaration.value()));
        for (Class<?> token : tokens) {
            if (!token.isInstance(instance)) throw new IllegalArgumentException("Invalid dependency alias " + token.getName());
        }
        IDependencyMetaData same = values().stream()
                .filter(metadata -> metadata.getDependencyInstance() == instance).findFirst().orElse(null);
        if (same != null) {
            for (Class<?> token : tokens) {
                IDependencyMetaData<?, ?> current = get(token);
                if (current != null && current != same) {
                    throw new IllegalStateException("Alias is owned by another dependency: " + token.getName());
                }
            }
            tokens.forEach(token -> put(token, same));
            return dependencyType.cast(instance);
        }
        java.util.Map<Class<?>, IDependencyMetaData<?, ?>> previous = new java.util.LinkedHashMap<>();
        java.util.Set<IDependencyMetaData<?, ?>> displaced = new java.util.HashSet<>();
        for (Class<?> token : tokens) {
            IDependencyMetaData<?, ?> metadata = get(token);
            if (metadata != null) displaced.add(metadata);
        }
        for (Map.Entry<Class<?>, IDependencyMetaData<?, ?>> entry : entrySet()) {
            if (displaced.contains(entry.getValue())) previous.put(entry.getKey(), entry.getValue());
        }
        owned.bindDependencyGeneration(this);
        DependencyMetaData metadata = new DependencyMetaData();
        metadata.bindDependencyInstance(dependencyType, instance.getClass(), new DependencyInterface(dependencyType),
                new DependencyInstance(instance.getClass()), instance, supplier);
        previous.keySet().forEach(super::remove);
        tokens.forEach(token -> put(token, metadata));
        try {
            metadata.initializeDependencyInstance();
        } catch (RuntimeException failure) {
            tokens.forEach(super::remove);
            super.putAll(previous);
            try { owned.unbindDependencyGeneration(this); }
            catch (RuntimeException cleanup) { failure.addSuppressed(cleanup); }
            throw failure;
        }
        for (IDependencyMetaData<?, ?> old : displaced) detach(old.getDependencyInstance());
        return dependencyType.cast(instance);
    }

    private void detach(Object instance) {
        if (instance instanceof org.tavall.dependency.GenerationOwnedDependencyAccess owned) {
            owned.unbindDependencyGeneration(this);
        }
    }

    @Override
    public synchronized void clear() {
        RuntimeException failure = null;
        for (IDependencyMetaData<?, ?> metadata : new java.util.HashSet<>(values())) {
            try { detach(metadata.getDependencyInstance()); }
            catch (RuntimeException exception) {
                if (failure == null) failure = exception; else failure.addSuppressed(exception);
            }
        }
        super.clear();
        Log.info("[DependencyMap] Cleared dependency registrations");
        if (failure != null) throw failure;
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
    public synchronized <T> IDependencyMetaData<?, ?> findMetaData(Class<T> dependencyType) {
        return dependencyType == null ? null : get(dependencyType);
    }

    @Override
    public synchronized <T> T findInstance(Class<T> dependencyType) {
        if (dependencyType == null) {
            return null;
        }
        IDependencyMetaData<?, ?> metaData = findMetaData(dependencyType);
        return metaData == null ? null : metaData.findInstance(dependencyType);
    }

    @Override
    public synchronized <T> T getInstance(Class<T> dependencyType) {
        if (dependencyType == null) {
            throw new IllegalArgumentException("dependencyType is required");
        }

        IDependencyMetaData<?, ?> metaData = findMetaData(dependencyType);
        if (metaData == null) {
            if (!dependencyType.isInterface() && !java.lang.reflect.Modifier.isAbstract(dependencyType.getModifiers())) {
                try {
                    java.lang.reflect.Constructor<T> constructor;
                    try {
                        constructor = dependencyType.getConstructor(IDependencyMap.class);
                        return registerInstance(dependencyType, constructor.newInstance(this));
                    } catch (NoSuchMethodException ignored) {
                        constructor = dependencyType.getConstructor();
                        return registerInstance(dependencyType, constructor.newInstance());
                    }
                } catch (ReflectiveOperationException ignored) {
                }
            }
            throw new IllegalStateException("No dependency registered for " + dependencyType.getName());
        }
        return metaData.getInstance(dependencyType);
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public synchronized <T> T replaceInstance(Class<T> dependencyType, Supplier<? extends T> supplier) {
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

        if (metaData.getDependencyInstance() instanceof org.tavall.dependency.GenerationOwnedDependencyAccess owned
                && !owned.isDependencyGenerationOwnedBy(this)) {
            throw new IllegalStateException("A child map cannot replace its parent's dependency generation");
        }
        T replacement = supplier.get();
        if (replacement == null) {
            throw new IllegalStateException("Replacement supplier returned null for " + dependencyType.getName());
        }

        List<Class<?>> mappedTypes = new ArrayList<>();
        for (Map.Entry<Class<?>, IDependencyMetaData<?, ?>> entry : entrySet()) {
            if (entry.getValue() == metaData) {
                mappedTypes.add(entry.getKey());
            }
        }
        for (Class<?> mappedType : mappedTypes) {
            if (!mappedType.isInstance(replacement)) {
                throw new IllegalArgumentException("Replacement " + replacement.getClass().getName()
                        + " cannot satisfy mapped dependency token " + mappedType.getName());
            }
        }

        Object original = metaData.getDependencyInstance();
        if (replacement instanceof org.tavall.dependency.GenerationOwnedDependencyAccess owned) {
            owned.bindDependencyGeneration(this);
        }
        ((IDependencyMetaData) metaData).replaceDependencyInstance(() -> replacement);
        if (original != replacement) detach(original);
        return metaData.getInstance(dependencyType);
    }

    @Override
    public synchronized void removeDependency(Class<?> dependencyType) {
        IDependencyMetaData<?, ?> metadata = dependencyType == null ? null : get(dependencyType);
        if (metadata == null) return;
        if (metadata.getDependencyInstance() instanceof org.tavall.dependency.GenerationOwnedDependencyAccess) {
            try { detach(metadata.getDependencyInstance()); }
            finally { entrySet().removeIf(entry -> entry.getValue() == metadata); }
        } else {
            remove(dependencyType);
        }
    }

    @Override
    public synchronized boolean isInstanceRegistered(Class<?> dependencyType) {
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

}
