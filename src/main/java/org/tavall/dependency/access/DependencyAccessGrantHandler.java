/*
 * TJVD License (TJ Valentine’s Discretionary License) — Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package org.tavall.dependency.access;

import org.tavall.dependency.annotations.DepAccess;
import org.tavall.dependency.annotations.GrantDependencyAccess;
import org.tavall.dependency.annotations.GrantedDependencyAccess;
import org.tavall.dependency.internal.DependencyTypeResult;
import org.tavall.dependency.internal.DependencyTypeResolver;

import java.util.ArrayList;
import java.util.List;

/**
 * Runtime access to granted dependency metadata generated for a class.
 */
public final class DependencyAccessGrantHandler {
    private static final String METADATA_SUFFIX = "DependencyAccessMetadata";

    private final DependencyTypeExpansionHandler expansionHandler = new DependencyTypeExpansionHandler();

    /**
     * Returns all grant descriptors for the supplied type, including any generated companion metadata.
     *
     * @param dependencyType the class to inspect
     * @return the grant descriptors in declaration order
     */
    public List<DependencyAccessGrantDescriptor> findGrantedDependencyAccesses(Class<?> dependencyType) {
        List<DependencyAccessGrantDescriptor> descriptors = new ArrayList<>();
        if (dependencyType == null) {
            return descriptors;
        }

        Class<?> metadataType = resolveMetadataType(dependencyType);
        if (metadataType != null && metadataType != dependencyType) {
            descriptors.addAll(expansionHandler.expandGrantedDependencyAccesses(
                    metadataType.getAnnotationsByType(GrantedDependencyAccess.class)));
            if (!descriptors.isEmpty()) {
                return descriptors;
            }
        }

        descriptors.addAll(expansionHandler.expandGrantedDependencyAccesses(
                dependencyType.getAnnotationsByType(GrantedDependencyAccess.class)));
        if (!descriptors.isEmpty()) {
            return descriptors;
        }

        if (dependencyType.isAnnotationPresent(GrantDependencyAccess.class)) {
            DependencyTypeResult dependencyTypeResult =
                    DependencyTypeResolver.resolveTypeParameters(dependencyType, DepAccess.class);
            Class<?>[] dependencyTypes = dependencyTypeResult.getTypeArguments();
            if (dependencyTypes.length > 0) {
                DependencyAccessValidationHandler validationHandler = new DependencyAccessValidationHandler();
                validationHandler.validateGrant(DepAccess.class, dependencyTypes);

                List<Class<?>> dependencyTypeList = new ArrayList<>(dependencyTypes.length);
                for (Class<?> dependencyClass : dependencyTypes) {
                    dependencyTypeList.add(dependencyClass);
                }
                descriptors.add(new DependencyAccessGrantDescriptor(DepAccess.class, dependencyTypeList));
            }
        }
        return descriptors;
    }

    /**
     * Returns whether any grant metadata is available for the supplied type.
     *
     * @param dependencyType the class to inspect
     * @return {@code true} when at least one grant descriptor is available
     */
    public boolean hasGrantedDependencyAccess(Class<?> dependencyType) {
        return !findGrantedDependencyAccesses(dependencyType).isEmpty();
    }

    private Class<?> resolveMetadataType(Class<?> dependencyType) {
        String metadataClassName = dependencyType.getName() + METADATA_SUFFIX;
        try {
            return Class.forName(metadataClassName, false, dependencyType.getClassLoader());
        } catch (ClassNotFoundException exception) {
            return dependencyType;
        }
    }
}
