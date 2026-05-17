package com.tjxjnoobie.api.dependency.metadata.wrappers;

import com.tjxjnoobie.api.dependency.DependencyLoaderAccess;
import com.tjxjnoobie.api.dependency.IDependencyInjectableConcrete;
import com.tjxjnoobie.api.dependency.injection.helpers.fixtures.DelegatingRedisService;
import com.tjxjnoobie.api.dependency.maps.DependencyMap;
import com.tjxjnoobie.api.dependency.metadata.DependencyMetaData;
import com.tjxjnoobie.api.dependency.metadata.wrappers.interfaces.IDependencyInterface;
import com.tjxjnoobie.api.interfaces.IRank;
import com.tjxjnoobie.api.interfaces.IRedis;
import com.tjxjnoobie.api.platform.velocity.startup.VelocityEnabler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNull;

class DependencyAccessMixinTest {

    @AfterEach
    void clearDependencyMap() {
        DependencyMap.getDependencyMap().clear();
        DelegatingRedisService.reset();
    }

    @Test
    void entrypointStyleWrapperResolvesDependenciesThroughInheritedComposedMethods() {
        DependencyMetaData<IRedis, DelegatingRedisService> redisMetaData = new DependencyMetaData<>();
        redisMetaData.populateMetaData(
                IRedis.class,
                DelegatingRedisService.class,
                new DependencyInterface<>(IRedis.class),
                new DependencyInstance<>(DelegatingRedisService.class));

        DependencyMetaData<IRank, TestRankService> rankMetaData = new DependencyMetaData<>();
        rankMetaData.populateMetaData(
                IRank.class,
                TestRankService.class,
                new DependencyInterface<>(IRank.class),
                new DependencyInstance<>(TestRankService.class));

        DependencyMap.getDependencyMap().registerDependency(IRedis.class, redisMetaData);
        DependencyMap.getDependencyMap().registerDependency(IRank.class, rankMetaData);

        VelocityEnabler velocityEnabler = new VelocityEnabler();

        assertFalse(velocityEnabler instanceof IDependencyInterface<?>);
        int connectCallsBefore = DelegatingRedisService.getConnectCalls();

        velocityEnabler.connectToRedis();

        assertEquals(connectCallsBefore + 1, DelegatingRedisService.getConnectCalls());
        assertSame(redisMetaData.getDependencyInstance(), DependencyLoaderAccess.findInstance(IRedis.class));
        assertSame(rankMetaData.getDependencyInstance().getRanks(), velocityEnabler.getRanks());
        assertNull(DependencyLoaderAccess.findInstance(VelocityEnabler.class));
    }

    private static class TestRankService implements IRank, IDependencyInjectableConcrete {
        private final List<String> ranks = new ArrayList<>();

        @Override
        public List<String> getRanks() {
            return ranks;
        }
    }
}
