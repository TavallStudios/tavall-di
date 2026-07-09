package org.tavall.dependency.injection.helpers;

import org.tavall.dependency.DependencyLoaderAccess;
import org.tavall.dependency.injection.helpers.fixtures.DelegatingRedisService;
import org.tavall.dependency.injection.helpers.multiplefixtures.DelegatingMultiInterfaceService;
import org.tavall.dependency.maps.DependencyMap;
import org.tavall.dependency.metadata.DependencyMetaData;
import org.tavall.dependency.metadata.fixtures.RealProjectMultiInterfaceService;
import org.tavall.dependency.metadata.wrappers.DependencyInstance;
import org.tavall.dependency.metadata.wrappers.DependencyInterface;
import org.tavall.dependency.fixtures.contracts.interfaces.IRedis;
import org.tavall.dependency.fixtures.contracts.interfaces.IUtils;
import org.tavall.dependency.fixtures.contracts.machine.data.interfaces.ILocalServerMetaData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class SetupAndRunDITest {
    private static final String FIXTURE_PACKAGE = "org.tavall.dependency.injection.helpers.fixtures";
    private static final String MULTI_FIXTURE_PACKAGE = "org.tavall.dependency.injection.helpers.multiplefixtures";

    @BeforeEach
    void resetState() {
        DependencyMap.getDependencyMap().clear();
        DelegatingRedisService.reset();
        DelegatingMultiInterfaceService.reset();
        RealProjectMultiInterfaceService.reset();
    }

    @AfterEach
    void clearMap() {
        DependencyMap.getDependencyMap().clear();
    }

    @Test
    void setupsDependencyInjectionAndRunsRealProjectInterfaces() throws Throwable {
        TestableDependencyInjectorHelper helper = new TestableDependencyInjectorHelper();
        helper.BASE_PACKAGE = FIXTURE_PACKAGE;

        helper.setupDISystem();

        IRedis redis = DependencyLoaderAccess.findInstance(IRedis.class);
        IUtils utils = DependencyLoaderAccess.findInstance(IUtils.class);

        assertNotNull(redis);
        assertNotNull(utils);

        redis.connectToRedis();
        utils.createServerID();

        assertEquals(1, DelegatingRedisService.getConnectCalls());
        assertEquals("server-generated", utils.getServerID());

        DependencyMap.getDependencyMap().clear();

        TestableDependencyInjectorHelper multiHelper = new TestableDependencyInjectorHelper();
        multiHelper.BASE_PACKAGE = MULTI_FIXTURE_PACKAGE;
        multiHelper.setupDISystem();

        IRedis annotationRedis = DependencyLoaderAccess.findInstance(IRedis.class);
        IUtils annotationUtils = DependencyLoaderAccess.findInstance(IUtils.class);
        ILocalServerMetaData annotationLocalMetaData = DependencyLoaderAccess.findInstance(ILocalServerMetaData.class);

        assertNotNull(annotationRedis);
        assertNotNull(annotationUtils);
        assertNotNull(annotationLocalMetaData);

        annotationRedis.connectToRedis();
        annotationUtils.createServerID();
        annotationUtils.createGameID();
        annotationLocalMetaData.setServerID("annotation-local-server");

        assertSame(annotationRedis, annotationUtils);
        assertSame(annotationRedis, annotationLocalMetaData);
        assertEquals(1, DelegatingMultiInterfaceService.getConnectCalls());
        assertEquals("annotation-local-server", annotationUtils.getServerID());
        assertEquals("annotation-local-server", annotationLocalMetaData.getLocalServerID());
        assertEquals("annotated-multi-game", annotationLocalMetaData.getGameID());

        MultiInterfaceOnEnableMetaData multiInterfaceMetaData = new MultiInterfaceOnEnableMetaData();
        multiInterfaceMetaData.populateMetaData(
                IRedis.class,
                RealProjectMultiInterfaceService.class,
                new DependencyInterface<>(IRedis.class),
                new DependencyInstance<>(RealProjectMultiInterfaceService.class));

        IRedis multiRedis = multiInterfaceMetaData.getDependencyInterface();
        IUtils multiUtils = multiInterfaceMetaData.findInstance(IUtils.class);
        ILocalServerMetaData localServerMetaData = multiInterfaceMetaData.findInstance(ILocalServerMetaData.class);

        multiRedis.connectToRedis();
        multiUtils.createServerID();
        multiUtils.createGameID();

        assertSame(multiInterfaceMetaData.getDependencyInstance(), multiRedis);
        assertSame(multiInterfaceMetaData.getDependencyInstance(), multiUtils);
        assertSame(multiInterfaceMetaData.getDependencyInstance(), localServerMetaData);
        assertEquals(1, RealProjectMultiInterfaceService.getRedisConnectCalls());
        assertEquals("multi-server", multiUtils.getServerID());
        assertEquals("multi-server", localServerMetaData.getLocalServerID());
        assertEquals("multi-game", multiUtils.getGameID());
    }

    private static class TestableDependencyInjectorHelper extends DependencyInjectorHelper<
            org.tavall.dependency.IDependencyInjectableInterface,
            org.tavall.dependency.IDependencyInjectableConcrete> {
    }

    private static class MultiInterfaceOnEnableMetaData extends DependencyMetaData<IRedis, RealProjectMultiInterfaceService> {
    }
}
