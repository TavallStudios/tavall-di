package org.tavall.dependency.metadata.wrappers;

import org.tavall.dependency.injection.helpers.fixtures.DelegatingRedisService;
import org.tavall.dependency.fixtures.contracts.interfaces.IRedis;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DependencyInstanceTest {

    @Test
    void storesConcreteClassAndRuntimeInstanceSeparately() {
        DependencyInstance<DelegatingRedisService> dependencyInstance =
                new DependencyInstance<>(DelegatingRedisService.class);
        DelegatingRedisService redisService = new DelegatingRedisService();

        dependencyInstance.setWrappedDependencyInstance(redisService);

        assertSame(DelegatingRedisService.class, dependencyInstance.getDependencyInstanceClass());
        assertSame(redisService, dependencyInstance.getWrappedDependencyInstance());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void rejectsInterfaceTokensWhenConcreteWrapperIsBuilt() {
        assertThrows(IllegalArgumentException.class,
                () -> new DependencyInstance((Class) IRedis.class));
    }
}
