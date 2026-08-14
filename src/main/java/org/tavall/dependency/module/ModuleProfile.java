package org.tavall.dependency.module;

import org.tavall.identity.CustomEnum;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Named selection of runtime modules. A profile describes requested modules only; technical
 * required-dependency closure belongs to {@link ModuleProfileResolver}.
 */
public record ModuleProfile<M extends CustomEnum<M>, P extends CustomEnum<P>>(
        P id,
        Set<M> modules
) {
    public ModuleProfile {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(modules, "modules");
        modules = Collections.unmodifiableSet(new LinkedHashSet<>(modules));
        if (modules.contains(null)) {
            throw new IllegalArgumentException("Module profile cannot contain a null module id");
        }
    }

    @SafeVarargs
    public static <M extends CustomEnum<M>, P extends CustomEnum<P>> ModuleProfile<M, P> of(
            P id,
            M... modules
    ) {
        Objects.requireNonNull(modules, "modules");
        LinkedHashSet<M> requested = new LinkedHashSet<>();
        Collections.addAll(requested, modules);
        return new ModuleProfile<>(id, requested);
    }
}
