/*
 * TJVD License (TJ Valentine’s Discretionary License) — Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package org.tavall.dependency;

import org.tavall.dependency.maps.interfaces.IDependencyMap;

/**
 * Creates a dependency instance using the map that will own the resulting binding.
 *
 * @param <T> the produced dependency type
 */
@FunctionalInterface
public interface IDependencyFactory<T> {

    /**
     * Creates one dependency instance.
     *
     * @param dependencyMap the dependency map available to the factory
     * @return the created dependency instance
     */
    T create(IDependencyMap dependencyMap);
}
