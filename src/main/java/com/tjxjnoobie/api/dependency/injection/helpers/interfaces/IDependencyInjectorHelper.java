/*
 * TJVD License (TJ Valentine’s Discretionary License) — Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package com.tjxjnoobie.api.dependency.injection.helpers.interfaces;

import com.tjxjnoobie.api.dependency.IDependencyInjectableConcrete;
import com.tjxjnoobie.api.dependency.IDependencyInjectableInterface;
import com.tjxjnoobie.api.internal.InjectionConfig;

/**
 * Interface for dependency injection helper functionality.
 * Provides the entrypoint used to scan and register DI bindings.
 *
 * @param <INTERFACE> the injectable interface token type
 * @param <INSTANCE> the injectable concrete instance type
 */
public interface IDependencyInjectorHelper<
        INTERFACE extends IDependencyInjectableInterface,
        INSTANCE extends IDependencyInjectableConcrete> extends InjectionConfig {

    /**
     * Scans and registers dependencies using the helper's own class loader.
     *
     * @throws Throwable when startup registration fails
     */
    void setupDISystem() throws Throwable;

    /**
     * Scans and registers dependencies using the supplied class loader.
     *
     * @param loader the class loader used for reflective package scanning
     * @throws Throwable when startup registration fails
     */
    void setupDISystem(ClassLoader loader) throws Throwable;

    /**
     * Forces a rescan and re-registration using the helper's own class loader.
     *
     * @throws Throwable when startup registration fails
     */
    void reloadDISystem() throws Throwable;

    /**
     * Forces a rescan and re-registration using the supplied class loader.
     *
     * @param loader the class loader used for reflective package scanning
     * @throws Throwable when startup registration fails
     */
    void reloadDISystem(ClassLoader loader) throws Throwable;

    /**
     * Scans and registers dependencies using the supplied type's class loader.
     *
     * @param type the type whose class loader should be used for scanning
     * @throws Throwable when startup registration fails
     */
    default void setupDISystem(Class<?> type) throws Throwable {
        if (type == null) {
            throw new IllegalArgumentException("type is required");
        }
        setupDISystem(type.getClassLoader());
    }

    /**
     * Scans and registers dependencies for the supplied DI entrypoint.
     *
     * @param entryPoint the object whose class loader should be used for scanning
     * @throws Throwable when startup registration fails
     */
    default void setupDISystem(Object entryPoint) throws Throwable {
        if (entryPoint == null) {
            throw new IllegalArgumentException("entryPoint is required");
        }
        setupDISystem(entryPoint.getClass());
    }

    /**
     * Forces a rescan and re-registration using the supplied type's class loader.
     *
     * @param type the type whose class loader should be used for scanning
     * @throws Throwable when startup registration fails
     */
    default void reloadDISystem(Class<?> type) throws Throwable {
        if (type == null) {
            throw new IllegalArgumentException("type is required");
        }
        reloadDISystem(type.getClassLoader());
    }

    /**
     * Forces a rescan and re-registration using the supplied DI entrypoint.
     *
     * @param entryPoint the object whose class loader should be used for scanning
     * @throws Throwable when startup registration fails
     */
    default void reloadDISystem(Object entryPoint) throws Throwable {
        if (entryPoint == null) {
            throw new IllegalArgumentException("entryPoint is required");
        }
        reloadDISystem(entryPoint.getClass());
    }
}
