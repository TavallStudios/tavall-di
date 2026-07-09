/*
 * TJVD License (TJ ValentineÃ¢â‚¬â„¢s Discretionary License) Ã¢â‚¬â€ Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package org.tavall.dependency.metadata.wrappers;

import org.tavall.dependency.metadata.wrappers.interfaces.IDependencyInstance;

/**
 * Default wrapper implementation for concrete-instance DI state.
 *
 * @param <INSTANCE> the injectable concrete instance type
 */
public class DependencyInstance<INSTANCE> implements IDependencyInstance<INSTANCE> {

    public DependencyInstance() {
    }

    /**
     * Creates a wrapper that immediately stores the supplied concrete token.
     *
     * @param rawDependencyInstanceClass the concrete token to store
     */
    public DependencyInstance(Class<? extends INSTANCE> rawDependencyInstanceClass) {
        setWrappedRawInstanceClass(rawDependencyInstanceClass);
    }
}
