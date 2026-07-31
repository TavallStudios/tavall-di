package org.tavall.dependency.annotations;

import org.tavall.dependency.DependencyLoaderAccess;
import org.tavall.dependency.injection.helpers.DependencyInjectorHelper;
import org.tavall.dependency.maps.DependencyMap;
import org.tavall.dependency.metadata.interfaces.IDependencyMetaData;
import org.tavall.dependency.annotations.fixtures.arrayonly.ArrayOnlyDelegatingService;
import org.tavall.dependency.annotations.fixtures.mixed.MixedDeclarationDelegatingService;
import org.tavall.dependency.fixtures.contracts.interfaces.IRedis;
import org.tavall.dependency.fixtures.contracts.interfaces.IUtils;
import org.tavall.dependency.fixtures.contracts.machine.data.interfaces.ILocalServerMetaData;
import org.tavall.dependency.fixtures.contracts.platform.velocity.startup.interfaces.IVelocityMain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DelegatesToTest {
    private static final String ARRAY_ONLY_FIXTURE_PACKAGE = "org.tavall.dependency.annotations.fixtures.arrayonly";
    private static final String MIXED_DECLARATION_FIXTURE_PACKAGE = "org.tavall.dependency.annotations.fixtures.mixed";

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
    void arrayAnnotationRegistersDependencyTokensInDeclaredOrder() {
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
    void mixedAnnotationDeduplicatesAndSkipsUnassignableDependencyTokens() {
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
            org.tavall.dependency.IDependencyInjectableInterface,
            org.tavall.dependency.IDependencyInjectableConcrete> {
    }
}
