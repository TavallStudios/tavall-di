package org.tavall.dependency.access;

import org.junit.jupiter.api.Test;
import org.tavall.dependency.injection.helpers.DependencyInjectorHelper;
import org.tavall.dependency.maps.DependencyMap;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ProvidedDependencyGeneratedAccessTest {
    @Test
    void expandedFactoryAndConsumerCompileAndBootstrapThroughTypedGetters() throws Exception {
        Map<String, String> sources = new LinkedHashMap<>();
        sources.put("provided.generated.Configuration", """
                package provided.generated;
                @org.tavall.dependency.annotations.DelegatesTo
                public final class Configuration {
                    private boolean ready;
                    @org.tavall.dependency.annotations.PostConstruct public void initialize() { ready = true; }
                    public String endpoint() { if (!ready) throw new IllegalStateException("uninitialized configuration"); return "https://example.test"; }
                }
                """);
        sources.put("provided.generated.Timeout", """
                package provided.generated;
                @org.tavall.dependency.annotations.DelegatesTo
                public final class Timeout { public int seconds() { return 4; } }
                """);
        sources.put("provided.generated.ClientFactory", """
                package provided.generated;
                import org.tavall.dependency.DependencyAccess;
                @org.tavall.dependency.annotations.ProvidedDependency(java.net.http.HttpClient.class)
                public final class ClientFactory implements org.tavall.dependency.IDependencyFactory<java.net.http.HttpClient>, DependencyAccess<Configuration, Timeout> {
                    public java.net.http.HttpClient create() {
                        getConfiguration().endpoint();
                        return java.net.http.HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(getTimeout().seconds())).build();
                    }
                    private Configuration getConfiguration() { return getInstance().configuration(); }
                    private Timeout getTimeout() { return getInstance().timeout(); }
                }
                """);
        sources.put("provided.generated.Consumer", """
                package provided.generated;
                import org.tavall.dependency.DependencyAccess;
                @org.tavall.dependency.annotations.DelegatesTo
                public final class Consumer implements DependencyAccess<java.net.http.HttpClient, Configuration> {
                    public long timeoutSeconds() { return getClient().connectTimeout().orElseThrow().toSeconds(); }
                    private java.net.http.HttpClient getClient() { return getInstance().httpClient(); }
                    private Configuration getConfiguration() { return getInstance().configuration(); }
                }
                """);
        var lowered = new DependencyAccessSourceLowerer().lowerSources(sources);
        var output = DependencyAccessTestCompiler.compileSources(lowered);
        var map = DependencyMap.getDependencyMap();
        map.clear();
        try (var loader = DependencyAccessTestCompiler.createClassLoader(output)) {
            var helper = new DependencyInjectorHelper<Object, Object>();
            helper.setBasePackage("provided.generated");
            helper.setupDISystem(loader);
            Object consumer = map.getInstance(loader.loadClass("provided.generated.Consumer"));
            assertEquals(4L, consumer.getClass().getMethod("timeoutSeconds").invoke(consumer));
            assertSame(map.getInstance(java.net.http.HttpClient.class), map.getInstance(java.net.http.HttpClient.class));
        } finally { map.clear(); }
    }

    @Test
    void providerChainsInitializeInDependencyOrderAndRejectCycles() throws Exception {
        for (boolean cycle : new boolean[] { false, true }) {
            String namespace = cycle ? "provided.cycle" : "provided.chain";
            Map<String, String> sources = new LinkedHashMap<>();
            sources.put(namespace + ".FirstFactory", """
                    package %s;
                    @org.tavall.dependency.annotations.ProvidedDependency(String.class)
                    public final class FirstFactory implements org.tavall.dependency.IDependencyFactory<String>, org.tavall.dependency.DependencyAccess<Integer> {
                        public String create() { return "value-" + getNumber(); }
                        private Integer getNumber() { return getInstance(); }
                    }
                    """.formatted(namespace));
            sources.put(namespace + ".SecondFactory", "package " + namespace + ";" +
                    "@org.tavall.dependency.annotations.ProvidedDependency(Integer.class) " +
                    "public final class SecondFactory implements org.tavall.dependency.IDependencyFactory<Integer>" +
                    (cycle ? ", org.tavall.dependency.DependencyAccess<String>" : "") +
                    " { public Integer create() { return " + (cycle ? "getInstance().length()" : "7") + "; } }");
            var output = DependencyAccessTestCompiler.compileSources(sources);
            var map = DependencyMap.getDependencyMap();
            map.clear();
            try (var loader = DependencyAccessTestCompiler.createClassLoader(output)) {
                var helper = new DependencyInjectorHelper<Object, Object>();
                helper.setBasePackage(namespace);
                if (cycle) {
                    var failure = assertThrows(IllegalStateException.class, () -> helper.setupDISystem(loader));
                    assertTrue(failure.getMessage().contains("Cyclic provided dependency"));
                } else {
                    helper.setupDISystem(loader);
                    assertEquals("value-7", map.getInstance(String.class));
                }
            } finally { map.clear(); }
        }
    }

    @Test
    void scannerRejectsEveryNonConcreteProviderDeclaration() throws Exception {
        for (String declaration : new String[] {
                "public abstract class Invalid implements org.tavall.dependency.IDependencyFactory<String> {}",
                "public interface Invalid extends org.tavall.dependency.IDependencyFactory<String> {}",
                "public enum Invalid { VALUE }",
                "public @interface Invalid {}"}) {
            String packageName = "provided.invalid.case" + Math.abs(declaration.hashCode());
            var output = DependencyAccessTestCompiler.compileSources(Map.of(packageName + ".Invalid",
                    "package " + packageName + ";\n@org.tavall.dependency.annotations.ProvidedDependency(String.class)\n" + declaration));
            try (var loader = DependencyAccessTestCompiler.createClassLoader(output)) {
                var helper = new DependencyInjectorHelper<Object, Object>();
                helper.setBasePackage(packageName);
                var failure = assertThrows(IllegalArgumentException.class, () -> helper.setupDISystem(loader));
                assertTrue(failure.getMessage().contains("concrete IDependencyFactory"));
            } finally { DependencyMap.getDependencyMap().clear(); }
        }
    }
}
