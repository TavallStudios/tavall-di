/*
 * TJVD License (TJ Valentine’s Discretionary License) — Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package org.tavall.dependency.metadata;

import org.tavall.dependency.annotations.Inject;
import org.tavall.dependency.injection.enums.LifecycleType;
import org.tavall.dependency.metadata.interfaces.IDependencyMetaData;
import org.tavall.dependency.metadata.wrappers.interfaces.IDependencyInstance;
import org.tavall.dependency.metadata.wrappers.interfaces.IDependencyInterface;
import org.tavall.interfaces.IContext;

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
 * Default metadata implementation and authoritative owner of one dependency instance.
 */
public class DependencyMetaData<INTERFACE, INSTANCE>
        implements IDependencyMetaData<INTERFACE, INSTANCE> {
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
            primaryInterfaceType = resolvedInterface;
        }
        if (resolvedConcrete != null) {
            concreteType = resolvedConcrete;
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
        Supplier<INSTANCE> resolvedSupplier = dependencySupplier == null
                ? () -> dependencyInstance
                : () -> (INSTANCE) dependencySupplier.get();
        setDependencySupplier(resolvedSupplier);
        publishDependencyInstance(dependencyInstance);
    }

    private void assignBinding(
            Class<? extends INTERFACE> rawDependencyInterface,
            Class<? extends INSTANCE> rawDependencyConcrete,
            IDependencyInterface<INTERFACE> wrappedInterface,
            IDependencyInstance<INSTANCE> wrappedInstance) {
        validateDependencyType(rawDependencyInterface);
        validateConcreteType(rawDependencyConcrete, true);

        primaryInterfaceType = rawDependencyInterface;
        concreteType = rawDependencyConcrete;
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
        concreteType = dependencyInstanceClass;
        setDependencySupplier(() -> instantiate(dependencyInstanceClass));
        publishDependencyInstance(dependencySupplier.get());
    }

    private INSTANCE instantiate(Class<? extends INSTANCE> dependencyInstanceClass) {
        try {
            var constructor = dependencyInstanceClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to instantiate dependency " + dependencyInstanceClass.getName(),
                    exception);
        }
    }

    public void setDependencySupplier(Supplier<INSTANCE> supplier) {
        dependencySupplier = supplier;
    }

    @Override
    public void replaceDependencyInstance(Supplier<? extends INSTANCE> dependencySupplier) {
        if (dependencySupplier == null) {
            throw new IllegalArgumentException("[DependencyMetaData] replacement supplier is required");
        }

        INSTANCE replacement = dependencySupplier.get();
        if (replacement == null) {
            String concreteName = concreteType == null ? "unknown dependency" : concreteType.getName();
            throw new IllegalStateException("Replacement supplier returned null for " + concreteName);
        }

        if (concreteType == null || !concreteType.isInstance(replacement)) {
            @SuppressWarnings("unchecked")
            Class<? extends INSTANCE> replacementType = (Class<? extends INSTANCE>) replacement.getClass();
            concreteType = replacementType;
            if (wrappedInstance != null) {
                wrappedInstance.setWrappedRawInstanceClass(replacementType);
            }
        }

        @SuppressWarnings("unchecked")
        Supplier<INSTANCE> resolvedSupplier = () -> (INSTANCE) replacement;
        setDependencySupplier(resolvedSupplier);
        publishDependencyInstance(replacement);
        initializeDependencyInstance();
    }

    private void publishDependencyInstance(INSTANCE instance) {
        dependencyInstance = instance;
        if (wrappedInstance != null) {
            wrappedInstance.setWrappedDependencyInstance(instance);
        }
        if (wrappedInterface != null) {
            wrappedInterface.setDependencyInterface(getDependencyInterface());
        }
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
        if (wrappedInterface != null) {
            return wrappedInterface.getInterface();
        }
        return null;
    }

    @Override
    public INSTANCE getDependencyInstance() {
        return dependencyInstance;
    }

    @Override
    public <T> T findInstance(Class<T> dependencyType) {
        if (dependencyType == null || !dependencyType.isInstance(dependencyInstance)) {
            return null;
        }
        return dependencyType.cast(dependencyInstance);
    }

    @Override
    public <T> T getInstance(Class<T> dependencyType) {
        if (dependencyType == null) {
            throw new IllegalArgumentException("dependencyType is required");
        }

        T resolved = findInstance(dependencyType);
        if (resolved != null) {
            return resolved;
        }

        String concreteName = dependencyInstance == null
                ? "no instance"
                : dependencyInstance.getClass().getName();
        throw new IllegalStateException(
                "Dependency metadata cannot expose " + dependencyType.getName()
                        + " from " + concreteName);
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
    public EnumMap<LifecycleType, Method> detectLifecycleForClass(INTERFACE dependencyClass) {
        EnumMap<LifecycleType, Method> methods = new EnumMap<>(LifecycleType.class);
        if (dependencyClass == null) {
            return methods;
        }
        for (LifecycleType lifecycleType : LifecycleType.values()) {
            lifecycleType.findIn(dependencyClass.getClass())
                    .ifPresent(method -> methods.put(lifecycleType, method));
        }
        return methods;
    }

    @Override
    public Set<INTERFACE> getSubDependencies() {
        return subDependencies;
    }

    @Override
    public void setSubDependencies(Set<INTERFACE> dependencyClassSet) {
        subDependencies = dependencyClassSet == null ? new HashSet<>() : dependencyClassSet;
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
        sourceContext = ctx;
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
        return dependencyType != null && dependencyType.isInstance(dependencyInstance)
                ? dependencyInstance
                : null;
    }

    @Override
    public IDependencyMetaData<?, ?> resolveLocalMetaData(Class<?> dependencyType) {
        return resolveLocalInstance(dependencyType) == null ? null : this;
    }

    private void validateDependencyType(Class<?> dependencyType) {
        if (dependencyType == null) {
            throw new IllegalArgumentException("[DependencyMetaData] dependency token is required");
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
        if (requireInstantiable && Modifier.isAbstract(concreteType.getModifiers())) {
            throw new IllegalArgumentException("[DependencyMetaData] concrete token cannot be abstract: "
                    + concreteType.getName());
        }
    }

    private void detectLifecycleMethods(INSTANCE instance) {
        LifecycleType.PRE_CONSTRUCT.findIn(instance.getClass()).ifPresent(this::setPreConstruct);
        LifecycleType.POST_CONSTRUCT.findIn(instance.getClass()).ifPresent(this::setPostConstruct);
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
            } catch (Exception exception) {
                incrementRetryCount();
                throw new IllegalStateException("[DependencyMetaData] failed to invoke "
                        + lifecycleType.name() + " on " + instance.getClass().getName(), exception);
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

                Object resolvedDependency = getDependencyMap().findInstance(field.getType());
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
                } catch (IllegalAccessException exception) {
                    incrementRetryCount();
                    throw new IllegalStateException("[DependencyMetaData] failed to inject field "
                            + field.getDeclaringClass().getName() + "#" + field.getName(), exception);
                }
            }
            current = current.getSuperclass();
        }
    }
}
