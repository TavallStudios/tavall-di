package org.tavall.dependency.module;

import org.tavall.dependency.IDependencyInjectableInterface;
import org.tavall.util.CustomEnum;

import java.util.Collection;
import java.util.function.Function;

/**
 * Contract for expanding requested module profiles into effective dependency-first module load orders.
 *
 * <p><b>Architectural Responsibility:</b> Interface-first, DI-managed resolver that determines
 * effective module composition and topological load ordering. Rejects enabled/disabled conflicts,
 * disabled required dependencies (without silently enabling them), missing/unavailable modules,
 * and dependency cycles.</p>
 *
 * <p><b>Fences:</b> Profile resolution does not pretend to implement durable persistence, runtime
 * process lifecycle, or dynamic enable/disable transitions; those belong to the module registry
 * and runtime host boundaries.</p>
 *
 * @param <M> module identifier type extending canonical {@link CustomEnum}
 * @param <P> profile identifier type extending canonical {@link CustomEnum}
 */
public interface IModuleProfileResolver<M extends CustomEnum<M>, P extends CustomEnum<P>>
        extends IDependencyInjectableInterface {

    /**
     * Resolves a {@link ModuleProfile} into its dependency-first {@link EffectiveModuleProfile}.
     *
     * @param profile requested module profile
     * @param availableModules all available modules in the catalog
     * @param requiredDependencies function mapping each module to its technical required dependencies
     * @return effective module profile with topological load order
     */
    EffectiveModuleProfile<M, P> resolve(
            ModuleProfile<M, P> profile,
            Collection<M> availableModules,
            Function<? super M, ? extends Collection<M>> requiredDependencies
    );

    /**
     * Resolves a {@link DeploymentModuleProfile} into its dependency-first
     * {@link EffectiveDeploymentModuleProfile}.
     *
     * <p>Rejects enabled/disabled conflicts and fails closed if any required dependency of an
     * enabled module is explicitly disabled, missing from available modules, or part of a cycle.</p>
     *
     * @param <R> runtime target identifier type
     * @param profile deployment module profile
     * @param availableModules all available modules in the catalog
     * @param requiredDependencies function mapping each module to its technical required dependencies
     * @return effective deployment module profile with topological load order
     */
    <R extends CustomEnum<R>> EffectiveDeploymentModuleProfile<M, P, R> resolve(
            DeploymentModuleProfile<M, P, R> profile,
            Collection<M> availableModules,
            Function<? super M, ? extends Collection<M>> requiredDependencies
    );
}
