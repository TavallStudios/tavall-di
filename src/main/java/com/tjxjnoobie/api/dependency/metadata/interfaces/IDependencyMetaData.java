/*
 * TJVD License (TJ Valentine’s Discretionary License) — Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package com.tjxjnoobie.api.dependency.metadata.interfaces;

import com.tjxjnoobie.api.dependency.IDependencyAccess;
import com.tjxjnoobie.api.dependency.IDependencyInjectableConcrete;
import com.tjxjnoobie.api.dependency.IDependencyInjectableInterface;
import com.tjxjnoobie.api.dependency.injection.enums.LifecycleType;
import com.tjxjnoobie.api.dependency.metadata.wrappers.interfaces.IDependencyInstance;
import com.tjxjnoobie.api.dependency.metadata.wrappers.interfaces.IDependencyInterface;
import com.tjxjnoobie.api.interfaces.IContext;
import com.tjxjnoobie.api.platform.global.enums.DependencyRole;

import java.lang.reflect.Method;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Canonical metadata contract for a DI binding between an interface token and a concrete instance.
 *
 * @param <INTERFACE> the injectable interface token type
 * @param <INSTANCE> the injectable concrete instance type
 */
public interface IDependencyMetaData<
        INTERFACE extends IDependencyInjectableInterface,
        INSTANCE extends IDependencyInjectableConcrete> extends IDependencyAccess {

    /**
     * Populates metadata using the provided interface token, concrete token, and wrappers.
     *
     * @param rawDependencyInterface the interface token used for registration
     * @param rawDependencyConcrete the concrete class that should be instantiated
     * @param wrappedInterface the wrapper that holds interface-facing state
     * @param wrappedInstance the wrapper that holds concrete-facing state
     */
    void populateMetaData(
            Class<? extends INTERFACE> rawDependencyInterface,
            Class<? extends INSTANCE> rawDependencyConcrete,
            IDependencyInterface<INTERFACE> wrappedInterface,
            IDependencyInstance<INSTANCE> wrappedInstance);

    /**
     * Populates metadata around an already-constructed dependency instance.
     *
     * @param rawDependencyInterface the interface token used for registration
     * @param rawDependencyConcrete the concrete class of the supplied instance
     * @param wrappedInterface the wrapper that holds interface-facing state
     * @param wrappedInstance the wrapper that holds concrete-facing state
     * @param dependencyInstance the concrete instance to bind without reflective construction
     * @param dependencySupplier the supplier that should be used for future replacements
     */
    void bindDependencyInstance(
            Class<? extends INTERFACE> rawDependencyInterface,
            Class<? extends INSTANCE> rawDependencyConcrete,
            IDependencyInterface<INTERFACE> wrappedInterface,
            IDependencyInstance<INSTANCE> wrappedInstance,
            INSTANCE dependencyInstance,
            Supplier<? extends INSTANCE> dependencySupplier);

    /**
     * Instantiates and stores the concrete dependency represented by this metadata.
     *
     * @param dependencyInstance the concrete class to instantiate
     */
    void createDependencyInstance(Class<? extends INSTANCE> dependencyInstance);

    /**
     * Stores the interface wrapper associated with this metadata.
     *
     * @param wrappedInterface the wrapper to store
     */
    void setWrappedInterface(IDependencyInterface<INTERFACE> wrappedInterface);

    /**
     * Stores the instance wrapper associated with this metadata.
     *
     * @param wrappedInstance the wrapper to store
     */
    void setWrappedInstance(IDependencyInstance<INSTANCE> wrappedInstance);

    /**
     * Returns the interface wrapper bound to this metadata.
     *
     * @return the stored interface wrapper
     */
    IDependencyInterface<INTERFACE> getWrappedInterface();

    /**
     * Returns the concrete wrapper bound to this metadata.
     *
     * @return the stored instance wrapper
     */
    IDependencyInstance<INSTANCE> getWrappedInstance();

    /**
     * Returns the primary interface token used for this metadata.
     *
     * @return the primary interface token
     */
    Class<? extends INTERFACE> getPrimaryInterfaceType();

    /**
     * Returns the concrete class token stored by this metadata.
     *
     * @return the concrete class token
     */
    Class<? extends INSTANCE> getConcreteType();

    /**
     * Returns the interface-facing view for the resolved dependency.
     *
     * @return the resolved interface view, or {@code null} when unavailable
     */
    INTERFACE getDependencyInterface();

    /**
     * Returns the concrete dependency instance owned by this metadata.
     *
     * @return the resolved concrete instance, or {@code null} when unavailable
     */
    INSTANCE getDependencyInstance();

    /**
     * Executes runtime initialization for the currently bound dependency instance.
     * This includes lifecycle callbacks and field injection.
     */
    void initializeDependencyInstance();

    /**
     * Replaces the concrete dependency instance owned by this metadata.
     *
     * @param dependencySupplier the factory used to refresh the concrete instance
     */
    void replaceDependencyInstance(Supplier<? extends INSTANCE> dependencySupplier);

    /**
     * Detects lifecycle callback methods on the supplied dependency instance.
     *
     * @param dependencyClass the dependency instance to inspect
     * @return a lifecycle-to-method map for discovered callbacks
     */
    EnumMap<LifecycleType, Method> detectLifecycleForClass(INTERFACE dependencyClass);

    /**
     * Returns the discovered sub-dependencies for this metadata.
     *
     * @return the current sub-dependency set
     */
    default Set<INTERFACE> getSubDependencies() {
        return new HashSet<>();
    }

    /**
     * Replaces the discovered sub-dependency set.
     *
     * @param dependencyClassSet the sub-dependencies to store
     */
    void setSubDependencies(Set<INTERFACE> dependencyClassSet);

    /**
     * Returns the current dependency depth.
     *
     * @return the stored dependency depth
     */
    int getDepth();

    /**
     * Stores the dependency depth used during traversal.
     *
     * @param depth the traversal depth to store
     */
    void setDepth(int depth);

    /**
     * Returns the dependency role recorded for this metadata.
     *
     * @return the stored dependency role
     */
    default DependencyRole getDependencyRole() {
        return DependencyRole.ISOLATED;
    }

    /**
     * Stores the dependency role recorded for this metadata.
     *
     * @param dependencyRole the role to store
     */
    void setDependencyRole(DependencyRole dependencyRole);

    /**
     * Returns the pre-construct lifecycle callback, if one was detected.
     *
     * @return the pre-construct method
     */
    Method getPreConstruct();

    /**
     * Returns the post-construct lifecycle callback, if one was detected.
     *
     * @return the post-construct method
     */
    Method getPostConstruct();

    /**
     * Stores the pre-construct lifecycle callback.
     *
     * @param preConstruct the callback to store
     */
    void setPreConstruct(Method preConstruct);

    /**
     * Stores the post-construct lifecycle callback.
     *
     * @param preConstruct the callback to store
     */
    void setPostConstruct(Method preConstruct);

    /**
     * Returns whether the pre-construct callback has already completed successfully.
     *
     * @return {@code true} when pre-construct has succeeded
     */
    boolean isPreConstructSuccess();

    /**
     * Stores the pre-construct callback status.
     *
     * @param success the callback status to store
     */
    void setPreConstructSuccess(boolean success);

    /**
     * Returns the number of retry attempts recorded for this metadata.
     *
     * @return the retry count
     */
    int getRetryCount();

    /**
     * Increments the retry counter for this metadata.
     */
    void incrementRetryCount();

    /**
     * Returns the source context that produced this metadata.
     *
     * @return the source context, or {@code null} when unset
     */
    IContext<INTERFACE> getSourceContext();

    /**
     * Stores the source context that produced this metadata.
     *
     * @param ctx the context to store
     */
    void setSourceContext(IContext<INTERFACE> ctx);

    /**
     * Returns the metadata priority used during ordering.
     *
     * @return the stored priority
     */
    int getPriority();

    /**
     * Stores the metadata priority used during ordering.
     *
     * @param priority the priority to store
     */
    void setPriority(int priority);
}
