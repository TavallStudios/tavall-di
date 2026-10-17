package org.tavall.dependency.injection.helpers;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tavall.dependency.IDependencyFactory;
import org.tavall.dependency.injection.helpers.providedfixtures.ProvidedDependencyFixtures.ExternalClient;
import org.tavall.dependency.injection.helpers.providedfixtures.ProvidedDependencyFixtures.ThirdPartyClient;
import org.tavall.dependency.injection.helpers.providedfixtures.ProvidedDependencyFixtures.ThirdPartyClientFactory;
import org.tavall.dependency.maps.DependencyMap;
import org.tavall.dependency.metadata.interfaces.IDependencyMetaData;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProvidedDependencyIntegrationTest {
    private static final String FIXTURE_PACKAGE =
            "org.tavall.dependency.injection.helpers.providedfixtures";

    @BeforeEach
    void resetState() {
        DependencyMap.getDependencyMap().clear();
        ThirdPartyClientFactory.reset();
    }

    @AfterEach
    void clearState() {
        DependencyMap.getDependencyMap().clear();
    }

    @Test
    void scannerCreatesProvidedDependencyAndMapsEveryTokenToOneMetadataInstance() {
        DependencyInjectorHelper<Object, Object> helper = new DependencyInjectorHelper<>();
        helper.setBasePackage(FIXTURE_PACKAGE);

        helper.setupDISystem();

        ExternalClient client = DependencyMap.getDependencyMap().getInstance(ExternalClient.class);
        ThirdPartyClient concrete = DependencyMap.getDependencyMap().getInstance(ThirdPartyClient.class);
        IDependencyMetaData<?, ?> clientMetadata =
                DependencyMap.getDependencyMap().findMetaData(ExternalClient.class);
        IDependencyMetaData<?, ?> concreteMetadata =
                DependencyMap.getDependencyMap().findMetaData(ThirdPartyClient.class);

        assertSame(client, concrete);
        assertSame(clientMetadata, concreteMetadata);
        assertEquals("fixture-endpoint", client.endpoint());
        assertEquals(1, ThirdPartyClientFactory.creationCount());
        assertEquals("fixture-endpoint", DependencyMap.getDependencyMap()
                .getInstance(org.tavall.dependency.injection.helpers.providedfixtures.ProvidedDependencyFixtures.ClientConsumer.class).endpoint());
    }

    @Test
    void setupRepairsMissingAliasesWithoutReconstructingFactoriesProductsOrOrdinaryInputs() {
        var helper = new DependencyInjectorHelper<Object, Object>();
        helper.setBasePackage(FIXTURE_PACKAGE);
        helper.setupDISystem();
        var map = DependencyMap.getDependencyMap();
        ExternalClient original = map.getInstance(ExternalClient.class);
        var originalMetadata = map.findMetaData(ExternalClient.class);
        var configurationType = org.tavall.dependency.injection.helpers.providedfixtures.ProvidedDependencyFixtures.TestConfiguration.class;
        Object configuration = map.getInstance(configurationType);
        map.removeDependency(ExternalClient.class);
        helper.setupDISystem();
        assertSame(original, map.getInstance(ExternalClient.class));
        assertSame(originalMetadata, map.findMetaData(ExternalClient.class));
        map.removeDependency(configurationType);
        helper.setupDISystem();
        assertSame(configuration, map.getInstance(configurationType));
        assertSame(original, map.getInstance(ExternalClient.class));
        assertEquals(1, ThirdPartyClientFactory.creationCount());
    }

    @Test
    void replacementThroughOneProvidedTokenRemainsVisibleThroughAliases() {
        DependencyInjectorHelper<Object, Object> helper = new DependencyInjectorHelper<>();
        helper.setBasePackage(FIXTURE_PACKAGE);
        helper.setupDISystem();

        ThirdPartyClient replacement = new ThirdPartyClient("replacement-endpoint");
        DependencyMap.getDependencyMap().replaceInstance(ExternalClient.class, () -> replacement);

        assertSame(replacement, DependencyMap.getDependencyMap().getInstance(ExternalClient.class));
        assertSame(replacement, DependencyMap.getDependencyMap().getInstance(ThirdPartyClient.class));
    }

    @Test
    void scopedFactoriesAndConsumersUseTheirOwnGenerationWithoutTheGlobalMap() {
        DependencyMap first = new DependencyMap();
        DependencyMap second = new DependencyMap();
        first.registerInstance(org.tavall.dependency.injection.helpers.providedfixtures.ProvidedDependencyFixtures.TestConfiguration.class, () -> "first");
        second.registerInstance(org.tavall.dependency.injection.helpers.providedfixtures.ProvidedDependencyFixtures.TestConfiguration.class, () -> "second");
        ScopedFactory a = new ScopedFactory();
        ScopedFactory b = new ScopedFactory();
        try {
            first.registerFactory(ExternalClient.class, a, ThirdPartyClient.class);
            second.registerFactory(ExternalClient.class, b, ThirdPartyClient.class);
            ScopedConsumer one = first.registerInstance(ScopedConsumer.class, new ScopedConsumer());
            ScopedConsumer two = second.registerInstance(ScopedConsumer.class, new ScopedConsumer());
            assertEquals("first", one.endpoint());
            assertEquals("second", two.endpoint());
            first.clear();
            assertThrows(IllegalStateException.class, a::create);
            assertThrows(IllegalStateException.class, one::endpoint);
            assertEquals("second", two.endpoint());
        } finally { first.clear(); second.clear(); }
    }

    static final class ScopedFactory extends org.tavall.dependency.GenerationOwnedDependencyAccess
            implements IDependencyFactory<ThirdPartyClient>, org.tavall.dependency.DependencyAccess<org.tavall.dependency.injection.helpers.providedfixtures.ProvidedDependencyFixtures.TestConfiguration> {
        @Override public ThirdPartyClient create() { return new ThirdPartyClient(getConfiguration().endpoint()); }
        private org.tavall.dependency.injection.helpers.providedfixtures.ProvidedDependencyFixtures.TestConfiguration getConfiguration() { return getInstance(); }
    }

    static final class ScopedConsumer extends org.tavall.dependency.GenerationOwnedDependencyAccess
            implements org.tavall.dependency.DependencyAccess<ExternalClient> {
        String endpoint() { return getClient().endpoint(); }
        private ExternalClient getClient() { return getInstance(); }
    }

    @Test
    void failedProductLifecycleRestoresAliasesAndAllowsAFreshFactoryRetry() {
        DependencyMap map = DependencyMap.getDependencyMap();
        ExternalClient original = map.registerInstance(ExternalClient.class, new ThirdPartyClient("original"));
        IDependencyMetaData<?, ?> originalMetadata = map.findMetaData(ExternalClient.class);
        assertThrows(IllegalStateException.class, () -> map.registerFactory(ExternalClient.class,
                new LifecycleFactory(true), LifecycleClient.class));
        assertSame(original, map.getInstance(ExternalClient.class));
        assertSame(originalMetadata, map.findMetaData(ExternalClient.class));
        assertEquals(null, map.findMetaData(LifecycleClient.class));
        assertEquals(null, map.findMetaData(LifecycleFactory.class));
        ExternalClient recovered = map.registerFactory(ExternalClient.class, new LifecycleFactory(false), LifecycleClient.class);
        assertEquals("ready", recovered.endpoint());
        assertSame(recovered, map.getInstance(LifecycleClient.class));
    }

    static final class LifecycleFactory implements IDependencyFactory<LifecycleClient> {
        private final boolean fail;
        LifecycleFactory(boolean fail) { this.fail = fail; }
        @Override public LifecycleClient create() { return new LifecycleClient(fail); }
    }

    static final class LifecycleClient implements ExternalClient {
        private final boolean fail;
        LifecycleClient(boolean fail) { this.fail = fail; }
        @org.tavall.dependency.annotations.PostConstruct public void initialize() {
            if (fail) throw new IllegalStateException("initialization failed");
        }
        @Override public String endpoint() { return "ready"; }
    }

    @Test
    void manualLambdaFactoriesCanBeReplacedAtTheSameCallSite() {
        var map = DependencyMap.getDependencyMap();
        IDependencyFactory<ThirdPartyClient> first = endpointFactory("first");
        IDependencyFactory<ThirdPartyClient> second = endpointFactory("second");
        assertSame(first.getClass(), second.getClass());
        map.registerFactory(ExternalClient.class, first, ThirdPartyClient.class);
        ExternalClient replacement = map.registerFactory(ExternalClient.class, second, ThirdPartyClient.class);
        assertEquals("second", replacement.endpoint());
        assertSame(replacement, map.getInstance(ThirdPartyClient.class));
        assertEquals(null, map.findMetaData(first.getClass()));
    }

    @Test
    void pureNamedAndAnonymousFactoriesRemainConstructionInstructions() {
        var map = DependencyMap.getDependencyMap();
        map.registerFactory(ExternalClient.class, new PureFactory("one"), ThirdPartyClient.class);
        map.registerFactory(ExternalClient.class, new PureFactory("two"), ThirdPartyClient.class);
        assertEquals("two", map.getInstance(ExternalClient.class).endpoint());
        map.registerFactory(ExternalClient.class, anonymousFactory("three"), ThirdPartyClient.class);
        map.registerFactory(ExternalClient.class, anonymousFactory("four"), ThirdPartyClient.class);
        assertEquals("four", map.getInstance(ExternalClient.class).endpoint());
        assertEquals(null, map.findMetaData(PureFactory.class));
    }

    @Test
    void manualFactoryInjectionAndLifecycleAreStillOwnedByTheMap() {
        var map = DependencyMap.getDependencyMap();
        map.registerInstance(org.tavall.dependency.injection.helpers.providedfixtures.ProvidedDependencyFixtures.TestConfiguration.class, () -> "manual");
        var factory = new InjectedFactory();
        assertEquals("manual", map.registerFactory(ExternalClient.class, factory).endpoint());
        assertSame(factory, map.getInstance(InjectedFactory.class));
        DependencyMap scoped = new DependencyMap();
        try {
            assertThrows(IllegalArgumentException.class, () -> scoped.registerFactory(ExternalClient.class, new InjectedFactory()));
            assertEquals(0, scoped.getDependencyMapSize(), "scoped field injection cannot silently use the global map");
        } finally { scoped.clear(); }
    }

    static final class PureFactory implements IDependencyFactory<ThirdPartyClient> {
        private final String endpoint;
        PureFactory(String endpoint) { this.endpoint = endpoint; }
        @Override public ThirdPartyClient create() { return new ThirdPartyClient(endpoint); }
    }

    static final class InjectedFactory implements IDependencyFactory<ThirdPartyClient> {
        @org.tavall.dependency.annotations.Inject
        private org.tavall.dependency.injection.helpers.providedfixtures.ProvidedDependencyFixtures.TestConfiguration configuration;
        private boolean initialized;
        @org.tavall.dependency.annotations.PostConstruct public void initialize() {
            if (configuration == null) throw new IllegalStateException("missing configuration");
            initialized = true;
        }
        @Override public ThirdPartyClient create() {
            if (!initialized) throw new IllegalStateException("factory not initialized");
            return new ThirdPartyClient(configuration.endpoint());
        }
    }

    private static IDependencyFactory<ThirdPartyClient> anonymousFactory(String endpoint) {
        return new IDependencyFactory<>() {
            @Override public ThirdPartyClient create() { return new ThirdPartyClient(endpoint); }
        };
    }

    private static IDependencyFactory<ThirdPartyClient> endpointFactory(String endpoint) {
        return () -> new ThirdPartyClient(endpoint);
    }

    @Test
    void manualFactoryRegistrationRejectsAnAliasBeforePublishingMetadata() {
        IDependencyFactory<ThirdPartyClient> factory =
                () -> new ThirdPartyClient("manual-endpoint");

        assertThrows(
                IllegalArgumentException.class,
                () -> DependencyMap.getDependencyMap().registerFactory(
                        ThirdPartyClient.class,
                        factory,
                        Runnable.class));

        assertEquals(0, DependencyMap.getDependencyMap().getDependencyMapSize());
    }
}
