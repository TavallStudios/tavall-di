/*
 * TJVD License (TJ ValentineÃ¢â‚¬â„¢s Discretionary License) Ã¢â‚¬â€ Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package org.tavall.dependency.metadata.interfaces;

import org.tavall.dependency.metadata.wrappers.interfaces.IDependencyInstance;
import org.tavall.dependency.metadata.wrappers.interfaces.IDependencyInterface;

/**
 * Helper contract for populating metadata from wrapper state.
 *
 * @param <INTERFACE> the injectable interface token type
 * @param <INSTANCE> the injectable concrete instance type
 */
public interface IDependencyMetaDataHelper<INTERFACE, INSTANCE> {

    /**
     * Populates metadata using interface and instance wrapper state.
     *
     * @param dependencyMetaData the metadata being populated
     * @param wrappedInterface the interface wrapper that owns the interface token
     * @param wrappedInstance the concrete wrapper that owns the concrete token
     */
    void populateMetaData(
            IDependencyMetaData<INTERFACE, INSTANCE> dependencyMetaData,
            IDependencyInterface<INTERFACE> wrappedInterface,
            IDependencyInstance<INSTANCE> wrappedInstance);
}
