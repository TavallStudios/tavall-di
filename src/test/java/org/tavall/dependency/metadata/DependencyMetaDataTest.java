package org.tavall.dependency.metadata;

import org.tavall.dependency.injection.helpers.fixtures.DelegatingUtilsService;
import org.tavall.dependency.maps.DependencyMap;
import org.tavall.dependency.metadata.wrappers.DependencyInstance;
import org.tavall.dependency.metadata.wrappers.DependencyInterface;
import org.tavall.dependency.fixtures.contracts.interfaces.IRank;
import org.tavall.dependency.fixtures.contracts.interfaces.IUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @Test
    void defaultMetadataStateRemainsIsolatedAndEmpty() {
        DependencyMetaData<IUtils, DelegatingUtilsService> metaData = new DependencyMetaData<>();

        assertTrue(metaData.getSubDependencies().isEmpty());
        assertEquals(0, metaData.getDepth());
        assertEquals(DependencyRole.ISOLATED, metaData.getDependencyRole());
        assertEquals(0, metaData.getPriority());
        assertEquals(0, metaData.getRetryCount());
    }
}
