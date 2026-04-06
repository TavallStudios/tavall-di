package com.tjxjnoobie.api.dependency.annotations;

import com.tjxjnoobie.api.dependency.DependencyLoaderAccess;
import com.tjxjnoobie.api.dependency.injection.helpers.DependencyInjectorHelper;
import com.tjxjnoobie.api.dependency.maps.DependencyMap;
import com.tjxjnoobie.api.dependency.metadata.interfaces.IDependencyMetaData;
import com.tjxjnoobie.api.dependency.annotations.fixtures.arrayonly.ArrayOnlyDelegatingService;
import com.tjxjnoobie.api.dependency.annotations.fixtures.mixed.MixedDeclarationDelegatingService;
import com.tjxjnoobie.api.interfaces.IRedis;
import com.tjxjnoobie.api.interfaces.IUtils;
import com.tjxjnoobie.api.machine.data.interfaces.ILocalServerMetaData;
import com.tjxjnoobie.api.platform.velocity.startup.interfaces.IVelocityMain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DelegatesToInterfaceIntegrationTest {
    private static final String ARRAY_ONLY_FIXTURE_PACKAGE = "com.tjxjnoobie.api.dependency.annotations.fixtures.arrayonly";
    private static final String MIXED_DECLARATION_FIXTURE_PACKAGE = "com.tjxjnoobie.api.dependency.annotations.fixtures.mixed";

    @BeforeEach
    void resetState() {
        DependencyMap.getDependencyMap().clear();
        ArrayOnlyDelegatingService.reset();
        MixedDeclarationDelegatingService.reset();
    }

    @AfterEach
    void clearMap() {
        DependencyMap.getDependencyMap().clear();
    }

    @Test
    void arrayOnlyAnnotationRegistersInterfacesInDeclaredOrder() {
        TestableDependencyInjectorHelper helper = new TestableDependencyInjectorHelper();

        helper.scanPackage(ARRAY_ONLY_FIXTURE_PACKAGE, getClass().getClassLoader());
        helper.registerDependenciesViaAnnotation();

        IRedis redis = DependencyLoaderAccess.findInstance(IRedis.class);
        IUtils utils = DependencyLoaderAccess.findInstance(IUtils.class);
        ILocalServerMetaData localServerMetaData = DependencyLoaderAccess.findInstance(ILocalServerMetaData.class);
        IDependencyMetaData<?, ?> metaData = DependencyLoaderAccess.findMetaData(IRedis.class);

        assertNotNull(redis);
        assertNotNull(utils);
        assertNotNull(localServerMetaData);
        assertNotNull(metaData);
        assertSame(redis, utils);
        assertSame(redis, localServerMetaData);
        assertEquals(IRedis.class, metaData.getPrimaryInterfaceType());
        assertEquals(3, DependencyMap.getDependencyMap().getDependencyMapSize());

        redis.connectToRedis();
        utils.createServerID();
        utils.createGameID();

        assertEquals(1, ArrayOnlyDelegatingService.getConnectCalls());
        assertEquals("array-only-server", utils.getServerID());
        assertEquals("array-only-server", localServerMetaData.getLocalServerID());
        assertEquals("array-only-game", utils.getGameID());
    }

    @Test
    void mixedAnnotationDeduplicatesAndSkipsInterfacesTheConcreteDoesNotImplement() {
        TestableDependencyInjectorHelper helper = new TestableDependencyInjectorHelper();

        helper.scanPackage(MIXED_DECLARATION_FIXTURE_PACKAGE, getClass().getClassLoader());
        helper.registerDependenciesViaAnnotation();

        assertTrue(DependencyLoaderAccess.isInstanceRegistered(IRedis.class));
        assertTrue(DependencyLoaderAccess.isInstanceRegistered(IUtils.class));
        assertFalse(DependencyLoaderAccess.isInstanceRegistered(IVelocityMain.class));
        assertEquals(2, DependencyMap.getDependencyMap().getDependencyMapSize());

        IRedis redis = DependencyLoaderAccess.findInstance(IRedis.class);
        IUtils utils = DependencyLoaderAccess.findInstance(IUtils.class);

        assertNotNull(redis);
        assertNotNull(utils);
        assertSame(redis, utils);

        redis.connectToRedis();
        utils.setConfigValue("style", "mixed");

        assertEquals(1, MixedDeclarationDelegatingService.getConnectCalls());
        assertEquals("mixed", utils.getConfigValues().get("style"));
    }

    private static class TestableDependencyInjectorHelper extends DependencyInjectorHelper<
            com.tjxjnoobie.api.dependency.IDependencyInjectableInterface,
            com.tjxjnoobie.api.dependency.IDependencyInjectableConcrete> {
    }
}
