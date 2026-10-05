package org.tavall.dependency.architecture;

import org.tavall.dependency.annotations.DelegatesTo;

import java.io.IOException;
import java.io.InputStream;
import java.lang.classfile.ClassFile;
import java.lang.classfile.CodeElement;
import java.lang.classfile.Opcode;
import java.lang.classfile.instruction.InvokeInstruction;
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
import java.util.concurrent.ConcurrentHashMap;

public final class DependencyArchitecturePolicy {
    public static final String MANAGED_CONSTRUCTOR_INJECTION_RULE = "managed-constructor-injection";
    public static final String BUILDER_MANAGED_CONSTRUCTOR_INJECTION_RULE = "builder-managed-constructor-injection";
    public static final String CONSTRUCTOR_BYTECODE_RULE = "managed-constructor-bytecode";

    private static final Map<Class<?>, ConstructorScan> CONSTRUCTOR_SCAN_CACHE = new ConcurrentHashMap<>();

    private DependencyArchitecturePolicy() {
    }

    /**
     * Audits one production type that declares constructors accepting Tavall-managed dependency tokens.
     *
     * <p>The constructor declaration itself is not forbidden for a normal constructed type. The rule is about
     * composition ownership: every direct call to a dependency-bearing constructor must originate from a
     * {@code *Builder}. Builders themselves must not receive Tavall-managed dependencies through their own
     * constructors; they consume the owning DI map/access surface and then assemble the target object.</p>
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

        List<Violation> violations = new ArrayList<>();
        if (isBuilder(constructedType)) {
            managedConstructors.forEach((descriptor, managedParameters) -> violations.add(new Violation(
                    BUILDER_MANAGED_CONSTRUCTOR_INJECTION_RULE,
                    constructedType.getName()
                            + " is a Builder whose constructor accepts Tavall-managed dependency token(s) "
                            + typeNames(managedParameters)
                            + ". Builders resolve managed dependencies through DependencyAccess/IDependencyMap; "
                            + "their constructor inputs are reserved for values or externally owned platform objects."
            )));
            return List.copyOf(violations);
        }

        String targetInternalName = constructedType.getName().replace('.', '/');
        for (Class<?> callerType : productionTypes) {
            if (callerType == null || callerType == constructedType) {
                continue;
            }

            ConstructorScan scan = constructorCalls(callerType);
            if (scan.failure() != null) {
                violations.add(new Violation(
                        CONSTRUCTOR_BYTECODE_RULE,
                        "Could not inspect constructor call sites in "
                                + callerType.getName()
                                + ": "
                                + scan.failure()
                ));
                continue;
            }

            scan.calls().stream()
                    .filter(call -> call.ownerInternalName().equals(targetInternalName))
                    .filter(call -> managedConstructors.containsKey(call.descriptor()))
                    .filter(call -> !isBuilder(callerType))
                    .forEach(call -> violations.add(new Violation(
                            MANAGED_CONSTRUCTOR_INJECTION_RULE,
                            callerType.getName()
                                    + "#"
                                    + call.callingMethod()
                                    + " directly constructs "
                                    + constructedType.getName()
                                    + call.descriptor()
                                    + " with Tavall-managed dependency token(s) "
                                    + typeNames(managedConstructors.get(call.descriptor()))
                                    + ". Move dependency-bearing construction into a *Builder."
                    )));
        }

        return List.copyOf(violations);
    }

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

    public static boolean isBuilder(Class<?> type) {
        Objects.requireNonNull(type, "type");
        return type.getSimpleName().endsWith("Builder");
    }

    static void clearConstructorScanCacheForTests() {
        CONSTRUCTOR_SCAN_CACHE.clear();
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

    private static ConstructorScan constructorCalls(Class<?> callerType) {
        return CONSTRUCTOR_SCAN_CACHE.computeIfAbsent(
                callerType,
                DependencyArchitecturePolicy::scanConstructorCalls
        );
    }

    private static ConstructorScan scanConstructorCalls(Class<?> callerType) {
        String resourceName = callerType.getName().replace('.', '/') + ".class";
        ClassLoader classLoader = callerType.getClassLoader();
        if (classLoader == null) {
            return new ConstructorScan(
                    Set.of(),
                    "No class loader is available for " + callerType.getName()
            );
        }

        try (InputStream input = classLoader.getResourceAsStream(resourceName)) {
            if (input == null) {
                return new ConstructorScan(
                        Set.of(),
                        "Class bytes are unavailable at " + resourceName
                );
            }

            var classModel = ClassFile.of().parse(input.readAllBytes());
            Set<ConstructorCall> calls = new LinkedHashSet<>();
            classModel.methods().forEach(method -> method.code().ifPresent(code -> {
                for (CodeElement element : code) {
                    if (!(element instanceof InvokeInstruction invocation)) {
                        continue;
                    }
                    if (invocation.opcode() != Opcode.INVOKESPECIAL) {
                        continue;
                    }
                    if (!invocation.method().name().equalsString("<init>")) {
                        continue;
                    }

                    calls.add(new ConstructorCall(
                            invocation.method().owner().asInternalName(),
                            invocation.method().type().stringValue(),
                            method.methodName().stringValue()
                    ));
                }
            }));
            return new ConstructorScan(Set.copyOf(calls), null);
        } catch (IOException | RuntimeException exception) {
            return new ConstructorScan(
                    Set.of(),
                    exception.getClass().getSimpleName() + ": " + String.valueOf(exception.getMessage())
            );
        }
    }

    private static String typeNames(Collection<Class<?>> types) {
        return types.stream()
                .map(Class::getName)
                .sorted()
                .toList()
                .toString();
    }

    private record ConstructorScan(Set<ConstructorCall> calls, String failure) {
        private ConstructorScan {
            calls = Set.copyOf(calls);
        }
    }

    private record ConstructorCall(
            String ownerInternalName,
            String descriptor,
            String callingMethod
    ) {
    }

    public record Violation(String rule, String message) {
        public Violation {
            Objects.requireNonNull(rule, "rule");
            Objects.requireNonNull(message, "message");
        }
    }
}
