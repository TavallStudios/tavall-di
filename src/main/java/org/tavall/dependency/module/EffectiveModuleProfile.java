package org.tavall.dependency.module;

import org.tavall.util.CustomEnum;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Resolved module profile after all technical required dependencies have been expanded.
 * The load order is dependency-first and deterministic for deterministic profile/dependency input.
 *
 * @param <M> module identifier type extending canonical {@link CustomEnum}
 * @param <P> profile identifier type extending canonical {@link CustomEnum}
 * @param id the profile identifier
 * @param requestedModules the initially requested modules
 * @param loadOrder the dependency-first load order of all effective modules
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
