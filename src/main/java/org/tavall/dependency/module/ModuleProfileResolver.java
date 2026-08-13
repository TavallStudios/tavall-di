package org.tavall.dependency.module;

import org.tavall.identity.CustomEnum;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * Expands a requested module profile into its effective dependency-first module load order.
 *
 * <p>The resolver is intentionally ignorant of any product, platform, or server type. Callers own
 * the module catalog and provide only the required-dependency relation for their module domain.</p>
 */
public final class ModuleProfileResolver<M extends CustomEnum<M>, P extends CustomEnum<P>> {

    public EffectiveModuleProfile<M, P> resolve(
            ModuleProfile<M, P> profile,
            Collection<M> availableModules,
            Function<? super M, ? extends Collection<M>> requiredDependencies
    ) {
        Objects.requireNonNull(profile, "profile");
        Objects.requireNonNull(availableModules, "availableModules");
        Objects.requireNonNull(requiredDependencies, "requiredDependencies");

        Set<M> available = new LinkedHashSet<>(availableModules);
        if (available.contains(null)) {
            throw new IllegalArgumentException("Available modules cannot contain null");
        }

        Map<M, VisitState> visitStates = new HashMap<>();
        List<M> loadOrder = new ArrayList<>();
        for (M module : profile.modules()) {
            visit(module, available, requiredDependencies, visitStates, loadOrder);
        }
        return new EffectiveModuleProfile<>(profile.id(), profile.modules(), loadOrder);
    }

    private void visit(
            M module,
            Set<M> available,
            Function<? super M, ? extends Collection<M>> requiredDependencies,
            Map<M, VisitState> visitStates,
            List<M> loadOrder
    ) {
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
                visit(dependency, available, requiredDependencies, visitStates, loadOrder);
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
