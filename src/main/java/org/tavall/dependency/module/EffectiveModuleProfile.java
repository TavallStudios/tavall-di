package org.tavall.dependency.module;

import org.tavall.identity.CustomEnum;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Resolved module profile after all technical required dependencies have been expanded.
 * The load order is dependency-first and deterministic for deterministic profile/dependency input.
 */
public record EffectiveModuleProfile<M extends CustomEnum<M>, P extends CustomEnum<P>>(
        P id,
        Set<M> requestedModules,
        List<M> loadOrder
) {
    public EffectiveModuleProfile {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(requestedModules, "requestedModules");
        Objects.requireNonNull(loadOrder, "loadOrder");
        requestedModules = Collections.unmodifiableSet(new LinkedHashSet<>(requestedModules));
        loadOrder = List.copyOf(loadOrder);
    }

    public Set<M> modules() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(loadOrder));
    }
}
