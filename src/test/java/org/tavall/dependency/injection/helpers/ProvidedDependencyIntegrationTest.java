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
