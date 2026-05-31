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
 * Describes a granted dependency-access interface as source-level type names.
 *
 * @param accessType the access interface type name
 * @param dependencyTypes the dependency type names granted to the access interface
 */
public record DependencyAccessSourceGrantDescriptor(String accessType, List<String> dependencyTypes) {
    public DependencyAccessSourceGrantDescriptor {
        dependencyTypes = dependencyTypes == null ? List.of() : List.copyOf(dependencyTypes);
    }
}
