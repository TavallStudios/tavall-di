package org.tavall.dependency.composition;

/**
 * Factory for hydrating dependency bundles from the DI metadata map.
 */
public interface IDependencyBundleFactory {

    <BUNDLE extends IDependencyBundle> BUNDLE createDependencies(Class<BUNDLE> bundleType);
}
