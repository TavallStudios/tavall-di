/*
 * TJVD License (TJ Valentine’s Discretionary License) — Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package com.tjxjnoobie.api.dependency.metadata.wrappers.interfaces;

import com.tjxjnoobie.api.dependency.IDependencyAccess;
import com.tjxjnoobie.api.dependency.IDependencyInjectableInterface;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Stateful mixin for objects that expose interface-token DI behavior.
 *
 * @param <INTERFACE> the injectable interface token type
 */
public interface IDependencyInterface<INTERFACE extends IDependencyInjectableInterface> extends IDependencyAccess {
    Map<IDependencyInterface<?>, InterfaceState<?>> STATE = Collections.synchronizedMap(new WeakHashMap<>());

    /**
     * Returns the synthetic identifier assigned to this wrapper state.
     *
     * @return the dependency identifier
     */
    default UUID getDependencyId() {
        return state().dependencyId;
    }

    /**
     * Returns the interface-facing object stored in this wrapper state.
     *
     * @return the wrapped interface implementation
     */
    default INTERFACE getInterface() {
        return state().dependencyInterface;
    }

    /**
     * Returns the raw interface token stored in this wrapper state.
     *
     * @return the wrapped interface token
     */
    default Class<? extends INTERFACE> getRawDependencyInterface() {
        return state().rawDependencyInterfaceClass;
    }

    /**
     * Returns the wrapper creation timestamp.
     *
     * @return the creation time
     */
    default Date getCreationTime() {
        return state().creationTime;
    }

    /**
     * Stores the raw interface token after validating that it is an interface.
     *
     * @param rawDependencyInterfaceClass the interface token to store
     */
    default void setDependencyInterfaceWrapperRawClass(Class<? extends INTERFACE> rawDependencyInterfaceClass) {
        validateInterfaceType(rawDependencyInterfaceClass);
        state().rawDependencyInterfaceClass = rawDependencyInterfaceClass;
    }

    /**
     * Stores the interface-facing dependency value.
     *
     * @param dependencyInterface the interface-facing dependency value
     */
    default void setDependencyInterface(INTERFACE dependencyInterface) {
        state().dependencyInterface = dependencyInterface;
    }

    /**
     * Replaces the wrapper identifier.
     *
     * @param dependencyId the new wrapper identifier
     */
    default void setDependencyId(UUID dependencyId) {
        state().dependencyId = dependencyId == null ? UUID.randomUUID() : dependencyId;
    }

    /**
     * Replaces the wrapper creation timestamp.
     *
     * @param creationTime the new creation timestamp
     */
    default void setCreationTime(Date creationTime) {
        state().creationTime = creationTime == null ? new Date() : creationTime;
    }

    private InterfaceState<INTERFACE> state() {
        synchronized (STATE) {
            @SuppressWarnings("unchecked")
            InterfaceState<INTERFACE> state = (InterfaceState<INTERFACE>) STATE.computeIfAbsent(this, ignored -> new InterfaceState<>());
            return state;
        }
    }

    private void validateInterfaceType(Class<?> interfaceType) {
        if (interfaceType == null) {
            throw new IllegalArgumentException("[DependencyInterface] interface token is required");
        }
        if (!interfaceType.isInterface()) {
            throw new IllegalArgumentException("[DependencyInterface] wrapper token must be an interface: "
                    + interfaceType.getName());
        }
    }

    /**
     * Internal wrapper state used by interface-based DI mixins.
     *
     * @param <T> the interface token type
     */
    final class InterfaceState<T extends IDependencyInjectableInterface> {
        private UUID dependencyId = UUID.randomUUID();
        private Date creationTime = new Date();
        private T dependencyInterface;
        private Class<? extends T> rawDependencyInterfaceClass;
    }
}
