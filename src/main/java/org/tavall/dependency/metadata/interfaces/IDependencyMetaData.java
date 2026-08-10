/*
 * TJVD License (TJ Valentine’s Discretionary License) — Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package org.tavall.dependency.metadata.interfaces;

import org.tavall.dependency.IDependencyAccess;
import org.tavall.dependency.injection.enums.LifecycleType;
import org.tavall.dependency.metadata.DependencyRole;
import org.tavall.dependency.metadata.wrappers.interfaces.IDependencyInstance;
import org.tavall.dependency.metadata.wrappers.interfaces.IDependencyInterface;
import org.tavall.interfaces.IContext;

import java.lang.reflect.Method;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Canonical metadata contract for a dependency binding and its owned instance.
 *
 * @param <INTERFACE> the primary dependency token type
 * @param <INSTANCE> the concrete instance type
 */
public interface IDependencyMetaData<INTERFACE, INSTANCE> extends IDependencyAccess {

    void populateMetaData(
            Class<? extends INTERFACE> rawDependencyInterface,
            Class<? extends INSTANCE> rawDependencyConcrete,
            IDependencyInterface<INTERFACE> wrappedInterface,
            IDependencyInstance<INSTANCE> wrappedInstance);

    void bindDependencyInstance(
            Class<? extends INTERFACE> rawDependencyInterface,
            Class<? extends INSTANCE> rawDependencyConcrete,
            IDependencyInterface<INTERFACE> wrappedInterface,
            IDependencyInstance<INSTANCE> wrappedInstance,
            INSTANCE dependencyInstance,
            Supplier<? extends INSTANCE> dependencySupplier);

    void createDependencyInstance(Class<? extends INSTANCE> dependencyInstance);

    void setWrappedInterface(IDependencyInterface<INTERFACE> wrappedInterface);

    void setWrappedInstance(IDependencyInstance<INSTANCE> wrappedInstance);

    IDependencyInterface<INTERFACE> getWrappedInterface();

    IDependencyInstance<INSTANCE> getWrappedInstance();

    Class<? extends INTERFACE> getPrimaryInterfaceType();

    Class<? extends INSTANCE> getConcreteType();

    INTERFACE getDependencyInterface();

    INSTANCE getDependencyInstance();

    /**
     * Returns the metadata-owned instance when it satisfies the supplied token.
     */
    @Override
    <T> T findInstance(Class<T> dependencyType);

    /**
     * Returns the metadata-owned instance or fails when the token is incompatible.
     */
    @Override
    <T> T getInstance(Class<T> dependencyType);

    void initializeDependencyInstance();

    void replaceDependencyInstance(Supplier<? extends INSTANCE> dependencySupplier);

    EnumMap<LifecycleType, Method> detectLifecycleForClass(INTERFACE dependencyClass);

    default Set<INTERFACE> getSubDependencies() {
        return new HashSet<>();
    }

    void setSubDependencies(Set<INTERFACE> dependencyClassSet);

    int getDepth();

    void setDepth(int depth);

    default DependencyRole getDependencyRole() {
        return DependencyRole.ISOLATED;
    }

    void setDependencyRole(DependencyRole dependencyRole);

    Method getPreConstruct();

    Method getPostConstruct();

    void setPreConstruct(Method preConstruct);

    void setPostConstruct(Method postConstruct);

    boolean isPreConstructSuccess();

    void setPreConstructSuccess(boolean success);

    int getRetryCount();

    void incrementRetryCount();

    IContext<INTERFACE> getSourceContext();

    void setSourceContext(IContext<INTERFACE> ctx);

    int getPriority();

    void setPriority(int priority);
}
