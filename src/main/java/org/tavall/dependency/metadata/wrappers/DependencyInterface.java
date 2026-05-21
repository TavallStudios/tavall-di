/*
 * TJVD License (TJ Valentine’s Discretionary License) — Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package org.tavall.dependency.metadata.wrappers;

import org.tavall.dependency.metadata.wrappers.interfaces.IDependencyInterface;

/**
 * Default wrapper implementation for interface-token DI state.
 *
 * @param <INTERFACE> the injectable interface token type
 */
public class DependencyInterface<INTERFACE> implements IDependencyInterface<INTERFACE> {

    public DependencyInterface() {
    }

    /**
     * Creates a wrapper that immediately stores the supplied interface token.
     *
     * @param dependencyInterface the interface token to store
     */
    public DependencyInterface(Class<? extends INTERFACE> dependencyInterface) {
        setDependencyInterfaceWrapperRawClass(dependencyInterface);
    }
}
