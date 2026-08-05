package org.tavall.dependency.access;

import org.junit.jupiter.api.Test;
import org.tavall.dependency.annotations.DelegatesTo;

import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependencyAccessGrantProcessorTest {

    @Test
    void generatedAccessSourceCompilesWithTypedGetters() throws Exception {
        DependencyAccessSourceLowerer lowerer = new DependencyAccessSourceLowerer();
        Map<String, String> loweredSources = lowerer.lowerSources(buildSources(
                "IPlayerRegistry",
                "IPlayerDataRepository",
                "IEconomyService"));
        Path outputDirectory = DependencyAccessTestCompiler.compileSources(loweredSources);

        try (URLClassLoader loader = DependencyAccessTestCompiler.createClassLoader(outputDirectory)) {
            Class<?> handlerClass = Class.forName("org.example.RewardHandler", true, loader);
            Class<?> accessClass = Class.forName(
                    "org.example.RewardHandlerDependencyAccess",
                    true,
                    loader
            );

            assertNotNull(handlerClass.getAnnotation(DelegatesTo.class));
            DelegatesTo generatedDelegation = accessClass.getAnnotation(DelegatesTo.class);
            assertNotNull(generatedDelegation);
            assertEquals(0, generatedDelegation.value().length);

            Set<String> methodNames = Arrays.stream(accessClass.getDeclaredMethods())
                    .map(Method::getName)
                    .filter(name -> !name.equals("getDependencyMap"))
                    .collect(Collectors.toSet());
            assertEquals(Set.of("playerRegistry", "playerDataRepository", "economyService"), methodNames);
        }
    }

    @Test
    void generatedAccessSourceCompilesWithTenDependencies() throws Exception {
        Map<String, String> loweredSources = new DependencyAccessSourceLowerer().lowerSources(buildSources(
                "IDependencyOne",
                "IDependencyTwo",
                "IDependencyThree",
                "IDependencyFour",
                "IDependencyFive",
                "IDependencySix",
                "IDependencySeven",
                "IDependencyEight",
                "IDependencyNine",
                "IDependencyTen"));
        Path outputDirectory = DependencyAccessTestCompiler.compileSources(loweredSources);

        try (URLClassLoader loader = DependencyAccessTestCompiler.createClassLoader(outputDirectory)) {
            Class<?> accessClass = Class.forName(
                    "org.example.RewardHandlerDependencyAccess",
                    true,
                    loader
            );

            long accessorCount = Arrays.stream(accessClass.getDeclaredMethods())
                    .filter(method -> !method.getName().equals("getDependencyMap"))
                    .count();
            assertEquals(10L, accessorCount);
            assertTrue(Arrays.stream(accessClass.getDeclaredMethods())
                    .anyMatch(method -> method.getName().equals("dependencyTen")));
        }
    }

    private Map<String, String> buildSources(String... dependencyTypes) {
        Map<String, String> sources = new LinkedHashMap<>();
        for (String dependencyType : dependencyTypes) {
            sources.put("org.example." + dependencyType, """
                    package org.example;

                    public interface %s {
                    }
                    """.formatted(dependencyType));
        }
        sources.put("org.example.RewardHandler", """
                package org.example;

                import org.tavall.dependency.DependencyAccess;
                import org.tavall.dependency.annotations.DelegatesTo;

                @DelegatesTo
                public final class RewardHandler
                        implements DependencyAccess<%s> {
                }
                """.formatted(String.join(", ", dependencyTypes)));
        return sources;
    }
}
