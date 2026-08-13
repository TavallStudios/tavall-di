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
 * Canonical metadata contract for one dependency binding and the runtime instance owned by it.
 *
 * <p>Metadata joins the API-facing dependency token, concrete implementation type, wrapper state,
 * lifecycle methods, dependency-graph placement, and source context for one binding. Implementations
 * are also dependency-access objects, allowing their owned instance to satisfy compatible local
 * lookups before falling back to the shared dependency map.</p>
 *
 * @param <INTERFACE> primary dependency token type
 * @param <INSTANCE> concrete instance type
 */
public interface IDependencyMetaData<INTERFACE, INSTANCE> extends IDependencyAccess {

    /**
     * Populates a binding and creates its concrete instance from the supplied class token.
     *
     * <p>The concrete type must be instantiable by the implementation's construction strategy.
     * Implementations should publish wrapper state consistently with the created instance.</p>
     *
     * @param rawDependencyInterface primary API token for the binding
     * @param rawDependencyConcrete concrete implementation type to create
     * @param wrappedInterface wrapper that exposes interface-facing binding state
     * @param wrappedInstance wrapper that exposes concrete-instance binding state
     */
    void populateMetaData(
            Class<? extends INTERFACE> rawDependencyInterface,
            Class<? extends INSTANCE> rawDependencyConcrete,
            IDependencyInterface<INTERFACE> wrappedInterface,
            IDependencyInstance<INSTANCE> wrappedInstance);

    /**
     * Binds an already-created dependency instance and the supplier used for future replacement or
     * recreation semantics.
     *
     * @param rawDependencyInterface primary API token for the binding
     * @param rawDependencyConcrete concrete type the instance must satisfy
     * @param wrappedInterface interface-facing wrapper state
     * @param wrappedInstance concrete-instance wrapper state
     * @param dependencyInstance instance to publish through this metadata
     * @param dependencySupplier supplier associated with the instance; implementations may derive a
     *                           stable supplier from {@code dependencyInstance} when this is null
     */
    void bindDependencyInstance(
            Class<? extends INTERFACE> rawDependencyInterface,
            Class<? extends INSTANCE> rawDependencyConcrete,
            IDependencyInterface<INTERFACE> wrappedInterface,
            IDependencyInstance<INSTANCE> wrappedInstance,
            INSTANCE dependencyInstance,
            Supplier<? extends INSTANCE> dependencySupplier);

    /**
     * Creates and publishes a dependency instance for a concrete type.
     *
     * @param dependencyInstance concrete type to instantiate
     */
    void createDependencyInstance(Class<? extends INSTANCE> dependencyInstance);

    /**
     * Replaces the wrapper used to expose interface-facing binding state.
     *
     * @param wrappedInterface interface wrapper, or {@code null} when no wrapper is required
     */
    void setWrappedInterface(IDependencyInterface<INTERFACE> wrappedInterface);

    /**
     * Replaces the wrapper used to expose concrete-instance binding state.
     *
     * @param wrappedInstance concrete-instance wrapper, or {@code null} when no wrapper is required
     */
    void setWrappedInstance(IDependencyInstance<INSTANCE> wrappedInstance);

    /**
     * Returns the interface-facing wrapper associated with this binding.
     *
     * @return current interface wrapper, or {@code null} when none is configured
     */
    IDependencyInterface<INTERFACE> getWrappedInterface();

    /**
     * Returns the concrete-instance wrapper associated with this binding.
     *
     * @return current instance wrapper, or {@code null} when none is configured
     */
    IDependencyInstance<INSTANCE> getWrappedInstance();

    /**
     * Returns the primary API token under which this dependency is exposed.
     *
     * @return primary dependency token, or {@code null} before binding is established
     */
    Class<? extends INTERFACE> getPrimaryInterfaceType();

    /**
     * Returns the concrete implementation type currently owned by the metadata.
     *
     * @return concrete dependency type, or {@code null} before binding is established
     */
    Class<? extends INSTANCE> getConcreteType();

    /**
     * Returns the dependency through its primary interface view when available.
     *
     * @return interface-facing dependency value, or {@code null} when no compatible value is
     *         currently published
     */
    INTERFACE getDependencyInterface();

    /**
     * Returns the concrete instance currently owned by this metadata.
     *
     * @return current dependency instance, or {@code null} before creation or binding
     */
    INSTANCE getDependencyInstance();

    /**
     * Returns the metadata-owned instance when it satisfies the supplied token.
     *
     * @param dependencyType token to resolve against the owned instance
     * @param <T> requested dependency type
     * @return the owned instance cast to the requested type, or {@code null} when the token is null
     *         or incompatible
     */
    @Override
    <T> T findInstance(Class<T> dependencyType);

