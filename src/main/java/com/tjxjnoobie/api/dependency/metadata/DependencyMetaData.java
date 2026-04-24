/*
 * TJVD License (TJ Valentine’s Discretionary License) — Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package com.tjxjnoobie.api.dependency.metadata;

import com.tjxjnoobie.api.dependency.IDependencyInjectableConcrete;
import com.tjxjnoobie.api.dependency.IDependencyInjectableInterface;
import com.tjxjnoobie.api.dependency.injection.enums.LifecycleType;
import com.tjxjnoobie.api.dependency.metadata.interfaces.IDependencyMetaData;
import com.tjxjnoobie.api.dependency.metadata.wrappers.interfaces.IDependencyInstance;
import com.tjxjnoobie.api.dependency.metadata.wrappers.interfaces.IDependencyInterface;
import com.tjxjnoobie.api.interfaces.IContext;
import com.tjxjnoobie.api.platform.global.annotations.Inject;
import com.tjxjnoobie.api.platform.global.console.Log;
import com.tjxjnoobie.api.platform.global.enums.DependencyRole;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Default metadata implementation for DI bindings registered in the API module.
 *
 * @param <INTERFACE> the injectable interface token type
 * @param <INSTANCE> the injectable concrete instance type
 */
public class DependencyMetaData<
        INTERFACE extends IDependencyInjectableInterface,
        INSTANCE extends IDependencyInjectableConcrete> implements IDependencyMetaData<INTERFACE, INSTANCE> {
    private Set<INTERFACE> subDependencies = new HashSet<>();
    private int priority;
    private int depth;
    private DependencyRole dependencyRole = DependencyRole.ISOLATED;
    private final Map<LifecycleType, Method> lifecycleMethods = new EnumMap<>(LifecycleType.class);
    private final Map<LifecycleType, Boolean> lifecycleSuccess = new EnumMap<>(LifecycleType.class);
    private int retryCount;
    private IContext<INTERFACE> sourceContext;
    private Supplier<INSTANCE> dependencySupplier;
    private IDependencyInterface<INTERFACE> wrappedInterface;
    private IDependencyInstance<INSTANCE> wrappedInstance;
    private Class<? extends INTERFACE> primaryInterfaceType;
    private Class<? extends INSTANCE> concreteType;
    private INSTANCE dependencyInstance;

    public DependencyMetaData() {
        resolveTypesFromSubclass();
    }

    private void resolveTypesFromSubclass() {
        Type superType = getClass().getGenericSuperclass();
        if (!(superType instanceof ParameterizedType parameterizedType)) {
            return;
        }

        Class<? extends INTERFACE> resolvedInterface = resolveClass(parameterizedType.getActualTypeArguments()[0]);
        Class<? extends INSTANCE> resolvedConcrete = resolveClass(parameterizedType.getActualTypeArguments()[1]);

        if (resolvedInterface != null) {
            this.primaryInterfaceType = resolvedInterface;
        }
        if (resolvedConcrete != null) {
            this.concreteType = resolvedConcrete;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> Class<? extends T> resolveClass(Type type) {
        if (type instanceof Class<?> rawClass) {
            return (Class<? extends T>) rawClass;
        }
        if (type instanceof ParameterizedType parameterizedType
                && parameterizedType.getRawType() instanceof Class<?> rawType) {
            return (Class<? extends T>) rawType;
        }
        return null;
    }

    @Override
    public void populateMetaData(
            Class<? extends INTERFACE> rawDependencyInterface,
            Class<? extends INSTANCE> rawDependencyConcrete,
            IDependencyInterface<INTERFACE> wrappedInterface,
            IDependencyInstance<INSTANCE> wrappedInstance) {
        assignBinding(rawDependencyInterface, rawDependencyConcrete, wrappedInterface, wrappedInstance);
        createDependencyInstance(rawDependencyConcrete);
    }

    @Override
    public void bindDependencyInstance(
            Class<? extends INTERFACE> rawDependencyInterface,
            Class<? extends INSTANCE> rawDependencyConcrete,
            IDependencyInterface<INTERFACE> wrappedInterface,
            IDependencyInstance<INSTANCE> wrappedInstance,
            INSTANCE dependencyInstance,
            Supplier<? extends INSTANCE> dependencySupplier) {
        if (dependencyInstance == null) {
            throw new IllegalArgumentException("[DependencyMetaData] dependency instance is required");
        }

        assignBinding(rawDependencyInterface, rawDependencyConcrete, wrappedInterface, wrappedInstance);
        if (!rawDependencyConcrete.isInstance(dependencyInstance)) {
            throw new IllegalArgumentException("[DependencyMetaData] dependency instance must match concrete token: "
                    + rawDependencyConcrete.getName());
        }

        @SuppressWarnings("unchecked")
        Supplier<INSTANCE> castSupplier = dependencySupplier == null
                ? () -> dependencyInstance
                : () -> (INSTANCE) dependencySupplier.get();
        setDependencySupplier(castSupplier);
        this.dependencyInstance = dependencyInstance;

        if (this.wrappedInstance != null) {
            this.wrappedInstance.setWrappedDependencyInstance(dependencyInstance);
        }

        if (this.wrappedInterface != null) {
            this.wrappedInterface.setDependencyInterface(getDependencyInterface());
        }
    }

    private void assignBinding(
            Class<? extends INTERFACE> rawDependencyInterface,
            Class<? extends INSTANCE> rawDependencyConcrete,
            IDependencyInterface<INTERFACE> wrappedInterface,
            IDependencyInstance<INSTANCE> wrappedInstance) {
        validateInterfaceType(rawDependencyInterface);
        validateConcreteType(rawDependencyConcrete, true);

        this.primaryInterfaceType = rawDependencyInterface;
        this.concreteType = rawDependencyConcrete;
        setWrappedInterface(wrappedInterface);
        setWrappedInstance(wrappedInstance);

        if (this.wrappedInterface != null) {
            this.wrappedInterface.setDependencyInterfaceWrapperRawClass(rawDependencyInterface);
        }
        if (this.wrappedInstance != null) {
            this.wrappedInstance.setWrappedRawInstanceClass(rawDependencyConcrete);
        }
    }

    @Override
    public void createDependencyInstance(Class<? extends INSTANCE> dependencyInstanceClass) {
        validateConcreteType(dependencyInstanceClass, true);

        this.concreteType = dependencyInstanceClass;
        setDependencySupplier(() -> instantiate(dependencyInstanceClass));
        this.dependencyInstance = dependencySupplier.get();

        if (wrappedInstance != null) {
            wrappedInstance.setWrappedRawInstanceClass(dependencyInstanceClass);
            wrappedInstance.setWrappedDependencyInstance(this.dependencyInstance);
        }

        if (wrappedInterface != null) {
            wrappedInterface.setDependencyInterface(getDependencyInterface());
        }
    }

    private INSTANCE instantiate(Class<? extends INSTANCE> dependencyInstanceClass) {
        try {
            var constructor = dependencyInstanceClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to instantiate dependency " + dependencyInstanceClass.getName(), e);
        }
    }

    /**
     * Stores the supplier used to build dependency instances for this metadata.
     *
     * @param supplier the supplier used to create concrete instances
     */
    public void setDependencySupplier(Supplier<INSTANCE> supplier) {
        this.dependencySupplier = supplier;
    }

    /**
     * Replaces the concrete instance using the supplied factory.
     *
     * @param dependencySupplier the factory used to refresh the concrete instance
     */
    @Override
    public void replaceDependencyInstance(Supplier<? extends INSTANCE> dependencySupplier) {
        if (dependencySupplier == null) {
            throw new IllegalArgumentException("[DependencyMetaData] replacement supplier is required");
        }

        INSTANCE replacement = dependencySupplier.get();
        if (replacement == null) {
            throw new IllegalStateException("Replacement supplier returned null for " + concreteType.getName());
        }
        if (concreteType != null && !concreteType.isInstance(replacement)) {
            throw new IllegalArgumentException("[DependencyMetaData] replacement must match concrete token: "
                    + concreteType.getName());
        }

        @SuppressWarnings("unchecked")
        Supplier<INSTANCE> castSupplier = () -> (INSTANCE) replacement;
        setDependencySupplier(castSupplier);
        this.dependencyInstance = replacement;

        if (wrappedInstance != null) {
            wrappedInstance.setWrappedDependencyInstance(replacement);
        }
        if (wrappedInterface != null) {
            wrappedInterface.setDependencyInterface(getDependencyInterface());
        }

        initializeDependencyInstance();
    }

    @Override
    public void setWrappedInterface(IDependencyInterface<INTERFACE> wrappedInterface) {
        this.wrappedInterface = wrappedInterface;
    }

    @Override
    public void setWrappedInstance(IDependencyInstance<INSTANCE> wrappedInstance) {
        this.wrappedInstance = wrappedInstance;
    }

    @Override
    public IDependencyInterface<INTERFACE> getWrappedInterface() {
        return wrappedInterface;
    }

    @Override
    public IDependencyInstance<INSTANCE> getWrappedInstance() {
        return wrappedInstance;
    }

    @Override
    public Class<? extends INTERFACE> getPrimaryInterfaceType() {
        return primaryInterfaceType;
    }

    @Override
    public Class<? extends INSTANCE> getConcreteType() {
        return concreteType;
    }

    @Override
    public INTERFACE getDependencyInterface() {
        if (primaryInterfaceType != null && primaryInterfaceType.isInstance(dependencyInstance)) {
            return primaryInterfaceType.cast(dependencyInstance);
        }

        if (wrappedInterface != null && wrappedInterface.getInterface() != null) {
            return wrappedInterface.getInterface();
        }

        return null;
    }

    @Override
    public INSTANCE getDependencyInstance() {
        return dependencyInstance;
    }

    @Override
    public void initializeDependencyInstance() {
        if (dependencyInstance == null) {
            throw new IllegalStateException("[DependencyMetaData] dependency instance has not been created");
        }

        detectLifecycleMethods(dependencyInstance);
        invokeLifecycleMethods(dependencyInstance, LifecycleType.PRE_CONSTRUCT);
        injectAnnotatedFields(dependencyInstance);
        invokeLifecycleMethods(dependencyInstance, LifecycleType.POST_CONSTRUCT);
    }

    @Override
    public <T> T findInstance(Class<T> dependencyType) {
        return IDependencyMetaData.super.findInstance(dependencyType);
    }

    @Override
    public <T> T requireInstance(Class<T> dependencyType) {
        return IDependencyMetaData.super.requireInstance(dependencyType);
    }

    @Override
    public EnumMap<LifecycleType, Method> detectLifecycleForClass(INTERFACE dependencyClass) {
        EnumMap<LifecycleType, Method> map = new EnumMap<>(LifecycleType.class);
        if (dependencyClass == null) {
            return map;
        }

        for (LifecycleType lifecycle : LifecycleType.values()) {
            lifecycle.findIn(dependencyClass.getClass()).ifPresent(method -> map.put(lifecycle, method));
        }

        return map;
    }

    @Override
    public Set<INTERFACE> getSubDependencies() {
        return subDependencies;
    }

    @Override
    public void setSubDependencies(Set<INTERFACE> dependencyClassSet) {
        this.subDependencies = dependencyClassSet == null ? new HashSet<>() : dependencyClassSet;
    }

    @Override
    public int getDepth() {
        return depth;
    }

    @Override
    public void setDepth(int depth) {
        this.depth = depth;
    }

    @Override
    public DependencyRole getDependencyRole() {
        return dependencyRole;
    }

    @Override
    public void setDependencyRole(DependencyRole dependencyRole) {
        this.dependencyRole = dependencyRole;
    }

    @Override
    public Method getPreConstruct() {
        return lifecycleMethods.get(LifecycleType.PRE_CONSTRUCT);
    }

    @Override
    public Method getPostConstruct() {
        return lifecycleMethods.get(LifecycleType.POST_CONSTRUCT);
    }

    @Override
    public void setPreConstruct(Method preConstruct) {
        lifecycleMethods.put(LifecycleType.PRE_CONSTRUCT, preConstruct);
    }

    @Override
    public void setPostConstruct(Method postConstruct) {
        lifecycleMethods.put(LifecycleType.POST_CONSTRUCT, postConstruct);
    }

    @Override
    public boolean isPreConstructSuccess() {
        return lifecycleSuccess.getOrDefault(LifecycleType.PRE_CONSTRUCT, false);
    }

    @Override
    public void setPreConstructSuccess(boolean success) {
        lifecycleSuccess.put(LifecycleType.PRE_CONSTRUCT, success);
    }

    @Override
    public int getRetryCount() {
        return retryCount;
    }

    @Override
    public void incrementRetryCount() {
        retryCount++;
    }

    @Override
    public IContext<INTERFACE> getSourceContext() {
        return sourceContext;
    }

    @Override
    public void setSourceContext(IContext<INTERFACE> ctx) {
        this.sourceContext = ctx;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public Object resolveLocalInstance(Class<?> dependencyType) {
        if (dependencyType == null) {
            return null;
        }

        if (dependencyType.isInstance(dependencyInstance)) {
            return dependencyInstance;
        }

        INTERFACE dependencyInterface = getDependencyInterface();
        if (dependencyType.isInstance(dependencyInterface)) {
            return dependencyInterface;
        }

        return null;
    }

    @Override
    public IDependencyMetaData<?, ?> resolveLocalMetaData(Class<?> dependencyType) {
        return resolveLocalInstance(dependencyType) == null ? null : this;
    }

    private void validateInterfaceType(Class<?> interfaceType) {
        if (interfaceType == null) {
            throw new IllegalArgumentException("[DependencyMetaData] interface token is required");
        }
        if (!interfaceType.isInterface()) {
            throw new IllegalArgumentException("[DependencyMetaData] interface token must be an interface: "
                    + interfaceType.getName());
        }
    }

    private void validateConcreteType(Class<?> concreteType, boolean requireInstantiable) {
        if (concreteType == null) {
            throw new IllegalArgumentException("[DependencyMetaData] concrete token is required");
        }
        if (concreteType.isInterface()) {
            throw new IllegalArgumentException("[DependencyMetaData] concrete token cannot be an interface: "
                    + concreteType.getName());
        }
        if (requireInstantiable && java.lang.reflect.Modifier.isAbstract(concreteType.getModifiers())) {
            throw new IllegalArgumentException("[DependencyMetaData] concrete token cannot be abstract: "
                    + concreteType.getName());
        }
    }

    private void detectLifecycleMethods(INSTANCE instance) {
        Class<?> instanceClass = instance.getClass();
        LifecycleType.PRE_CONSTRUCT.findIn(instanceClass).ifPresent(this::setPreConstruct);
        LifecycleType.POST_CONSTRUCT.findIn(instanceClass).ifPresent(this::setPostConstruct);
    }

    private void invokeLifecycleMethods(INSTANCE instance, LifecycleType lifecycleType) {
        List<Method> methods = lifecycleType.findAllIn(instance.getClass());
        for (Method method : methods) {
            try {
                method.setAccessible(true);
                method.invoke(instance);
                if (lifecycleType == LifecycleType.PRE_CONSTRUCT) {
                    setPreConstructSuccess(true);
                }
            } catch (Exception e) {
                incrementRetryCount();
                throw new IllegalStateException("[DependencyMetaData] failed to invoke "
                        + lifecycleType.name() + " on " + instance.getClass().getName(), e);
            }
        }
    }

    private void injectAnnotatedFields(INSTANCE instance) {
        Class<?> current = instance.getClass();
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                Inject inject = field.getAnnotation(Inject.class);
                if (inject == null || Modifier.isStatic(field.getModifiers())) {
                    continue;
                }

                Object resolvedDependency = findInstance(field.getType());
                if (resolvedDependency == null) {
                    if (inject.optional()) {
                        continue;
                    }
                    incrementRetryCount();
                    throw new IllegalStateException("[DependencyMetaData] no dependency registered for injected field "
                            + field.getDeclaringClass().getName() + "#" + field.getName()
                            + " of type " + field.getType().getName());
                }

                try {
                    field.setAccessible(true);
                    field.set(instance, resolvedDependency);
                } catch (IllegalAccessException e) {
                    incrementRetryCount();
                    throw new IllegalStateException("[DependencyMetaData] failed to inject field "
                            + field.getDeclaringClass().getName() + "#" + field.getName(), e);
                }
            }
            current = current.getSuperclass();
        }
    }
}
