package com.tjxjnoobie.api.dependency.injection.helpers;

import com.tjxjnoobie.api.dependency.DependencyLoaderAccess;
import com.tjxjnoobie.api.dependency.injection.helpers.fixtures.DelegatingRedisService;
import com.tjxjnoobie.api.dependency.injection.helpers.fixtures.DelegatingUtilsService;
import com.tjxjnoobie.api.dependency.injection.helpers.fixtures.DelegatingVelocityMainService;
import com.tjxjnoobie.api.dependency.injection.helpers.multiplefixtures.DelegatingMultiInterfaceService;
import com.tjxjnoobie.api.dependency.maps.DependencyMap;
import com.tjxjnoobie.api.interfaces.IRedis;
import com.tjxjnoobie.api.interfaces.IUtils;
import com.tjxjnoobie.api.machine.data.interfaces.ILocalServerMetaData;
import com.tjxjnoobie.api.platform.velocity.startup.interfaces.IVelocityMain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependencyInjectorHelperIntegrationTest {
    private static final String FIXTURE_PACKAGE = "com.tjxjnoobie.api.dependency.injection.helpers.fixtures";
    private static final String MULTI_FIXTURE_PACKAGE = "com.tjxjnoobie.api.dependency.injection.helpers.multiplefixtures";

    @BeforeEach
    void resetState() {
        DependencyMap.getDependencyMap().clear();
        DelegatingRedisService.reset();
        DelegatingVelocityMainService.reset();
        DelegatingMultiInterfaceService.reset();
    }

    @AfterEach
    void clearMap() {
        DependencyMap.getDependencyMap().clear();
    }

    @Test
    void setupDISystemRegistersDependenciesImmediately() throws Throwable {
        TestableDependencyInjectorHelper helper = new TestableDependencyInjectorHelper();
        helper.BASE_PACKAGE = FIXTURE_PACKAGE;
        helper.setupDISystem();

        assertFixtureBindingsRegistered();
    }

    @Test
    void setupDISystemIsIdempotentForTheSamePackageAndClassLoader() throws Throwable {
        TestableDependencyInjectorHelper helper = new TestableDependencyInjectorHelper();
        helper.BASE_PACKAGE = FIXTURE_PACKAGE;

        helper.setupDISystem();
        IUtils first = DependencyLoaderAccess.requireInstance(IUtils.class);

        helper.setupDISystem();
        IUtils second = DependencyLoaderAccess.requireInstance(IUtils.class);

        assertSame(first, second);
    }

    @Test
    void reloadDISystemRefreshesBindingsForTheSamePackageAndClassLoader() throws Throwable {
        TestableDependencyInjectorHelper helper = new TestableDependencyInjectorHelper();
        helper.BASE_PACKAGE = FIXTURE_PACKAGE;
        helper.setupDISystem();

        IUtils original = DependencyLoaderAccess.requireInstance(IUtils.class);
        IUtils replaced = DependencyLoaderAccess.replaceInstance(IUtils.class, DelegatingUtilsService::new);

        helper.reloadDISystem();

        IUtils reloaded = DependencyLoaderAccess.requireInstance(IUtils.class);
        assertNotSame(original, replaced);
        assertNotSame(replaced, reloaded);
    }

    @Test
    void setupDISystemOverloadsProduceTheSameFixtureBindings() throws Throwable {
        runBootstrapAndAssertFixtureBindings(helper -> helper.setupDISystem());
        runBootstrapAndAssertFixtureBindings(helper -> helper.setupDISystem(getClass().getClassLoader()));
        runBootstrapAndAssertFixtureBindings(helper -> helper.setupDISystem(DependencyInjectorHelperIntegrationTest.class));
        runBootstrapAndAssertFixtureBindings(helper -> helper.setupDISystem(new TestEntryPoint()));
    }

    @Test
    void setupDISystemWithExplicitClassLoaderStillDrivesReflectiveScanning() throws Throwable {
        TestableDependencyInjectorHelper helper = new TestableDependencyInjectorHelper();
        helper.BASE_PACKAGE = MULTI_FIXTURE_PACKAGE;

        helper.setupDISystem(getClass().getClassLoader());

        assertTrue(DependencyLoaderAccess.isInstanceRegistered(IRedis.class));
        assertTrue(DependencyLoaderAccess.isInstanceRegistered(IUtils.class));
        assertTrue(DependencyLoaderAccess.isInstanceRegistered(ILocalServerMetaData.class));

        IRedis redis = DependencyLoaderAccess.findInstance(IRedis.class);
        IUtils utils = DependencyLoaderAccess.findInstance(IUtils.class);
        ILocalServerMetaData localServerMetaData = DependencyLoaderAccess.findInstance(ILocalServerMetaData.class);

        assertNotNull(redis);
        assertNotNull(utils);
        assertNotNull(localServerMetaData);
        assertSame(redis, utils);
        assertSame(redis, localServerMetaData);
    }

    @Test
    void scansMultiInterfaceFixtureAndRegistersEveryDeclaredInterfaceAgainstTheSameSingleton() throws Throwable {
        TestableDependencyInjectorHelper helper = new TestableDependencyInjectorHelper();

        helper.BASE_PACKAGE = MULTI_FIXTURE_PACKAGE;
        helper.setupDISystem();

        assertTrue(DependencyLoaderAccess.isInstanceRegistered(IRedis.class));
        assertTrue(DependencyLoaderAccess.isInstanceRegistered(IUtils.class));
        assertTrue(DependencyLoaderAccess.isInstanceRegistered(ILocalServerMetaData.class));

        IRedis redis = DependencyLoaderAccess.findInstance(IRedis.class);
        IUtils utils = DependencyLoaderAccess.findInstance(IUtils.class);
        ILocalServerMetaData localServerMetaData = DependencyLoaderAccess.findInstance(ILocalServerMetaData.class);

        assertNotNull(redis);
        assertNotNull(utils);
        assertNotNull(localServerMetaData);
        assertSame(redis, utils);
        assertSame(redis, localServerMetaData);

        redis.connectToRedis();
        utils.createServerID();
        utils.createGameID();
        utils.setConfigValue("surface", "annotation-multi");
        localServerMetaData.setServerID("delegated-local-server");

        assertEquals(1, DelegatingMultiInterfaceService.getConnectCalls());
        assertEquals("delegated-local-server", utils.getServerID());
        assertEquals("delegated-local-server", localServerMetaData.getLocalServerID());
        assertEquals("annotated-multi-game", localServerMetaData.getGameID());
        assertEquals("annotation-multi", utils.getConfigValues().get("surface"));
        assertEquals("annotated-multi-4", utils.generateRandomID(4));
    }

    private void runBootstrapAndAssertFixtureBindings(ThrowingHelperBootstrap bootstrap) throws Throwable {
        DependencyMap.getDependencyMap().clear();
        DelegatingRedisService.reset();
        DelegatingVelocityMainService.reset();
        DelegatingMultiInterfaceService.reset();

        TestableDependencyInjectorHelper helper = new TestableDependencyInjectorHelper();
        helper.BASE_PACKAGE = FIXTURE_PACKAGE;

        bootstrap.run(helper);
        assertFixtureBindingsRegistered();
    }

    private void assertFixtureBindingsRegistered() throws Throwable {
        assertTrue(DependencyLoaderAccess.isInstanceRegistered(IRedis.class));
        assertTrue(DependencyLoaderAccess.isInstanceRegistered(IUtils.class));
        assertTrue(DependencyLoaderAccess.isInstanceRegistered(IVelocityMain.class));

        IRedis redis = DependencyLoaderAccess.findInstance(IRedis.class);
        IUtils utils = DependencyLoaderAccess.findInstance(IUtils.class);
        IVelocityMain velocityMain = DependencyLoaderAccess.findInstance(IVelocityMain.class);

        assertNotNull(redis);
        assertNotNull(utils);
        assertNotNull(velocityMain);

        redis.connectToRedis();
        redis.disconnectFromRedis();
        utils.createServerID();
        utils.createGameID();
        utils.setConfigValue("mode", "integration");
        velocityMain.onProxyInitialization(null);
        velocityMain.registerCommand("ping", null, null, "p");

        assertEquals(1, DelegatingRedisService.getConnectCalls());
        assertEquals(1, DelegatingRedisService.getDisconnectCalls());
        assertEquals("server-generated", utils.getServerID());
        assertEquals("game-generated", utils.getGameID());
        assertEquals("integration", utils.getConfigValues().get("mode"));
        assertEquals(1, DelegatingVelocityMainService.getInitializationCalls());
        assertEquals("ping", DelegatingVelocityMainService.getLastCommand());
        assertEquals(1, DelegatingVelocityMainService.getLastAliasCount());
    }

    private static class TestableDependencyInjectorHelper extends DependencyInjectorHelper<
            com.tjxjnoobie.api.dependency.IDependencyInjectableInterface,
            com.tjxjnoobie.api.dependency.IDependencyInjectableConcrete> {
    }

    private static class TestEntryPoint {
    }

    @FunctionalInterface
    private interface ThrowingHelperBootstrap {
        void run(TestableDependencyInjectorHelper helper) throws Throwable;
    }
}
