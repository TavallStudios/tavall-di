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
import com.tjxjnoobie.api.dependency.IDependencyInjectableConcrete;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Stateful mixin for objects that expose concrete-instance DI behavior.
 *
 * @param <INSTANCE> the injectable concrete instance type
 */
public interface IDependencyInstance<INSTANCE extends IDependencyInjectableConcrete> extends IDependencyAccess {
    Map<IDependencyInstance<?>, InstanceState<?>> STATE = Collections.synchronizedMap(new WeakHashMap<>());

    /**
     * Returns the wrapped concrete dependency instance.
     *
     * @return the wrapped dependency instance
     */
    default INSTANCE getWrappedDependencyInstance() {
        return state().dependencyInstance;
    }

    /**
     * Returns the raw concrete class token stored in this wrapper state.
     *
     * @return the wrapped concrete class token
     */
    default Class<? extends INSTANCE> getDependencyInstanceClass() {
        return state().dependencyInstanceRawClass;
    }

    /**
     * Stores the wrapped concrete dependency instance.
     *
     * @param dependencyInstance the concrete instance to store
     */
    default void setWrappedDependencyInstance(INSTANCE dependencyInstance) {
        state().dependencyInstance = dependencyInstance;
    }

    /**
     * Stores the raw concrete class token after validating that it is instantiable.
     *
     * @param rawDependencyInstanceClass the concrete class token to store
     */
    default void setWrappedRawInstanceClass(Class<? extends INSTANCE> rawDependencyInstanceClass) {
        validateConcreteType(rawDependencyInstanceClass);
        state().dependencyInstanceRawClass = rawDependencyInstanceClass;
    }

    private InstanceState<INSTANCE> state() {
        synchronized (STATE) {
            @SuppressWarnings("unchecked")
            InstanceState<INSTANCE> state = (InstanceState<INSTANCE>) STATE.computeIfAbsent(this, ignored -> new InstanceState<>());
            return state;
        }
    }

    private void validateConcreteType(Class<?> concreteType) {
        if (concreteType == null) {
            throw new IllegalArgumentException("[DependencyInstance] concrete token is required");
        }
        if (concreteType.isInterface()) {
            throw new IllegalArgumentException("[DependencyInstance] concrete token cannot be an interface: "
                    + concreteType.getName());
        }
        if (Modifier.isAbstract(concreteType.getModifiers())) {
            throw new IllegalArgumentException("[DependencyInstance] concrete token cannot be abstract: "
                    + concreteType.getName());
        }
    }

    /**
     * Internal wrapper state used by concrete-instance DI mixins.
     *
     * @param <T> the concrete instance type
     */
    final class InstanceState<T extends IDependencyInjectableConcrete> {
        private T dependencyInstance;
        private Class<? extends T> dependencyInstanceRawClass;
    }
}
