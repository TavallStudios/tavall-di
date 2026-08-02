package org.tavall.dependency.access;

import org.junit.jupiter.api.Test;
import org.tavall.dependency.annotations.DelegatesTo;

import java.lang.reflect.Method;
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
    void generatedAccessSourceCompilesWithTypedGetters() {
        DependencyAccessSourceLowerer lowerer = new DependencyAccessSourceLowerer();
        Map<String, String> loweredSources = lowerer.lowerSources(buildSources(
                "IPlayerRegistry",
                "IPlayerDataRepository",
                "IEconomyService"));
        Path outputDirectory = DependencyAccessTestCompiler.compileSources(loweredSources);

        Class<?> handlerClass = DependencyAccessTestCompiler.loadClass(
                outputDirectory,
                "org.example.RewardHandler");
        Class<?> accessClass = DependencyAccessTestCompiler.loadClass(
                outputDirectory,
                "org.example.RewardHandlerDependencyAccess");

        assertNotNull(handlerClass.getAnnotation(DelegatesTo.class));
        assertNotNull(accessClass.getAnnotation(DelegatesTo.class));
        Set<String> methodNames = Arrays.stream(accessClass.getDeclaredMethods())
                .map(Method::getName)
                .collect(Collectors.toSet());
        assertEquals(Set.of("playerRegistry", "playerDataRepository", "economyService"), methodNames);
    }

    @Test
    void generatedAccessSourceCompilesWithTenDependencies() {
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

        Class<?> accessClass = DependencyAccessTestCompiler.loadClass(
                outputDirectory,
                "org.example.RewardHandlerDependencyAccess");

        assertEquals(10, accessClass.getDeclaredMethods().length);
        assertTrue(Arrays.stream(accessClass.getDeclaredMethods())
                .anyMatch(method -> method.getName().equals("dependencyTen")));
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
