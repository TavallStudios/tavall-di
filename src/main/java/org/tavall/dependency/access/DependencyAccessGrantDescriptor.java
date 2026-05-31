/*
 * TJVD License (TJ Valentine’s Discretionary License) — Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package org.tavall.dependency.access;

import java.util.List;

/**
 * Describes a granted dependency-access interface and its dependency types.
 *
 * @param accessType the access interface token
 * @param dependencyTypes the dependency types granted to the access interface
 */
public record DependencyAccessGrantDescriptor(Class<?> accessType, List<Class<?>> dependencyTypes) {
    public DependencyAccessGrantDescriptor {
        dependencyTypes = dependencyTypes == null ? List.of() : List.copyOf(dependencyTypes);
    }
}
