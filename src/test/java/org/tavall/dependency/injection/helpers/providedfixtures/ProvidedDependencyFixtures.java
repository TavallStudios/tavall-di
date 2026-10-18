package org.tavall.dependency.injection.helpers.providedfixtures;

import org.tavall.dependency.IDependencyFactory;
import org.tavall.dependency.annotations.DelegatesTo;
import org.tavall.dependency.annotations.ProvidedDependency;
import org.tavall.dependency.annotations.Inject;
import org.tavall.dependency.annotations.PostConstruct;
import org.tavall.dependency.DependencyAccess;

import java.util.concurrent.atomic.AtomicInteger;

public final class ProvidedDependencyFixtures {
    private ProvidedDependencyFixtures() {
    }

    public interface TestConfiguration {
        String endpoint();
    }

    @DelegatesTo(TestConfiguration.class)
    public static final class DefaultTestConfiguration implements TestConfiguration {
        private boolean initialized;
        @PostConstruct public void initialize() { initialized = true; }
        @Override public String endpoint() {
            if (!initialized) throw new IllegalStateException("Configuration must initialize before factory construction");
            return "fixture-endpoint";
        }
    }

    public interface ClientConsumer { String endpoint(); }

    @DelegatesTo(ClientConsumer.class)
    public static final class InjectedClientConsumer implements ClientConsumer {
        @Inject private ExternalClient client;
        @Override public String endpoint() { return client.endpoint(); }
    }

    public interface ExternalClient {
        String endpoint();
    }

    public static final class ThirdPartyClient implements ExternalClient {
        private final String endpoint;

        public ThirdPartyClient(String endpoint) {
            this.endpoint = endpoint;
        }

        @Override
        public String endpoint() {
            return endpoint;
        }
    }

    @ProvidedDependency({ExternalClient.class, ThirdPartyClient.class})
    public static final class ThirdPartyClientFactory implements IDependencyFactory<ThirdPartyClient>, DependencyAccess<TestConfiguration> {
        private static final AtomicInteger CREATIONS = new AtomicInteger();

        @Override
        public ThirdPartyClient create() {
            CREATIONS.incrementAndGet();
            TestConfiguration configuration = getTestConfiguration();
            return new ThirdPartyClient(configuration.endpoint());
        }

        private TestConfiguration getTestConfiguration() {
            return getInstance();
        }

        public static int creationCount() {
            return CREATIONS.get();
        }

        public static void reset() {
            CREATIONS.set(0);
        }
    }
}
