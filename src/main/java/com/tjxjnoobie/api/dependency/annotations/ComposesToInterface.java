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
 * Declares that an interface should be composed into one or more generated domain interfaces.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ComposesToInterface {

    /**
     * Returns the generated domain interfaces that should include the annotated contract.
     *
     * @return the composition targets
     */
    Class<?>[] value();

    /**
     * Returns the optional method prefix applied during composition.
     *
     * @return the generated method prefix
     */
    String methodPrefix() default "";
}
