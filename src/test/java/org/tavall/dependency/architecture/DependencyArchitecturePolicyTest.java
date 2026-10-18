package org.tavall.dependency.architecture;

import org.junit.jupiter.api.Test;
import org.tavall.dependency.annotations.DelegatesTo;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DependencyArchitecturePolicyTest {
    @Test
    void rejectsManagedDependencyInjectionIntoOrdinaryConstructors() {
        var violations = DependencyArchitecturePolicy.auditManagedConstructorInjection(
                InvalidHandler.class,
                productionTypes()
        );

        assertTrue(violations.stream().anyMatch(violation ->
                violation.rule().equals(DependencyArchitecturePolicy.MANAGED_CONSTRUCTOR_INJECTION_RULE)
                        && violation.message().contains(IManagedDependency.class.getName())
        ));
    }

    @Test
    void builderOwnershipDoesNotPermitManagedDependencyConstructorInjection() {
        var violations = DependencyArchitecturePolicy.auditManagedConstructorInjection(
                BuilderOwnedHandler.class,
                productionTypes()
        );

        assertTrue(violations.stream().anyMatch(violation ->
                violation.rule().equals(DependencyArchitecturePolicy.MANAGED_CONSTRUCTOR_INJECTION_RULE)
                        && violation.message().contains(BuilderOwnedHandler.class.getName())
        ));
    }

    @Test
    void rejectsManagedDependencyInjectionIntoBuilderConstructors() {
        var violations = DependencyArchitecturePolicy.auditManagedConstructorInjection(
                InvalidHandlerBuilder.class,
                productionTypes()
        );

        assertTrue(violations.stream().anyMatch(violation ->
                violation.rule().equals(DependencyArchitecturePolicy.BUILDER_MANAGED_CONSTRUCTOR_INJECTION_RULE)
        ));
    }

    @Test
    void acceptsValueConstructorsThatDoNotCarryManagedDependencies() {
        assertTrue(DependencyArchitecturePolicy.auditManagedConstructorInjection(
                ValueObject.class,
                productionTypes()
        ).isEmpty());
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
                BuilderOwnedHandler.class,
                BuilderOwnedHandlerBuilder.class,
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

    private static final class BuilderOwnedHandler {
        private BuilderOwnedHandler(IManagedDependency dependency) {
        }
    }

    private static final class BuilderOwnedHandlerBuilder {
        private BuilderOwnedHandler build(IManagedDependency dependency) {
            return new BuilderOwnedHandler(dependency);
        }
    }

    private static final class InvalidHandlerBuilder {
        private InvalidHandlerBuilder(IManagedDependency dependency) {
        }
    }

    private record ValueObject(String value) {
    }
}
