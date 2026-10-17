/*
 * TJVD License (TJ Valentine’s Discretionary License) — Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package org.tavall.dependency.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an explicit, reviewable exception where a class is intentionally not managed by tavall-di.
 *
 * <p>Tavall-owned behavior is DI-managed by default. A class cannot escape DI merely because direct
 * construction is convenient. Every exception must document an explicit architectural reason.</p>
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExplicitNonDi {

    /**
     * The explicit, reviewable architectural reason why this class is not managed by tavall-di.
     *
     * @return the non-DI rationale
     */
    String reason();
}
