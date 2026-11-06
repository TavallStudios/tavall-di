package org.tavall.dependency.module;

import org.tavall.util.CustomEnum;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable typed specification of a per-deployment module profile.
 *
 * <p>Defines the desired module composition for a designated runtime target and configuration
 * generation. Separates deployment profile identity from immutable artifact bytes, physical
 * placement, and observed runtime activation states.</p>
 *
 * <p>A deployment profile declares explicitly enabled modules and explicitly disabled modules.
 * Conflict between enabled and disabled modules is rejected upon construction and during
 * resolution.</p>
 *
 * @param <M> module identifier type extending canonical {@link CustomEnum}
 * @param <P> profile identifier type extending canonical {@link CustomEnum}
 * @param <R> runtime target identifier type extending canonical {@link CustomEnum}
 * @param schemaVersion positive schema version of this deployment profile definition
 * @param profileId profile identity
 * @param runtimeTarget designated runtime target identity
 * @param configurationGeneration configuration generation index (non-negative)
 * @param enabledModules explicit set of modules desired enabled
 * @param disabledModules explicit set of modules desired disabled
 */
public record DeploymentModuleProfile<M extends CustomEnum<M>, P extends CustomEnum<P>, R extends CustomEnum<R>>(
        long schemaVersion,
        P profileId,
        R runtimeTarget,
        long configurationGeneration,
        Set<M> enabledModules,
        Set<M> disabledModules
) {
    public static final long DEFAULT_SCHEMA_VERSION = 1L;

    public DeploymentModuleProfile {
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
        Objects.requireNonNull(enabledModules, "enabledModules");
        Objects.requireNonNull(disabledModules, "disabledModules");

        enabledModules = Collections.unmodifiableSet(new LinkedHashSet<>(enabledModules));
        disabledModules = Collections.unmodifiableSet(new LinkedHashSet<>(disabledModules));

        if (enabledModules.contains(null)) {
            throw new IllegalArgumentException("Enabled modules cannot contain null");
        }
        if (disabledModules.contains(null)) {
            throw new IllegalArgumentException("Disabled modules cannot contain null");
        }

        for (M enabled : enabledModules) {
            if (disabledModules.contains(enabled)) {
                throw new IllegalArgumentException("Module cannot be both enabled and disabled: " + enabled);
            }
        }
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
     * Returns the enabled modules (alias for {@link #enabledModules()}).
     *
     * @return the set of enabled modules
     */
    public Set<M> modules() {
        return enabledModules;
    }

    public static final Set<Long> SUPPORTED_SCHEMA_VERSIONS = Set.of(DEFAULT_SCHEMA_VERSION);

    /**
     * Checks whether the specified schema version is supported by the deployment profile model.
     *
     * @param schemaVersion the schema version to check
     * @return true if supported, false otherwise
     */
    public static boolean isSupportedSchemaVersion(long schemaVersion) {
        return SUPPORTED_SCHEMA_VERSIONS.contains(schemaVersion);
    }

    public static <M extends CustomEnum<M>, P extends CustomEnum<P>, R extends CustomEnum<R>>
    DeploymentModuleProfile<M, P, R> of(
            long schemaVersion,
            P profileId,
            R runtimeTarget,
            long configurationGeneration,
            Set<M> enabledModules,
            Set<M> disabledModules
    ) {
        return new DeploymentModuleProfile<>(
                schemaVersion,
                profileId,
                runtimeTarget,
                configurationGeneration,
                enabledModules,
                disabledModules
        );
    }

    public static <M extends CustomEnum<M>, P extends CustomEnum<P>, R extends CustomEnum<R>>
    DeploymentModuleProfile<M, P, R> of(
            P profileId,
            R runtimeTarget,
            Set<M> enabledModules,
            Set<M> disabledModules
    ) {
        return new DeploymentModuleProfile<>(
                DEFAULT_SCHEMA_VERSION,
                profileId,
                runtimeTarget,
                0L,
                enabledModules,
                disabledModules
        );
    }

    public static <M extends CustomEnum<M>, P extends CustomEnum<P>, R extends CustomEnum<R>>
    Builder<M, P, R> builder() {
        return new Builder<>();
    }

    public static final class Builder<M extends CustomEnum<M>, P extends CustomEnum<P>, R extends CustomEnum<R>> {
        private long schemaVersion = DEFAULT_SCHEMA_VERSION;
        private P profileId;
        private R runtimeTarget;
        private long configurationGeneration = 0L;
        private final Set<M> enabledModules = new LinkedHashSet<>();
        private final Set<M> disabledModules = new LinkedHashSet<>();

        public Builder<M, P, R> schemaVersion(long schemaVersion) {
            this.schemaVersion = schemaVersion;
            return this;
        }

        public Builder<M, P, R> profileId(P profileId) {
            this.profileId = profileId;
            return this;
        }

        public Builder<M, P, R> runtimeTarget(R runtimeTarget) {
            this.runtimeTarget = runtimeTarget;
            return this;
        }

        public Builder<M, P, R> configurationGeneration(long configurationGeneration) {
            this.configurationGeneration = configurationGeneration;
            return this;
        }

        @SafeVarargs
        public final Builder<M, P, R> enable(M... modules) {
            if (modules != null) {
                Collections.addAll(this.enabledModules, modules);
            }
            return this;
        }

        public Builder<M, P, R> enableAll(Iterable<? extends M> modules) {
            if (modules != null) {
                for (M m : modules) {
                    this.enabledModules.add(m);
                }
            }
            return this;
        }

        @SafeVarargs
        public final Builder<M, P, R> disable(M... modules) {
            if (modules != null) {
                Collections.addAll(this.disabledModules, modules);
            }
            return this;
        }

        public Builder<M, P, R> disableAll(Iterable<? extends M> modules) {
            if (modules != null) {
                for (M m : modules) {
                    this.disabledModules.add(m);
                }
            }
            return this;
        }

        public DeploymentModuleProfile<M, P, R> build() {
            return new DeploymentModuleProfile<>(
                    schemaVersion,
                    profileId,
                    runtimeTarget,
                    configurationGeneration,
                    enabledModules,
                    disabledModules
            );
        }
    }
}