    /**
     * Returns the metadata-owned instance or fails when the requested token is unavailable or
     * incompatible.
     *
     * @param dependencyType token the owned instance must satisfy
     * @param <T> requested dependency type
     * @return the owned instance cast to the requested type
     * @throws IllegalArgumentException when {@code dependencyType} is {@code null}
     * @throws IllegalStateException when the owned instance cannot satisfy the token
     */
    @Override
    <T> T getInstance(Class<T> dependencyType);

    /**
     * Runs dependency initialization for the currently owned instance.
     *
     * <p>The standard Tavall lifecycle discovers lifecycle callbacks, executes pre-construction
     * callbacks, performs annotated field injection, and then executes post-construction callbacks.
     * Implementations should fail rather than silently report successful initialization when one of
     * those stages cannot complete.</p>
     */
    void initializeDependencyInstance();

    /**
     * Replaces the metadata-owned dependency instance and initializes the replacement.
     *
     * @param dependencySupplier supplier that creates the replacement instance
     */
    void replaceDependencyInstance(Supplier<? extends INSTANCE> dependencySupplier);

    /**
     * Detects lifecycle callbacks declared for a dependency object's runtime class.
     *
     * @param dependencyClass dependency object whose class should be inspected
     * @return lifecycle-to-method mapping; an empty map when the value is {@code null} or no
     *         callbacks are discovered
     */
    EnumMap<LifecycleType, Method> detectLifecycleForClass(INTERFACE dependencyClass);

    /**
     * Returns dependencies recorded beneath this binding in the dependency graph.
     *
     * <p>The default implementation returns a new empty set. Stateful implementations should expose
     * their actual graph state instead.</p>
     *
     * @return current sub-dependency set
     */
    default Set<INTERFACE> getSubDependencies() {
        return new HashSet<>();
    }

    /**
     * Replaces the recorded sub-dependency set for this binding.
     *
     * @param dependencyClassSet dependencies to associate beneath this metadata
     */
    void setSubDependencies(Set<INTERFACE> dependencyClassSet);

    /**
     * Returns this binding's resolved depth in the dependency graph.
     *
     * @return dependency graph depth
     */
    int getDepth();

    /**
     * Updates this binding's resolved depth in the dependency graph.
     *
     * @param depth dependency graph depth to record
     */
    void setDepth(int depth);

    /**
     * Returns the architectural role assigned to this dependency binding.
     *
     * @return dependency role; the default contract treats unclassified bindings as isolated
     */
    default DependencyRole getDependencyRole() {
        return DependencyRole.ISOLATED;
    }

    /**
     * Assigns the architectural role used to classify this dependency binding.
     *
     * @param dependencyRole role to record
     */
    void setDependencyRole(DependencyRole dependencyRole);

    /**
     * Returns the discovered pre-construction lifecycle method.
     *
     * @return pre-construction callback, or {@code null} when none is known
     */
    Method getPreConstruct();

    /**
     * Returns the discovered post-construction lifecycle method.
     *
     * @return post-construction callback, or {@code null} when none is known
     */
    Method getPostConstruct();

    /**
     * Records the pre-construction lifecycle method associated with this binding.
     *
     * @param preConstruct callback method to record
     */
    void setPreConstruct(Method preConstruct);

    /**
     * Records the post-construction lifecycle method associated with this binding.
     *
     * @param postConstruct callback method to record
     */
    void setPostConstruct(Method postConstruct);

    /**
     * Reports whether pre-construction lifecycle execution has completed successfully.
     *
     * @return {@code true} when pre-construction execution has been recorded as successful
     */
    boolean isPreConstructSuccess();

    /**
     * Records the success state of pre-construction lifecycle execution.
     *
     * @param success success state to record
     */
    void setPreConstructSuccess(boolean success);

    /**
     * Returns the number of lifecycle/injection retry or failure attempts tracked by this metadata.
     *
     * @return current retry count
     */
    int getRetryCount();

    /**
     * Increments the tracked retry/failure count by one.
     */
    void incrementRetryCount();

    /**
     * Returns the context describing where this dependency binding originated.
     *
     * @return source context, or {@code null} when none is recorded
     */
    IContext<INTERFACE> getSourceContext();

    /**
     * Records the context describing where this dependency binding originated.
     *
     * @param ctx source context to associate with the binding
     */
    void setSourceContext(IContext<INTERFACE> ctx);

    /**
     * Returns the dependency's explicit load or resolution priority.
     *
     * @return configured priority
     */
    int getPriority();

    /**
     * Updates the dependency's explicit load or resolution priority.
     *
     * @param priority priority to record
     */
    void setPriority(int priority);
}
