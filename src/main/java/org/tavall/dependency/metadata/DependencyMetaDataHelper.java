/*
 * TJVD License (TJ ValentineÃ¢â‚¬â„¢s Discretionary License) Ã¢â‚¬â€ Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package org.tavall.dependency.metadata;

import org.tavall.dependency.metadata.interfaces.IDependencyMetaData;
import org.tavall.dependency.metadata.interfaces.IDependencyMetaDataHelper;
import org.tavall.dependency.metadata.wrappers.interfaces.IDependencyInstance;
import org.tavall.dependency.metadata.wrappers.interfaces.IDependencyInterface;
import org.tavall.logging.Log;

/**
 * Default helper that transfers interface and instance wrapper state into metadata.
 *
 * @param <INTERFACE> the injectable interface token type
 * @param <INSTANCE> the injectable concrete instance type
 */
public class DependencyMetaDataHelper<INTERFACE, INSTANCE> implements IDependencyMetaDataHelper<INTERFACE, INSTANCE> {

    @Override
    public void populateMetaData(
            IDependencyMetaData<INTERFACE, INSTANCE> dependencyMetaData,
            IDependencyInterface<INTERFACE> wrappedInterface,
            IDependencyInstance<INSTANCE> wrappedInstance) {

        if (dependencyMetaData == null || wrappedInterface == null || wrappedInstance == null) {
            Log.error("[DependencyMetaDataHelper] dependencyMetaData, wrappedInterface, or wrappedInstance is null");
            return;
        }

        Class<? extends INTERFACE> rawDependencyInterface = wrappedInterface.getRawDependencyInterface();
        Class<? extends INSTANCE> rawDependencyConcrete = wrappedInstance.getDependencyInstanceClass();

        dependencyMetaData.populateMetaData(rawDependencyInterface, rawDependencyConcrete, wrappedInterface, wrappedInstance);
    }
}
