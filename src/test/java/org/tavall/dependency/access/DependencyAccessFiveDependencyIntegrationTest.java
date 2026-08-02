package org.tavall.dependency.access;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.tavall.dependency.maps.DependencyMap;

import java.lang.reflect.Proxy;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class DependencyAccessFiveDependencyIntegrationTest {

    @AfterEach
    void clearDependencies() {
        DependencyMap.getDependencyMap().clear();
    }

    @Test
    void resolvesFiveGeneratedDependenciesAndObservesReplacement() throws Exception {
        Map<String, String> loweredSources = new DependencyAccessSourceLowerer()
                .lowerSources(buildSources());
        Path outputDirectory = DependencyAccessTestCompiler.compileSources(loweredSources);

        try (URLClassLoader loader = DependencyAccessTestCompiler.createClassLoader(outputDirectory)) {
            Class<?> handlerType = Class.forName("org.example.Handler", true, loader);
            Class<?> accessType = Class.forName("org.example.HandlerDependencyAccess", true, loader);
            Class<?> dependencyOneType = Class.forName("org.example.IDependencyOne", true, loader);

            Object firstDependency = proxy(loader, dependencyOneType);
            Object replacementDependency = proxy(loader, dependencyOneType);
            register(dependencyOneType, firstDependency);
            register(Class.forName("org.example.IDependencyTwo", true, loader),
                    proxy(loader, Class.forName("org.example.IDependencyTwo", true, loader)));
            register(Class.forName("org.example.IDependencyThree", true, loader),
                    proxy(loader, Class.forName("org.example.IDependencyThree", true, loader)));
            register(Class.forName("org.example.IDependencyFour", true, loader),
                    proxy(loader, Class.forName("org.example.IDependencyFour", true, loader)));
            register(Class.forName("org.example.IDependencyFive", true, loader),
                    proxy(loader, Class.forName("org.example.IDependencyFive", true, loader)));

            Object access = accessType.getDeclaredConstructor().newInstance();
            register(accessType, access);
            Object handler = handlerType.getDeclaredConstructor().newInstance();

            assertSame(access, handlerType.getMethod("getInstance").invoke(handler));
            assertSame(firstDependency, accessType.getMethod("dependencyOne").invoke(access));
            long accessorCount = Arrays.stream(accessType.getDeclaredMethods())
                    .filter(method -> !method.getName().equals("getDependencyMap"))
                    .count();
            assertEquals(5L, accessorCount);

            replace(dependencyOneType, replacementDependency);
            assertSame(replacementDependency, accessType.getMethod("dependencyOne").invoke(access));
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void register(Class<?> dependencyType, Object instance) {
        DependencyMap.getDependencyMap().registerInstance((Class) dependencyType, instance);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void replace(Class<?> dependencyType, Object instance) {
        DependencyMap.getDependencyMap().replaceInstance((Class) dependencyType, () -> instance);
    }

    private Object proxy(ClassLoader loader, Class<?> dependencyType) {
        return Proxy.newProxyInstance(loader, new Class<?>[]{dependencyType}, (proxy, method, arguments) -> null);
    }

    private Map<String, String> buildSources() {
        Map<String, String> sources = new LinkedHashMap<>();
        for (int index = 1; index <= 5; index++) {
            String name = switch (index) {
                case 1 -> "IDependencyOne";
                case 2 -> "IDependencyTwo";
                case 3 -> "IDependencyThree";
                case 4 -> "IDependencyFour";
                default -> "IDependencyFive";
            };
            sources.put("org.example." + name, """
                    package org.example;

                    public interface %s {
                    }
                    """.formatted(name));
        }
        sources.put("org.example.Handler", """
                package org.example;

                import org.tavall.dependency.DependencyAccess;
                import org.tavall.dependency.annotations.DelegatesTo;

                @DelegatesTo
                public final class Handler
                        implements DependencyAccess<
                                IDependencyOne,
                                IDependencyTwo,
                                IDependencyThree,
                                IDependencyFour,
                                IDependencyFive
                        > {
                }
                """);
        return sources;
    }
}
