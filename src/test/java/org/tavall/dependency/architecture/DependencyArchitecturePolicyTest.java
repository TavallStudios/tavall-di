package org.tavall.dependency.architecture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tavall.dependency.annotations.DelegatesTo;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DependencyArchitecturePolicyTest {
    @BeforeEach
    void clearPolicyCache() {
        DependencyArchitecturePolicy.clearConstructorScanCacheForTests();
    }

    @Test
    void rejectsManagedDependencyConstructionFromNonBuilderCallers() {
        var violations = DependencyArchitecturePolicy.auditManagedConstructorInjection(
                InvalidHandler.class,
                productionTypes()
        );

        assertTrue(violations.stream().anyMatch(violation ->
                violation.rule().equals(DependencyArchitecturePolicy.MANAGED_CONSTRUCTOR_INJECTION_RULE)
                        && violation.message().contains(InvalidHandlerFactory.class.getName())
        ));
    }

    @Test
    void acceptsBuilderOwnedManagedConstructorComposition() {
        var violations = DependencyArchitecturePolicy.auditManagedConstructorInjection(
                ValidHandler.class,
                productionTypes()
        );

        assertTrue(violations.isEmpty(), () -> "Unexpected violations: " + violations);
    }

    @Test
    void rejectsManagedDependencyInjectionIntoBuilderConstructor() {
        var violations = DependencyArchitecturePolicy.auditManagedConstructorInjection(
                InvalidHandlerBuilder.class,
                productionTypes()
        );

        assertTrue(violations.stream().anyMatch(violation ->
                violation.rule().equals(DependencyArchitecturePolicy.BUILDER_MANAGED_CONSTRUCTOR_INJECTION_RULE)
        ));
    }

    @Test
    void acceptsValueConstructorsThatAreNotManagedDependencies() {
        var violations = DependencyArchitecturePolicy.auditManagedConstructorInjection(
                ValueObject.class,
                productionTypes()
        );

        assertTrue(violations.isEmpty());
    }

    @Test
    void exposesConcreteAndDelegatedTokensAsManagedDependencies() {
        var tokens = DependencyArchitecturePolicy.managedDependencyTokens(productionTypes());
        assertFalse(tokens.isEmpty());
        assertTrue(tokens.contains(ManagedDependency.class));
        assertTrue(tokens.contains(IManagedDependency.class));
    }

    private List<Class<?>> productionTypes() {
        return List.of(
                ManagedDependency.class,
                InvalidHandler.class,
                InvalidHandlerFactory.class,
                ValidHandler.class,
                ValidHandlerBuilder.class,
                InvalidHandlerBuilder.class,
                ValueObject.class
        );
    }

    private interface IManagedDependency {
    }

    @DelegatesTo(IManagedDependency.class)
    private static final class ManagedDependency implements IManagedDependency {
    }

    private static final class InvalidHandler {
        private InvalidHandler(IManagedDependency dependency) {
        }
    }

    private static final class InvalidHandlerFactory {
        private InvalidHandler build(IManagedDependency dependency) {
            return new InvalidHandler(dependency);
        }
    }

    private static final class ValidHandler {
        private ValidHandler(IManagedDependency dependency) {
        }
    }

    private static final class ValidHandlerBuilder {
        private ValidHandler build(IManagedDependency dependency) {
            return new ValidHandler(dependency);
        }
    }

    private static final class InvalidHandlerBuilder {
        private InvalidHandlerBuilder(IManagedDependency dependency) {
        }
    }

    private record ValueObject(String value) {
    }
}
