package org.tavall.dependency.module;

import org.tavall.dependency.IDependencyInjectableConcrete;
import org.tavall.dependency.annotations.DelegatesTo;
import org.tavall.util.CustomEnum;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * Production implementation of {@link IModuleProfileResolver}.
 *
 * <p>Expands requested or deployment module profiles into their effective dependency-first module
 * load orders.</p>
 *
 * <p>The resolver is intentionally ignorant of any product, platform, or server type. Callers own
 * the module catalog and provide only the required-dependency relation for their module domain.</p>
 *
 * <p><b>Safety Guarantees:</b></p>
 * <ul>
 *   <li>Rejects conflict where a module is both enabled and disabled.</li>
 *   <li>Fails closed when an enabled module requires an explicitly disabled dependency; does not silently re-enable.</li>
 *   <li>Rejects unknown/unavailable modules in requested sets and dependency graphs.</li>
 *   <li>Rejects dependency cycles.</li>
 * </ul>
 *
 * @param <M> module identifier type extending canonical {@link CustomEnum}
 * @param <P> profile identifier type extending canonical {@link CustomEnum}
 */
@DelegatesTo(IModuleProfileResolver.class)
public class ModuleProfileResolver<M extends CustomEnum<M>, P extends CustomEnum<P>>
        implements IModuleProfileResolver<M, P>, IDependencyInjectableConcrete {

    public ModuleProfileResolver() {
    }

    @Override
    public EffectiveModuleProfile<M, P> resolve(
            ModuleProfile<M, P> profile,
            Collection<M> availableModules,
            Function<? super M, ? extends Collection<M>> requiredDependencies
    ) {
        Objects.requireNonNull(profile, "profile");
        Objects.requireNonNull(availableModules, "availableModules");
        Objects.requireNonNull(requiredDependencies, "requiredDependencies");

        Set<M> available = validateAvailable(availableModules);
        Set<M> emptyDisabled = Collections.emptySet();

        Map<M, VisitState> visitStates = new HashMap<>();
        List<M> loadOrder = new ArrayList<>();
        for (M module : profile.modules()) {
            visit(module, available, emptyDisabled, requiredDependencies, visitStates, loadOrder, null);
        }
        return new EffectiveModuleProfile<>(profile.id(), profile.modules(), loadOrder);
    }

    @Override
    public <R extends CustomEnum<R>> EffectiveDeploymentModuleProfile<M, P, R> resolve(
            DeploymentModuleProfile<M, P, R> profile,
            Collection<M> availableModules,
            Function<? super M, ? extends Collection<M>> requiredDependencies
    ) {
        Objects.requireNonNull(profile, "profile");
        Objects.requireNonNull(availableModules, "availableModules");
        Objects.requireNonNull(requiredDependencies, "requiredDependencies");

        Set<M> available = validateAvailable(availableModules);

        // Reject unsupported schema versions
        if (!DeploymentModuleProfile.isSupportedSchemaVersion(profile.schemaVersion())) {
            throw new IllegalArgumentException(
                    "Unsupported deployment profile schema version: " + profile.schemaVersion()
                            + " (supported: " + DeploymentModuleProfile.SUPPORTED_SCHEMA_VERSIONS + ")"
            );
        }

        // Validate that all explicitly disabled modules exist in the available artifact catalog
        for (M disabled : profile.disabledModules()) {
            if (!available.contains(disabled)) {
                throw new IllegalArgumentException(
                        "Unknown disabled module in deployment profile: " + disabled
                                + " (not present in available catalog)"
                );
            }
        }

        // Explicit validation: reject enabled/disabled conflict
        for (M enabled : profile.enabledModules()) {
            if (profile.disabledModules().contains(enabled)) {
                throw new IllegalArgumentException("Module cannot be both enabled and disabled: " + enabled);
            }
        }

        Map<M, VisitState> visitStates = new HashMap<>();
        List<M> loadOrder = new ArrayList<>();
        for (M module : profile.enabledModules()) {
            visit(module, available, profile.disabledModules(), requiredDependencies, visitStates, loadOrder, null);
        }

        return new EffectiveDeploymentModuleProfile<>(
                profile.schemaVersion(),
                profile.profileId(),
                profile.runtimeTarget(),
                profile.configurationGeneration(),
                profile.enabledModules(),
                profile.disabledModules(),
                loadOrder
        );
    }

    private Set<M> validateAvailable(Collection<M> availableModules) {
        Set<M> available = new LinkedHashSet<>(availableModules);
        if (available.contains(null)) {
            throw new IllegalArgumentException("Available modules cannot contain null");
        }
        return available;
    }

    private void visit(
            M module,
            Set<M> available,
            Set<M> disabledModules,
            Function<? super M, ? extends Collection<M>> requiredDependencies,
            Map<M, VisitState> visitStates,
            List<M> loadOrder,
            M requester
    ) {
        if (disabledModules.contains(module)) {
            if (requester != null) {
                throw new IllegalStateException(
                        "Required dependency is explicitly disabled: " + module + " required by " + requester
                );
            } else {
                throw new IllegalArgumentException("Requested module is explicitly disabled: " + module);
            }
        }

        if (!available.contains(module)) {
            throw new IllegalArgumentException("Unknown module in profile dependency graph: " + module);
        }

        VisitState state = visitStates.get(module);
        if (state == VisitState.VISITED) {
            return;
        }
        if (state == VisitState.VISITING) {
            throw new IllegalStateException("Required module dependency cycle detected at: " + module);
        }

        visitStates.put(module, VisitState.VISITING);
        Collection<M> dependencies = requiredDependencies.apply(module);
        if (dependencies != null) {
            for (M dependency : dependencies) {
                if (dependency == null) {
                    throw new IllegalArgumentException("Required dependencies cannot contain null: " + module);
                }
                visit(dependency, available, disabledModules, requiredDependencies, visitStates, loadOrder, module);
            }
        }
        visitStates.put(module, VisitState.VISITED);
        loadOrder.add(module);
    }

    private enum VisitState {
        VISITING,
        VISITED
    }
}
