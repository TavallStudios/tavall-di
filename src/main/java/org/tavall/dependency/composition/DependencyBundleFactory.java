package org.tavall.dependency.composition;

import org.tavall.dependency.DependencyLoaderAccess;
import org.tavall.dependency.metadata.interfaces.IDependencyMetaData;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;

/**
 * Default bundle factory backed by the dependency metadata map.
 */
public final class DependencyBundleFactory implements IDependencyBundleFactory {
    private static final DependencyBundleFactory DEPENDENCY_BUNDLE_FACTORY = new DependencyBundleFactory();

    public static DependencyBundleFactory getDependencyBundleFactory() {
        return DEPENDENCY_BUNDLE_FACTORY;
    }

    private DependencyBundleFactory() {
    }

    @Override
    public <BUNDLE extends IDependencyBundle> BUNDLE createDependencies(Class<BUNDLE> bundleType) {
        if (bundleType == null) {
            throw new IllegalArgumentException("[DependencyBundleFactory] dependency bundle type is required");
        }
        if (!bundleType.isRecord()) {
            throw new IllegalStateException("[DependencyBundleFactory] dependency bundle type must be a record: "
                    + bundleType.getName());
        }

        RecordComponent[] recordComponents = bundleType.getRecordComponents();
        Object[] dependencyArguments = new Object[recordComponents.length];
        Class<?>[] constructorTypes = new Class<?>[recordComponents.length];

        for (int i = 0; i < recordComponents.length; i++) {
            RecordComponent recordComponent = recordComponents[i];
            Class<?> componentType = recordComponent.getType();
            constructorTypes[i] = componentType;

            IDependencyMetaData<?, ?> dependencyMetaData = DependencyLoaderAccess.findMetaData(componentType);
            if (dependencyMetaData == null) {
                throw new IllegalStateException("[DependencyBundleFactory] No dependency registered for bundle component "
                        + componentType.getName() + " in bundle " + bundleType.getName());
            }

            Object dependency = dependencyMetaData.getDependencyInterface();
            if (dependency == null) {
                dependency = dependencyMetaData.getDependencyInstance();
            }

            if (dependency == null) {
                throw new IllegalStateException("[DependencyBundleFactory] Registered metadata for component "
                        + componentType.getName() + " does not expose an instance for bundle " + bundleType.getName());
            }

            dependencyArguments[i] = dependency;
        }

        try {
            Constructor<BUNDLE> constructor = bundleType.getDeclaredConstructor(constructorTypes);
            constructor.setAccessible(true);
            return constructor.newInstance(dependencyArguments);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("[DependencyBundleFactory] Failed to create dependency bundle "
                    + bundleType.getName(), exception);
        }
    }
}
