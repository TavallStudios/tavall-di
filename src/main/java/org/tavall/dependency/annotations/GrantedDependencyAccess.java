/*
 * TJVD License (TJ Valentine’s Discretionary License) — Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package org.tavall.dependency.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Generated metadata that records the dependency types granted to an access interface.
 */
@Repeatable(GrantedDependencyAccesses.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface GrantedDependencyAccess {

    /**
     * Returns the access interface that owns the dependency grant.
     *
     * @return the access interface type
     */
    Class<?> accessType();

    /**
     * Returns the concrete dependency types that were authored in the access declaration.
     *
     * @return the dependency type list
     */
    Class<?>[] dependencyTypes();
}
