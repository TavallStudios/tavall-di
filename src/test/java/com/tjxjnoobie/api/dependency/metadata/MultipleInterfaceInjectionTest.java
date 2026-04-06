package com.tjxjnoobie.api.dependency.metadata;

import com.tjxjnoobie.api.dependency.metadata.fixtures.RealProjectMultiInterfaceService;
import com.tjxjnoobie.api.dependency.metadata.wrappers.DependencyInstance;
import com.tjxjnoobie.api.dependency.metadata.wrappers.DependencyInterface;
import com.tjxjnoobie.api.interfaces.IRedis;
import com.tjxjnoobie.api.interfaces.IUtils;
import com.tjxjnoobie.api.machine.data.interfaces.ILocalServerMetaData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class MultipleInterfaceInjectionTest {

    @BeforeEach
    void resetFixture() {
        RealProjectMultiInterfaceService.reset();
    }

    @Test
    void resolvesMultipleRealProjectInterfacesFromSingleMetadataInstance() {
        MultiInterfaceMetaData metaData = new MultiInterfaceMetaData();
        metaData.populateMetaData(
                IRedis.class,
                RealProjectMultiInterfaceService.class,
                new DependencyInterface<>(IRedis.class),
                new DependencyInstance<>(RealProjectMultiInterfaceService.class));

        IRedis redis = metaData.getDependencyInterface();
        RealProjectMultiInterfaceService concrete = metaData.getDependencyInstance();
        IUtils utils = metaData.findInstance(IUtils.class);
        ILocalServerMetaData localServerMetaData = metaData.findInstance(ILocalServerMetaData.class);

        redis.connectToRedis();
        utils.createServerID();
        utils.createGameID();
        utils.setConfigValue("surface", "type-token");

        assertSame(concrete, redis);
        assertSame(concrete, utils);
        assertSame(concrete, localServerMetaData);
        assertEquals(1, RealProjectMultiInterfaceService.getRedisConnectCalls());
        assertEquals("multi-server", utils.getServerID());
        assertEquals("multi-server", localServerMetaData.getLocalServerID());
        assertEquals("multi-game", utils.getGameID());
        assertEquals("type-token", utils.getConfigValues().get("surface"));
        assertEquals("multi-6", utils.generateRandomID(6));
    }

    private static class MultiInterfaceMetaData extends DependencyMetaData<IRedis, RealProjectMultiInterfaceService> {
    }
}
