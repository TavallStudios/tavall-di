package org.tavall.dependency.module;

import org.tavall.util.CustomEnum;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Resolved deployment module profile containing validated metadata, topological load order,
 * and effective active modules.
 *
 * @param <M> module identifier type extending canonical {@link CustomEnum}
 * @param <P> profile identifier type extending canonical {@link CustomEnum}
 * @param <R> runtime target identifier type extending canonical {@link CustomEnum}
 * @param schemaVersion schema version of the source deployment profile
 * @param profileId profile identity
 * @param runtimeTarget designated runtime target identity
 * @param configurationGeneration configuration generation index
 * @param requestedModules initially requested enabled modules
 * @param disabledModules explicit disabled modules
 * @param loadOrder dependency-first load order of active modules
 */
public record EffectiveDeploymentModuleProfile<M extends CustomEnum<M>, P extends CustomEnum<P>, R extends CustomEnum<R>>(
        long schemaVersion,
        P profileId,
        R runtimeTarget,
        long configurationGeneration,
        Set<M> requestedModules,
        Set<M> disabledModules,
        List<M> loadOrder
) {
    public EffectiveDeploymentModuleProfile {
        if (schemaVersion <= 0) {
            throw new IllegalArgumentException("Schema version must be positive: " + schemaVersion);
        }
        Objects.requireNonNull(profileId, "profileId");
        Objects.requireNonNull(runtimeTarget, "runtimeTarget");
        if (configurationGeneration < 0) {
            throw new IllegalArgumentException(
                    "Configuration generation must be non-negative: " + configurationGeneration
            );
        }
        Objects.requireNonNull(requestedModules, "requestedModules");
        Objects.requireNonNull(disabledModules, "disabledModules");
        Objects.requireNonNull(loadOrder, "loadOrder");

        requestedModules = Collections.unmodifiableSet(new LinkedHashSet<>(requestedModules));
        disabledModules = Collections.unmodifiableSet(new LinkedHashSet<>(disabledModules));
        loadOrder = List.copyOf(loadOrder);
    }

    /**
     * Returns the profile identity (alias for {@link #profileId()}).
     *
     * @return the profile identity
     */
    public P id() {
        return profileId;
    }

    /**
     * Returns an immutable set of resolved active modules.
     *
     * @return set of effective active modules
     */
    public Set<M> modules() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(loadOrder));
    }
}
