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
 */
public interface IDependencyInjectorHelper<INTERFACE, INSTANCE> extends InjectionConfig {

    void setupDISystem() throws Throwable;

    void setupDISystem(ClassLoader loader) throws Throwable;

    void reloadDISystem() throws Throwable;

    void reloadDISystem(ClassLoader loader) throws Throwable;

    default void setupDISystem(Class<?> type) throws Throwable {
        if (type == null) {
            throw new IllegalArgumentException("type is required");
        }
        setupDISystem(type.getClassLoader());
    }

    default void setupDISystem(Object entryPoint) throws Throwable {
        if (entryPoint == null) {
            throw new IllegalArgumentException("entryPoint is required");
        }
        setupDISystem(entryPoint.getClass());
    }

    default void reloadDISystem(Class<?> type) throws Throwable {
        if (type == null) {
            throw new IllegalArgumentException("type is required");
        }
        reloadDISystem(type.getClassLoader());
    }

    default void reloadDISystem(Object entryPoint) throws Throwable {
        if (entryPoint == null) {
            throw new IllegalArgumentException("entryPoint is required");
        }
        reloadDISystem(entryPoint.getClass());
    }
}
