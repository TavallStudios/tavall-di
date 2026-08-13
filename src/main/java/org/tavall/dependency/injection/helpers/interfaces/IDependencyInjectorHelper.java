/*
 * TJVD License (TJ Valentine’s Discretionary License) — Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package org.tavall.dependency.injection.helpers.interfaces;

import org.tavall.dependency.injection.InjectionConfig;

/**
 * Entry point for annotation-driven dependency discovery and registration.
 *
 * <p>Setup performs the initial discovery pass against a class-loader boundary. Reload performs the
 * implementation-defined refresh path for an already configured DI system. Convenience overloads
 * derive the discovery class loader from a class or entry-point object so callers do not need to
 * manually thread class-loader references through application bootstraps.</p>
 *
 * @param <INTERFACE> dependency interface type handled by the injector implementation
 * @param <INSTANCE> dependency implementation type handled by the injector implementation
 */
public interface IDependencyInjectorHelper<INTERFACE, INSTANCE> extends InjectionConfig {

    /**
     * Performs initial dependency discovery using the injector's default class-loader context.
     *
     * @throws Throwable when discovery, validation, construction, or lifecycle initialization fails
     */
    void setupDISystem() throws Throwable;

    /**
     * Performs initial dependency discovery using an explicit class loader.
     *
     * @param loader class loader whose reachable Tavall classes should be considered for discovery
     * @throws Throwable when discovery, validation, construction, or lifecycle initialization fails
     */
    void setupDISystem(ClassLoader loader) throws Throwable;

    /**
     * Reloads dependency discovery using the injector's default class-loader context.
     *
     * @throws Throwable when refresh, validation, construction, or lifecycle initialization fails
     */
    void reloadDISystem() throws Throwable;

    /**
     * Reloads dependency discovery using an explicit class loader.
     *
     * @param loader class loader whose reachable Tavall classes should be considered during refresh
     * @throws Throwable when refresh, validation, construction, or lifecycle initialization fails
     */
    void reloadDISystem(ClassLoader loader) throws Throwable;

    /**
     * Performs initial dependency discovery using the class loader that loaded a reference type.
     *
     * @param type reference type used to select the discovery class loader
     * @throws IllegalArgumentException if {@code type} is {@code null}
     * @throws Throwable when setup using the derived class loader fails
     */
    default void setupDISystem(Class<?> type) throws Throwable {
        if (type == null) {
            throw new IllegalArgumentException("type is required");
        }
        setupDISystem(type.getClassLoader());
    }

    /**
     * Performs initial dependency discovery using the runtime class of an application entry point.
     *
     * @param entryPoint object whose runtime class selects the discovery class loader
     * @throws IllegalArgumentException if {@code entryPoint} is {@code null}
     * @throws Throwable when setup using the derived class loader fails
     */
    default void setupDISystem(Object entryPoint) throws Throwable {
        if (entryPoint == null) {
            throw new IllegalArgumentException("entryPoint is required");
        }
        setupDISystem(entryPoint.getClass());
    }

    /**
     * Reloads dependency discovery using the class loader that loaded a reference type.
     *
     * @param type reference type used to select the discovery class loader
     * @throws IllegalArgumentException if {@code type} is {@code null}
     * @throws Throwable when reload using the derived class loader fails
     */
    default void reloadDISystem(Class<?> type) throws Throwable {
        if (type == null) {
            throw new IllegalArgumentException("type is required");
        }
        reloadDISystem(type.getClassLoader());
    }

    /**
     * Reloads dependency discovery using the runtime class of an application entry point.
     *
     * @param entryPoint object whose runtime class selects the discovery class loader
     * @throws IllegalArgumentException if {@code entryPoint} is {@code null}
     * @throws Throwable when reload using the derived class loader fails
     */
    default void reloadDISystem(Object entryPoint) throws Throwable {
        if (entryPoint == null) {
            throw new IllegalArgumentException("entryPoint is required");
        }
        reloadDISystem(entryPoint.getClass());
    }
}
