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
    void managedSelfAccessDoesNotCreateAnInitializationCycle() throws Exception {
        var output = DependencyAccessTestCompiler.compileSources(Map.of("provided.self.Component", """
                package provided.self;
                @org.tavall.dependency.annotations.DelegatesTo
                public final class Component implements org.tavall.dependency.DependencyAccess<Component> {
                    public boolean initialized;
                    @org.tavall.dependency.annotations.PostConstruct public void initialize() {
                        if (getInstance() != this) throw new IllegalStateException("different self");
                        initialized = true;
                    }
                }
                """));
        var map = DependencyMap.getDependencyMap();
        map.clear();
        try (var loader = DependencyAccessTestCompiler.createClassLoader(output)) {
            var helper = new DependencyInjectorHelper<Object, Object>();
            helper.setBasePackage("provided.self");
            helper.setupDISystem(loader);
            Object component = map.getInstance(loader.loadClass("provided.self.Component"));
            assertTrue(component.getClass().getField("initialized").getBoolean(component));
        } finally { map.clear(); }
    }

    @Test
    void producedObjectInputsAreInitializedBeforeItsLifecycle() throws Exception {
        Map<String, String> sources = new LinkedHashMap<>();
        sources.put("provided.product.Configuration", """
                package provided.product;
                @org.tavall.dependency.annotations.DelegatesTo
                public final class Configuration {
                    public boolean ready;
                    @org.tavall.dependency.annotations.PostConstruct public void initialize() { ready = true; }
                }
                """);
        sources.put("provided.product.Product", """
                package provided.product;
                public final class Product implements org.tavall.dependency.DependencyAccess<Product> {
                    @org.tavall.dependency.annotations.Inject private Configuration configuration;
                    @org.tavall.dependency.annotations.Inject private String label;
                    public boolean ready;
                    @org.tavall.dependency.annotations.PostConstruct public void initialize() {
                        if (!configuration.ready || !"ready".equals(label) || getInstance() != this) throw new IllegalStateException("uninitialized product inputs");
                        ready = true;
                    }
                }
                """);
        sources.put("provided.product.ProductFactory", """
                package provided.product;
                @org.tavall.dependency.annotations.ProvidedDependency(Product.class)
                public final class ProductFactory implements org.tavall.dependency.IDependencyFactory<Product> {
                    public Product create() { return new Product(); }
                }
                """);
        sources.put("provided.product.LabelFactory", """
                package provided.product;
                @org.tavall.dependency.annotations.ProvidedDependency(String.class)
                public final class LabelFactory implements org.tavall.dependency.IDependencyFactory<String> {
                    public String create() { return "ready"; }
                }
                """);
        var output = DependencyAccessTestCompiler.compileSources(sources);
        var map = DependencyMap.getDependencyMap();
        map.clear();
        try (var loader = DependencyAccessTestCompiler.createClassLoader(output)) {
            var helper = new DependencyInjectorHelper<Object, Object>();
            helper.setBasePackage("provided.product");
            helper.setupDISystem(loader);
            Object product = map.getInstance(loader.loadClass("provided.product.Product"));
            assertTrue(product.getClass().getField("ready").getBoolean(product));
        } finally { map.clear(); }
    }

    @Test
    void failedBootstrapRollsBackCompletedProvidersAndCanRetryWithoutClearingTheMap() throws Exception {
        Map<String, String> sources = new LinkedHashMap<>();
        sources.put("provided.retry.ReadyFactory", """
                package provided.retry;
                @org.tavall.dependency.annotations.ProvidedDependency(String.class)
                public final class ReadyFactory implements org.tavall.dependency.IDependencyFactory<String> {
                    public static int creations;
                    public String create() { creations++; return "ready"; }
                }
                """);
        sources.put("provided.retry.TransientFactory", """
                package provided.retry;
                @org.tavall.dependency.annotations.ProvidedDependency(Integer.class)
                public final class TransientFactory implements org.tavall.dependency.IDependencyFactory<Integer>, org.tavall.dependency.DependencyAccess<String> {
                    public static boolean fail = true;
                    public Integer create() {
                        if (!"ready".equals(getInstance())) throw new IllegalStateException("missing prerequisite");
                        if (fail) throw new IllegalStateException("transient failure");
                        return 7;
                    }
                }
                """);
        var output = DependencyAccessTestCompiler.compileSources(sources);
        var map = DependencyMap.getDependencyMap();
        map.clear();
        Long unrelated = map.registerInstance(Long.class, 123L);
        try (var loader = DependencyAccessTestCompiler.createClassLoader(output)) {
            var helper = new DependencyInjectorHelper<Object, Object>();
            helper.setBasePackage("provided.retry");
            assertThrows(IllegalStateException.class, () -> helper.setupDISystem(loader));
            assertEquals(1, loader.loadClass("provided.retry.ReadyFactory").getField("creations").getInt(null));
            assertNull(map.findInstance(String.class));
            assertNull(map.findInstance(Integer.class));
            assertNull(map.findMetaData(loader.loadClass("provided.retry.ReadyFactory")));
            assertSame(unrelated, map.getInstance(Long.class));
            loader.loadClass("provided.retry.TransientFactory").getField("fail").setBoolean(null, false);
            helper.setupDISystem(loader);
            assertEquals("ready", map.getInstance(String.class));
            assertEquals(7, map.getInstance(Integer.class));
            assertEquals(2, loader.loadClass("provided.retry.ReadyFactory").getField("creations").getInt(null));
        } finally { map.clear(); }
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
