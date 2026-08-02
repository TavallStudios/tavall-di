package org.tavall.dependency.annotations;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tavall.dependency.annotations.fixtures.arrayonly.ArrayOnlyDelegatingService;
import org.tavall.dependency.annotations.fixtures.concreteonly.ConcreteOnlyDelegatingService;
import org.tavall.dependency.annotations.fixtures.mixed.MixedDeclarationDelegatingService;
import org.tavall.dependency.fixtures.contracts.interfaces.IRedis;
import org.tavall.dependency.fixtures.contracts.interfaces.IUtils;
import org.tavall.dependency.fixtures.contracts.machine.data.interfaces.ILocalServerMetaData;
import org.tavall.dependency.injection.helpers.DependencyInjectorHelper;
import org.tavall.dependency.maps.DependencyMap;
import org.tavall.dependency.metadata.interfaces.IDependencyMetaData;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DelegatesToTest {
    private static final String ARRAY_ONLY_FIXTURE_PACKAGE =
            "org.tavall.dependency.annotations.fixtures.arrayonly";
    private static final String CONCRETE_ONLY_FIXTURE_PACKAGE =
            "org.tavall.dependency.annotations.fixtures.concreteonly";
    private static final String MIXED_DECLARATION_FIXTURE_PACKAGE =
            "org.tavall.dependency.annotations.fixtures.mixed";

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
    void registersConcreteAndMultipleAdditionalTokensToOneMetadataInstance() {
        TestableDependencyInjectorHelper helper = new TestableDependencyInjectorHelper();
        helper.scanPackage(ARRAY_ONLY_FIXTURE_PACKAGE, getClass().getClassLoader());
        helper.registerDependenciesViaAnnotation();

        ArrayOnlyDelegatingService concrete =
                DependencyMap.getDependencyMap().getInstance(ArrayOnlyDelegatingService.class);
        IRedis redis = DependencyMap.getDependencyMap().getInstance(IRedis.class);
        IUtils utils = DependencyMap.getDependencyMap().getInstance(IUtils.class);
        ILocalServerMetaData localServerMetaData =
                DependencyMap.getDependencyMap().getInstance(ILocalServerMetaData.class);

        assertSame(concrete, redis);
        assertSame(concrete, utils);
        assertSame(concrete, localServerMetaData);

        IDependencyMetaData<?, ?> metadata =
                DependencyMap.getDependencyMap().findMetaData(ArrayOnlyDelegatingService.class);
        assertNotNull(metadata);
        assertSame(metadata, DependencyMap.getDependencyMap().findMetaData(IRedis.class));
        assertSame(metadata, DependencyMap.getDependencyMap().findMetaData(IUtils.class));
        assertSame(metadata, DependencyMap.getDependencyMap().findMetaData(ILocalServerMetaData.class));
        assertEquals(ArrayOnlyDelegatingService.class, metadata.getPrimaryInterfaceType());
        assertEquals(4, DependencyMap.getDependencyMap().getDependencyMapSize());

        redis.connectToRedis();
        utils.createServerID();
        utils.createGameID();
        assertEquals(1, ArrayOnlyDelegatingService.getConnectCalls());
        assertEquals("array-only-server", localServerMetaData.getLocalServerID());
        assertEquals("array-only-game", utils.getGameID());
    }

    @Test
    void annotationWithoutValuesRegistersTheConcreteToken() {
        TestableDependencyInjectorHelper helper = new TestableDependencyInjectorHelper();
        helper.scanPackage(CONCRETE_ONLY_FIXTURE_PACKAGE, getClass().getClassLoader());
        helper.registerDependenciesViaAnnotation();

        ConcreteOnlyDelegatingService service = DependencyMap.getDependencyMap()
                .getInstance(ConcreteOnlyDelegatingService.class);

        assertNotNull(service);
        assertEquals(1, DependencyMap.getDependencyMap().getDependencyMapSize());
    }

    @Test
    void rejectsAnAdditionalTokenThatTheConcreteCannotSatisfy() {
        TestableDependencyInjectorHelper helper = new TestableDependencyInjectorHelper();
        helper.scanPackage(MIXED_DECLARATION_FIXTURE_PACKAGE, getClass().getClassLoader());

        IllegalArgumentException failure = assertThrows(
                IllegalArgumentException.class,
                helper::registerDependenciesViaAnnotation);

        assertTrue(failure.getMessage().contains("is not assignable from"));
        assertTrue(DependencyMap.getDependencyMap().isDependencyMapEmpty());
    }

    private static final class TestableDependencyInjectorHelper
            extends DependencyInjectorHelper<Object, Object> {
    }
}
