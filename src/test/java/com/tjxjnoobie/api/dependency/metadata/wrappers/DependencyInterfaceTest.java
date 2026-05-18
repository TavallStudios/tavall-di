package com.tjxjnoobie.api.dependency.metadata.wrappers;

import com.tjxjnoobie.api.dependency.injection.helpers.fixtures.DelegatingRedisService;
import com.tjxjnoobie.api.interfaces.IRedis;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class DependencyInterfaceTest {

    @Test
    void storesRealProjectInterfaceTokenAndDelegatedInstance() {
        DependencyInterface<IRedis> dependencyInterface = new DependencyInterface<>(IRedis.class);
        DelegatingRedisService redisService = new DelegatingRedisService();

        dependencyInterface.setDependencyInterface(redisService);

        assertSame(IRedis.class, dependencyInterface.getRawDependencyInterface());
        assertSame(redisService, dependencyInterface.getInterface());
        assertNotNull(dependencyInterface.getDependencyId());
        assertNotNull(dependencyInterface.getCreationTime());
    }

    @Test
    void storesConcreteTokensWhenWrapperIsBuilt() {
        DependencyInterface<IRedis> dependencyInterface = new DependencyInterface<>(DelegatingRedisService.class);

        assertSame(DelegatingRedisService.class, dependencyInterface.getRawDependencyInterface());
        assertNotNull(dependencyInterface.getDependencyId());
    }
}
