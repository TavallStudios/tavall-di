/*
 * TJVD License (TJ ValentineÃ¢â‚¬â„¢s Discretionary License) Ã¢â‚¬â€ Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package org.tavall.dependency.access;

import org.tavall.dependency.annotations.GrantedDependencyAccess;

import java.util.ArrayList;
import java.util.List;

/**
 * Expands grant annotations into normalized grant descriptors.
 */
public final class DependencyTypeExpansionHandler {
    private final DependencyAccessValidationHandler validationHandler = new DependencyAccessValidationHandler();

    /**
     * Expands the supplied grant annotations into immutable descriptors.
     *
     * @param grants the grant annotations to expand
     * @return the expanded grant descriptors in declaration order
     */
    public List<DependencyAccessGrantDescriptor> expandGrantedDependencyAccesses(GrantedDependencyAccess[] grants) {
        List<DependencyAccessGrantDescriptor> descriptors = new ArrayList<>();
        if (grants == null || grants.length == 0) {
            return descriptors;
        }

        for (GrantedDependencyAccess grant : grants) {
            if (grant == null) {
                continue;
            }

            Class<?> accessType = grant.accessType();
            Class<?>[] dependencyTypes = grant.dependencyTypes();
            validationHandler.validateGrant(accessType, dependencyTypes);

            List<Class<?>> normalizedDependencyTypes = new ArrayList<>(dependencyTypes.length);
            for (Class<?> dependencyType : dependencyTypes) {
                normalizedDependencyTypes.add(dependencyType);
            }

            descriptors.add(new DependencyAccessGrantDescriptor(accessType, normalizedDependencyTypes));
        }

        return descriptors;
    }
}
