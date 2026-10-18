package org.tavall.dependency.architecture;

import org.tavall.dependency.annotations.DelegatesTo;

import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Shared architecture rules for preserving Tavall dependency ownership. */
public final class DependencyArchitecturePolicy {
    /** Stable identifier for managed dependency injection through ordinary constructors. */
    public static final String MANAGED_CONSTRUCTOR_INJECTION_RULE = "managed-constructor-injection";

    /** Stable identifier for managed dependency injection through builder constructors. */
    public static final String BUILDER_MANAGED_CONSTRUCTOR_INJECTION_RULE = "builder-managed-constructor-injection";

    private DependencyArchitecturePolicy() {
    }

    /**
     * Audits one production type for constructors that accept Tavall-managed dependency tokens.
     *
     * <p>Managed dependencies are resolved through {@code DependencyAccess}; direct constructor injection is
     * prohibited for ordinary production types and builders alike. A builder can receive values or genuinely
     * externally owned platform handles, but it must not become a second Tavall dependency graph.</p>
     *
     * @param constructedType production type whose constructors are audited
     * @param productionTypes production types used to discover managed dependency tokens
     * @return one violation per constructor that accepts a managed dependency token
     */
    public static List<Violation> auditManagedConstructorInjection(
            Class<?> constructedType,
            Collection<Class<?>> productionTypes
    ) {
        Objects.requireNonNull(constructedType, "constructedType");
        Objects.requireNonNull(productionTypes, "productionTypes");

        Set<Class<?>> managedDependencyTokens = managedDependencyTokens(productionTypes);
        Map<String, Set<Class<?>>> managedConstructors = managedConstructors(
                constructedType,
                managedDependencyTokens
        );
        if (managedConstructors.isEmpty()) {
            return List.of();
        }

        String rule = isBuilder(constructedType)
                ? BUILDER_MANAGED_CONSTRUCTOR_INJECTION_RULE
                : MANAGED_CONSTRUCTOR_INJECTION_RULE;
        List<Violation> violations = new ArrayList<>();
        managedConstructors.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(constructor -> violations.add(new Violation(
                        rule,
                        constructedType.getName()
                                + " declares constructor "
                                + constructor.getKey()
                                + " accepting Tavall-managed dependency token(s) "
                                + typeNames(constructor.getValue())
                                + ". Resolve managed dependencies through DependencyAccess."
                )));
        return List.copyOf(violations);
    }

    /**
     * Finds each annotated production type and the interface contracts it delegates.
     *
     * @param productionTypes types inspected for {@link DelegatesTo}
     * @return the concrete managed types and delegated contracts
     */
    public static Set<Class<?>> managedDependencyTokens(Collection<Class<?>> productionTypes) {
        Objects.requireNonNull(productionTypes, "productionTypes");
        Set<Class<?>> managedTokens = new HashSet<>();
        for (Class<?> type : productionTypes) {
            if (type == null) {
                continue;
            }
            DelegatesTo delegatesTo = type.getAnnotation(DelegatesTo.class);
            if (delegatesTo == null) {
                continue;
            }
            managedTokens.add(type);
            managedTokens.addAll(Arrays.asList(delegatesTo.value()));
        }
        return Set.copyOf(managedTokens);
    }

    /**
     * Reports whether the type follows the repository's {@code *Builder} naming convention.
     *
     * @param type type to inspect
     * @return true when the simple class name ends in {@code Builder}
     */
    public static boolean isBuilder(Class<?> type) {
        Objects.requireNonNull(type, "type");
        return type.getSimpleName().endsWith("Builder");
    }

    private static Map<String, Set<Class<?>>> managedConstructors(
            Class<?> type,
            Set<Class<?>> managedDependencyTokens
    ) {
        Map<String, Set<Class<?>>> constructors = new LinkedHashMap<>();
        for (Constructor<?> constructor : type.getDeclaredConstructors()) {
            if (constructor.isSynthetic()) {
                continue;
            }

            Set<Class<?>> managedParameters = new LinkedHashSet<>();
            for (Class<?> parameterType : constructor.getParameterTypes()) {
                if (managedDependencyTokens.contains(parameterType)) {
                    managedParameters.add(parameterType);
                }
            }
            if (managedParameters.isEmpty()) {
                continue;
            }

            String descriptor = MethodType
                    .methodType(void.class, constructor.getParameterTypes())
                    .toMethodDescriptorString();
            constructors.put(descriptor, Set.copyOf(managedParameters));
        }
        return Map.copyOf(constructors);
    }

    private static String typeNames(Collection<Class<?>> types) {
        return types.stream()
                .map(Class::getName)
                .sorted()
                .toList()
                .toString();
    }

    /**
     * One architecture policy violation.
     *
     * @param rule stable rule identifier
     * @param message actionable explanation of the violation
     */
    public record Violation(String rule, String message) {
        public Violation {
            Objects.requireNonNull(rule, "rule");
            Objects.requireNonNull(message, "message");
        }
    }
}
