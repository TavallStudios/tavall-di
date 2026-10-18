/*
 * TJVD License (TJ Valentine’s Discretionary License) — Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package org.tavall.dependency;

/**
 * Creates a foreign dependency using ordinary typed dependency access where inputs are required.
 *
 * @param <T> the produced dependency type
 */
@FunctionalInterface
public interface IDependencyFactory<T> {

    /**
     * Creates one dependency instance.
     *
     * @return the created dependency instance
     */
    T create();
}
