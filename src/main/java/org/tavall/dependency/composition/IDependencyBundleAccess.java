package org.tavall.dependency.composition;

import org.tavall.dependency.internal.DependencyTypeResult;
import org.tavall.dependency.internal.DependencyTypeResolver;

/**
 * Access contract for record-based dependency bundles.
 *
 * @param <BUNDLE> the bundle record type
 */
public interface IDependencyBundleAccess<BUNDLE extends IDependencyBundle> {

    @SuppressWarnings("unchecked")
    default Class<? extends BUNDLE> getDependencyBundleTypeParam() {
        DependencyTypeResult typeResult =
                DependencyTypeResolver.resolveTypeParameters(getClass(), IDependencyBundleAccess.class);
        Class<?> bundleType = typeResult.getTypeArgument(0);
        if (bundleType == null) {
            throw new IllegalStateException("[IDependencyBundleAccess] Unable to resolve dependency bundle type parameter for "
                    + getClass().getName());
        }
        if (!IDependencyBundle.class.isAssignableFrom(bundleType)) {
            throw new IllegalStateException("[IDependencyBundleAccess] Resolved dependency bundle type is invalid: "
                    + bundleType.getName());
        }
        return (Class<? extends BUNDLE>) bundleType;
    }

    default BUNDLE getDependencies() {
        return DependencyBundleFactory.getDependencyBundleFactory().createDependencies(getDependencyBundleTypeParam());
    }

    default BUNDLE dependencies() {
        return getDependencies();
    }
}
