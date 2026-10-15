package org.tavall.dependency;

import java.util.Objects;
import org.tavall.dependency.maps.interfaces.IDependencyMap;

/**
 * Framework-owned binding to one dependency generation; ordinary consumers only declare typed access.
 * <p>
 * <b>Architectural Role:</b> Canonical DI infrastructure base for managed generation-owned
 * dependency consumers. Binds an instance to its owning {@link IDependencyMap} generation prior to
 * dependency initialization, and fences retained references when the generation is unbound.
 * <p>
 * <b>Lifecycle &amp; Invariants:</b> An instance belongs to at most one live generation. A retired
 * instance cannot be rebound or resurrected by subsequent registration calls. A replacement
 * generation requires a fresh managed instance. When unbound through map replacement, removal,
 * or clearing, retained handles fail closed.
 * <p>
 * <b>Concurrency:</b> Fully thread-safe. Generation binding transitions are guarded by an internal
 * monitor {@code bindingLock}, preventing rebind races across worker threads without exposing
 * locking monitors to consumers.
 */
public abstract class GenerationOwnedDependencyAccess implements IDependencyAccess {
    private volatile IDependencyMap owner;
    private volatile boolean retired;
    private final Object bindingLock = new Object();

    /**
     * Binds this instance to an authoritative dependency generation map.
     * <p>
     * <b>Invariants:</b> DI infrastructure binds before dependency initialization. An instance
     * cannot belong to two live generations, cannot rebind to the same generation as a replacement,
     * and cannot be rebound once retired.
     *
     * @param dependencyMap the owning dependency map generation, non-null
     * @throws NullPointerException if {@code dependencyMap} is null
     * @throws IllegalStateException if this instance is retired, or is already bound to a generation
     */
    public final void bindDependencyGeneration(IDependencyMap dependencyMap) {
        synchronized (bindingLock) {
            Objects.requireNonNull(dependencyMap, "dependencyMap");
            if (retired) {
                throw new IllegalStateException("Retired dependency generation cannot be rebound");
            }
            if (owner != null) {
                throw new IllegalStateException(owner == dependencyMap
                        ? "Dependency is already bound to this generation; replacement requires a fresh instance"
                        : "Dependency already belongs to another generation");
            }
            owner = dependencyMap;
        }
    }

    /**
     * Detaches this instance from its owning dependency generation.
     * <p>
     * <b>Invariants:</b> Only the current owning generation map may unbind the instance. Borrowed
     * references from other maps (such as parent references) are ignored without mutating binding
     * state. Once successfully unbound, the instance is permanently retired and retained references
     * fail closed.
     *
     * @param dependencyMap the dependency map attempting to unbind this instance
     */
    public final void unbindDependencyGeneration(IDependencyMap dependencyMap) {
        synchronized (bindingLock) {
            if (owner != dependencyMap) return;
            owner = null;
            retired = true;
        }
        onDependencyGenerationUnbound();
    }

    /** Infrastructure verifies parent references before permitting mutation through an alias. */
    public final boolean isDependencyGenerationOwnedBy(IDependencyMap dependencyMap) {
        return owner == dependencyMap;
    }

    /**
     * Checks if this instance has been permanently retired from dependency generations.
     *
     * @return true if this instance was unbound and retired, false otherwise
     */
    public final boolean isDependencyGenerationRetired() {
        return retired;
    }

    @Override
    public final IDependencyMap getDependencyMap() {
        IDependencyMap current = owner;
        if (current == null) {
            throw new IllegalStateException("Dependency generation is not bound or has been unloaded");
        }
        return current;
    }

    protected final void requireDependencyGenerationBound() {
        getDependencyMap();
    }

    /** Nonblocking state invalidation only; teardown belongs to the runtime and must precede map removal.
     * This hook must not perform dependency lookup or mutate a replacement. */
    protected void onDependencyGenerationUnbound() { }
}
