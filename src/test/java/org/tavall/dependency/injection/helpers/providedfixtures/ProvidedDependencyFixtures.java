package org.tavall.dependency.injection.helpers.providedfixtures;

import org.tavall.dependency.IDependencyFactory;
import org.tavall.dependency.annotations.DelegatesTo;
import org.tavall.dependency.annotations.ProvidedDependency;
import org.tavall.dependency.maps.interfaces.IDependencyMap;

import java.util.concurrent.atomic.AtomicInteger;

public final class ProvidedDependencyFixtures {
    private ProvidedDependencyFixtures() {
    }

    public interface TestConfiguration {
        String endpoint();
    }

    @DelegatesTo(TestConfiguration.class)
    public static final class DefaultTestConfiguration implements TestConfiguration {
        @Override
        public String endpoint() {
            return "fixture-endpoint";
        }
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
    public static final class ThirdPartyClientFactory implements IDependencyFactory<ThirdPartyClient> {
        private static final AtomicInteger CREATIONS = new AtomicInteger();

        @Override
        public ThirdPartyClient create(IDependencyMap dependencyMap) {
            CREATIONS.incrementAndGet();
            TestConfiguration configuration = dependencyMap.getInstance(TestConfiguration.class);
            return new ThirdPartyClient(configuration.endpoint());
        }

        public static int creationCount() {
            return CREATIONS.get();
        }

        public static void reset() {
            CREATIONS.set(0);
        }
    }
}
