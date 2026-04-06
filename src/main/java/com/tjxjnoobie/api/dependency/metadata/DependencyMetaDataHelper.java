/*
 * TJVD License (TJ Valentine’s Discretionary License) — Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package com.tjxjnoobie.api.dependency.metadata;

import com.tjxjnoobie.api.dependency.IDependencyInjectableConcrete;
import com.tjxjnoobie.api.dependency.IDependencyInjectableInterface;
import com.tjxjnoobie.api.dependency.metadata.interfaces.IDependencyMetaData;
import com.tjxjnoobie.api.dependency.metadata.interfaces.IDependencyMetaDataHelper;
import com.tjxjnoobie.api.dependency.metadata.wrappers.interfaces.IDependencyInstance;
import com.tjxjnoobie.api.dependency.metadata.wrappers.interfaces.IDependencyInterface;
import com.tjxjnoobie.api.platform.global.console.Log;

/**
 * Default helper that transfers interface and instance wrapper state into metadata.
 *
 * @param <INTERFACE> the injectable interface token type
 * @param <INSTANCE> the injectable concrete instance type
 */
public class DependencyMetaDataHelper<
        INTERFACE extends IDependencyInjectableInterface,
        INSTANCE extends IDependencyInjectableConcrete> implements IDependencyMetaDataHelper<INTERFACE, INSTANCE> {

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
