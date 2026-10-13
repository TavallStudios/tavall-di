package org.tavall.dependency;

import java.util.Objects;
import org.tavall.dependency.maps.interfaces.IDependencyMap;

/** Framework-owned binding to one dependency generation; ordinary consumers only declare typed access. */
public abstract class GenerationOwnedDependencyAccess implements IDependencyAccess {
    private volatile IDependencyMap owner;
    private final Object bindingLock = new Object();

    /** DI infrastructure binds before initialization; an instance cannot belong to two live generations. */
    public final void bindDependencyGeneration(IDependencyMap dependencyMap) {
        synchronized (bindingLock) {
        Objects.requireNonNull(dependencyMap, "dependencyMap");
        if (owner != null && owner != dependencyMap) {
            throw new IllegalStateException("Dependency already belongs to another generation");
        }
        owner = dependencyMap;
        }
    }

    /** DI infrastructure detaches after replacement/removal; retained references fail closed. */
    public final void unbindDependencyGeneration(IDependencyMap dependencyMap) {
        synchronized (bindingLock) {
            if (owner != dependencyMap) return;
            owner = null;
        }
        onDependencyGenerationUnbound();
    }

    /** Infrastructure verifies parent references before permitting mutation through an alias. */
    public final boolean isDependencyGenerationOwnedBy(IDependencyMap dependencyMap) { return owner == dependencyMap; }

    @Override
    public final IDependencyMap getDependencyMap() {
        IDependencyMap current = owner;
        if (current == null) throw new IllegalStateException("Dependency generation is not bound or has been unloaded");
        return current;
    }

    protected final void requireDependencyGenerationBound() { getDependencyMap(); }

    /** Nonblocking state invalidation only; teardown belongs to the runtime and must precede map removal.
     * This hook must not perform dependency lookup or mutate a replacement. */
    protected void onDependencyGenerationUnbound() { }
}
