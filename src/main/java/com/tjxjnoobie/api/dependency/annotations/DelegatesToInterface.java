/*
 * TJVD License (TJ Valentine’s Discretionary License) — Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package com.tjxjnoobie.api.dependency.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a concrete class should be registered as the delegate for one or more supplied interface types.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DelegatesToInterface {

    /**
     * Returns the primary interface token that should map to the annotated concrete.
     *
     * @return the primary interface token, or {@link Void} when unset
     */
    Class<?> getLinkedInterface() default Void.class;

    /**
     * Returns any additional interface tokens that should map to the annotated concrete.
     *
     * @return the additional interface tokens
     */
    Class<?>[] getLinkedInterfaces() default {};
}
