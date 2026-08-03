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
 * Declares a DI-managed concrete and any additional dependency tokens that should resolve to it.
 *
 * <p>The annotated concrete type is always registered implicitly. Every additional token must be
 * assignable from the annotated concrete and resolves to the same dependency metadata and instance.</p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DelegatesTo {

    /**
     * Returns additional dependency tokens that should resolve to the annotated concrete.
     *
     * @return assignable interface or supertype dependency tokens
     */
    Class<?>[] value() default {};
}
