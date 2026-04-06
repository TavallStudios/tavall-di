package com.tjxjnoobie.api.dependency.composition;

import com.tjxjnoobie.api.dependency.DependencyLoaderAccess;
import com.tjxjnoobie.api.dependency.composition.domains.IInfrastructureDomain;
import com.tjxjnoobie.api.dependency.maps.DependencyMap;
import com.tjxjnoobie.api.dependency.metadata.DependencyMetaData;
import com.tjxjnoobie.api.dependency.metadata.fixtures.RealProjectMultiInterfaceService;
import com.tjxjnoobie.api.dependency.metadata.wrappers.DependencyInstance;
import com.tjxjnoobie.api.dependency.metadata.wrappers.DependencyInterface;
import com.tjxjnoobie.api.interfaces.IRedis;
import com.tjxjnoobie.api.interfaces.IUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class GeneratedDomainCompositionIntegrationTest {

    @BeforeEach
    void resetFixture() {
        DependencyMap.getDependencyMap().clear();
        RealProjectMultiInterfaceService.reset();
    }

    @Test
    void generatedDomainBridgeResolvesThroughStaticLoaderAccess() {
        InfrastructureMetaData metaData = new InfrastructureMetaData();
        metaData.populateMetaData(
                IRedis.class,
                RealProjectMultiInterfaceService.class,
                new DependencyInterface<>(IRedis.class),
                new DependencyInstance<>(RealProjectMultiInterfaceService.class));
        DependencyMap.getDependencyMap().registerDependency(IRedis.class, metaData);
        DependencyMap.getDependencyMap().registerDependency(IUtils.class, metaData);

        metaData.connectToRedis();
        metaData.createServerID();
        metaData.createGameID();
        metaData.setConfigValue("surface", "generated-domain");

        assertSame(metaData.getDependencyInstance(), metaData.getRedis());
        assertSame(metaData.getDependencyInstance(), metaData.getUtils());
        assertSame(metaData.getDependencyInstance(), DependencyLoaderAccess.findInstance(IRedis.class));
        assertSame(metaData.getDependencyInstance(), DependencyLoaderAccess.findInstance(IUtils.class));
        assertEquals(1, RealProjectMultiInterfaceService.getRedisConnectCalls());
        assertEquals("multi-server", metaData.getServerID());
        assertEquals("multi-game", metaData.getGameID());
        assertEquals("generated-domain", metaData.getConfigValues().get("surface"));
    }

    private static class InfrastructureMetaData
            extends DependencyMetaData<IRedis, RealProjectMultiInterfaceService>
            implements IInfrastructureDomain {
    }
}
