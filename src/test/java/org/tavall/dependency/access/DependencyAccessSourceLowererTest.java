package org.tavall.dependency.access;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependencyAccessSourceLowererTest {

    @Test
    void preservesSingleDependencyAccessWithoutGeneration() {
        Map<String, String> lowered = new DependencyAccessSourceLowerer()
                .lowerSources(sourcesFor("IRepository"));

        assertEquals(2, lowered.size());
        assertTrue(lowered.get("org.example.Handler")
                .contains("DependencyAccess<IRepository>"));
        assertFalse(lowered.containsKey("org.example.HandlerDependencyAccess"));
    }

    @Test
    void generatesTypedAccessForTwoToFourDependencies() {
        Map<String, String> lowered = new DependencyAccessSourceLowerer()
                .lowerSources(sourcesFor("IPlayerData", "IEconomyService", "RewardAuditLogger"));

        String handler = lowered.get("org.example.Handler");
        String generated = lowered.get("org.example.HandlerDependencyAccess");

        assertTrue(handler.contains("DependencyAccess<HandlerDependencyAccess>"));
        assertTrue(generated.contains("IPlayerData playerData()"));
        assertTrue(generated.contains("IEconomyService economyService()"));
        assertTrue(generated.contains("RewardAuditLogger rewardAuditLogger()"));
        assertTrue(generated.contains("getDependencyMap().getInstance(IPlayerData.class)"));

        int delegatesToIndex = generated.indexOf(
                "@org.tavall.dependency.annotations.DelegatesTo");
        int classDeclarationIndex = generated.indexOf(
                "public final class HandlerDependencyAccess");
        assertTrue(delegatesToIndex >= 0);
        assertTrue(classDeclarationIndex > delegatesToIndex);
    }

    @Test
    void supportsMoreThanFourDependenciesWithoutAnArityLimit() {
        Map<String, String> lowered = new DependencyAccessSourceLowerer()
                .lowerSources(sourcesFor(
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

        String generated = lowered.get("org.example.HandlerDependencyAccess");
        assertTrue(generated.contains("dependencyOne()"));
        assertTrue(generated.contains("dependencyTen()"));
    }

    @Test
    void lowersLeadingAcronymsToLowerCamelAccessors() {
        Map<String, String> lowered = new DependencyAccessSourceLowerer()
                .lowerSources(sourcesFor("FFAEntityTransaction", "ISecondDependency"));

        String generated = lowered.get("org.example.HandlerDependencyAccess");
        assertTrue(generated.contains("FFAEntityTransaction ffaEntityTransaction()"));
    }

    @Test
    void rejectsExpandedAccessWithoutDelegatesTo() {
        Map<String, String> sources = sourcesFor("IDependencyOne", "IDependencyTwo");
        sources.put("org.example.Handler", sources.get("org.example.Handler")
                .replace("@DelegatesTo\n", ""));

        IllegalArgumentException failure = assertThrows(
                IllegalArgumentException.class,
                () -> new DependencyAccessSourceLowerer().lowerSources(sources));

        assertTrue(failure.getMessage().contains("requires @DelegatesTo"));
    }

    @Test
    void rejectsWildcardsTypeVariablesDuplicatesAndAccessorCollisions() {
        assertInvalid("? extends IDependencyOne", "IDependencyTwo", "wildcard");
        assertInvalid("Value", "IDependencyTwo", "type variable");
        assertInvalid("IDependencyOne", "IDependencyOne", "duplicate");
        assertInvalid("first.IPlayerData", "second.IPlayerData", "collision");
    }

    private void assertInvalid(String firstType, String secondType, String expectedMessage) {
        Map<String, String> sources = sourcesFor(firstType, secondType);
        sources.put("org.example.Handler", sources.get("org.example.Handler")
                .replace("public final class Handler", "public final class Handler<Value>"));

        IllegalArgumentException failure = assertThrows(
                IllegalArgumentException.class,
                () -> new DependencyAccessSourceLowerer().lowerSources(sources));

        assertTrue(failure.getMessage().contains(expectedMessage));
    }

    private Map<String, String> sourcesFor(String... dependencyTypes) {
        Map<String, String> sources = new LinkedHashMap<>();
        for (String dependencyType : dependencyTypes) {
            if (dependencyType.contains("?") || dependencyType.equals("Value")) {
                continue;
            }
            String simpleName = dependencyType.substring(dependencyType.lastIndexOf('.') + 1);
            String packageName = dependencyType.contains(".")
                    ? dependencyType.substring(0, dependencyType.lastIndexOf('.'))
                    : "org.example";
            sources.put(packageName + "." + simpleName, """
                    package %s;

                    public interface %s {
                    }
                    """.formatted(packageName, simpleName));
        }

        sources.put("org.example.Handler", """
                package org.example;

                import org.tavall.dependency.DependencyAccess;
                import org.tavall.dependency.annotations.DelegatesTo;

                @DelegatesTo
                public final class Handler
                        implements DependencyAccess<%s> {
                }
                """.formatted(String.join(", ", dependencyTypes)));
        return sources;
    }
}
