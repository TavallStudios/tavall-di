package com.tjxjnoobie.api.dependency.metadata;

import com.tjxjnoobie.api.dependency.injection.helpers.fixtures.DelegatingUtilsService;
import com.tjxjnoobie.api.dependency.maps.DependencyMap;
import com.tjxjnoobie.api.dependency.metadata.wrappers.DependencyInstance;
import com.tjxjnoobie.api.dependency.metadata.wrappers.DependencyInterface;
import com.tjxjnoobie.api.interfaces.IRank;
import com.tjxjnoobie.api.interfaces.IUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DependencyMetaDataTest {

    @BeforeEach
    void resetFixtureState() {
        DependencyMap.getDependencyMap().clear();
    }

    @AfterEach
    void clearDependencyMap() {
        DependencyMap.getDependencyMap().clear();
    }

    @Test
    void resolvesSingletonThroughInterfaceConcreteAndTokenLookup() {
        DependencyMetaData<IUtils, DelegatingUtilsService> metaData = new DependencyMetaData<>();
        metaData.populateMetaData(
                IUtils.class,
                DelegatingUtilsService.class,
                new DependencyInterface<>(IUtils.class),
                new DependencyInstance<>(DelegatingUtilsService.class));

        DependencyMap.getDependencyMap().registerDependency(IUtils.class, metaData);

        IUtils dependencyInterface = metaData.getDependencyInterface();
        DelegatingUtilsService dependencyInstance = metaData.getDependencyInstance();
        IUtils dependencyViaToken = metaData.findInstance(IUtils.class);

        assertSame(dependencyInstance, dependencyInterface);
        assertSame(dependencyInstance, dependencyViaToken);
        assertEquals("generated-4", dependencyInterface.generateRandomID(4));
    }

    @Test
    void returnsNullForMissingBindingsAndThrowsForRequiredLookup() {
        DependencyMetaData<IUtils, DelegatingUtilsService> metaData = new DependencyMetaData<>();

        assertNull(metaData.findInstance(IRank.class));
        assertThrows(IllegalStateException.class, () -> metaData.requireInstance(IRank.class));
    }
}
