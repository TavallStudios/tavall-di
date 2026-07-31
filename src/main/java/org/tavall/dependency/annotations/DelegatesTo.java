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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares one or more dependency tokens that resolve to the annotated concrete instance.
 *
 * <p>A token may be an interface, the annotated concrete type, or any assignable supertype.
 * Every declared token is registered against the same dependency metadata and singleton instance.</p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DelegatesTo {

    /**
     * Returns the dependency tokens that should resolve to the annotated concrete.
     *
     * @return assignable interface or concrete dependency tokens
     */
    Class<?>[] value();
}
