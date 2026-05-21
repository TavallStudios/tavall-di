package org.tavall.dependency.metadata.wrappers;

import org.tavall.dependency.injection.helpers.fixtures.DelegatingRedisService;
import org.tavall.dependency.fixtures.contracts.interfaces.IRedis;
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
