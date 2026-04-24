package com.tjxjnoobie.api.dependency;

import com.tjxjnoobie.api.dependency.fixtures.ConstructorBoundUtilsService;
import com.tjxjnoobie.api.dependency.fixtures.LifecycleAwareRedisService;
import com.tjxjnoobie.api.dependency.injection.helpers.DependencyInjectorHelper;
import com.tjxjnoobie.api.dependency.injection.helpers.lifecyclefixtures.LifecycleDelegatingRedisService;
import com.tjxjnoobie.api.dependency.maps.DependencyMap;
import com.tjxjnoobie.api.interfaces.IRedis;
import com.tjxjnoobie.api.interfaces.IUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependencyLifecycleTest {
    private static final String LIFECYCLE_FIXTURE_PACKAGE =
            "com.tjxjnoobie.api.dependency.injection.helpers.lifecyclefixtures";

    @BeforeEach
    void initializeDependencyState() {
        clearDependencyState();
    }

    @AfterEach
    void clearDependencyState() {
        DependencyMap.getDependencyMap().clear();
        DependencyLoader.clearNamedLoaders();
        LifecycleDelegatingRedisService.reset();
    }

    @Test
    void directRegistrationInjectsFieldsAndRunsLifecycleInPriorityOrder() {
        ConstructorBoundUtilsService utils = new ConstructorBoundUtilsService("lifecycle-utils");
        LifecycleAwareRedisService redis = new LifecycleAwareRedisService();

        DependencyLoaderAccess.registerInstance(IUtils.class, utils);
        IRedis registered = DependencyLoaderAccess.registerInstance(IRedis.class, redis);

        assertSame(redis, registered);
        assertSame(utils, redis.getInjectedUtils());
        assertEquals(List.of("pre-1", "pre-2", "post"), redis.getLifecycleEvents());
    }

    @Test
    void replaceInstanceReinitializesInjectedFieldsAndLifecycleCallbacks() {
        ConstructorBoundUtilsService utils = new ConstructorBoundUtilsService("lifecycle-utils");
        DependencyLoaderAccess.registerInstance(IUtils.class, utils);
        LifecycleAwareRedisService original = new LifecycleAwareRedisService();
        DependencyLoaderAccess.registerInstance(IRedis.class, original);

        IRedis replacement = DependencyLoaderAccess.replaceInstance(IRedis.class, LifecycleAwareRedisService::new);

        assertNotSame(original, replacement);
        assertTrue(replacement instanceof LifecycleAwareRedisService);
        LifecycleAwareRedisService replacementRedis = (LifecycleAwareRedisService) replacement;
        assertSame(utils, replacementRedis.getInjectedUtils());
        assertEquals(List.of("pre-1", "pre-2", "post"), replacementRedis.getLifecycleEvents());
    }

    @Test
    void annotationBootstrapInitializesLifecycleAfterBindingsAreRegistered() {
        DependencyInjectorHelper<
                com.tjxjnoobie.api.dependency.IDependencyInjectableInterface,
                com.tjxjnoobie.api.dependency.IDependencyInjectableConcrete> helper =
                new DependencyInjectorHelper<>();
        helper.setBasePackage(LIFECYCLE_FIXTURE_PACKAGE);

        helper.setupDISystem();

        IRedis redis = DependencyLoaderAccess.requireInstance(IRedis.class);
        assertTrue(redis instanceof LifecycleDelegatingRedisService);
        LifecycleDelegatingRedisService lifecycleRedis = (LifecycleDelegatingRedisService) redis;
        assertSame(DependencyLoaderAccess.requireInstance(IUtils.class), lifecycleRedis.getInjectedUtils());
        assertTrue(LifecycleDelegatingRedisService.isPostConstructCalled());
    }

}
