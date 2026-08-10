/*
 * TJVD License (TJ Valentine’s Discretionary License) — Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package org.tavall.dependency.injection.helpers;

import org.tavall.dependency.IDependencyInjectableInterface;
import org.tavall.dependency.annotations.DelegatesTo;
import org.tavall.dependency.annotations.DelegatesToInterface;
import org.tavall.dependency.injection.helpers.interfaces.IDependencyInjectorHelper;
import org.tavall.dependency.maps.DependencyMap;
import org.tavall.dependency.metadata.DependencyMetaData;
import org.tavall.dependency.metadata.DependencyMetaDataHelper;
import org.tavall.dependency.metadata.interfaces.IDependencyMetaData;
import org.tavall.dependency.metadata.interfaces.IDependencyMetaDataHelper;
import org.tavall.dependency.metadata.wrappers.DependencyInstance;
import org.tavall.dependency.metadata.wrappers.DependencyInterface;
import org.tavall.dependency.metadata.wrappers.interfaces.IDependencyInstance;
import org.tavall.dependency.metadata.wrappers.interfaces.IDependencyInterface;
import org.tavall.logging.Log;
import org.tavall.logging.style.LogColor;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;

/**
 * Scans packages for {@link DelegatesTo} concretes and registers their tokens.
 */
public class DependencyInjectorHelper<INTERFACE, INSTANCE>
        implements IDependencyInjectorHelper<INTERFACE, INSTANCE> {
    private static final Set<BootstrapKey> BOOTSTRAPPED = ConcurrentHashMap.newKeySet();
    private static final ConcurrentHashMap<BootstrapKey, Set<Class<?>>> REGISTERED_BINDINGS =
            new ConcurrentHashMap<>();

    private final Set<Class<?>> loadedInterfaces = ConcurrentHashMap.newKeySet();
    private final Set<Class<?>> loadedConcretes = ConcurrentHashMap.newKeySet();
    @SuppressWarnings("rawtypes")
    private final IDependencyMetaDataHelper dependencyMetaDataHelper = new DependencyMetaDataHelper();

    String BASE_PACKAGE = "org.tavall";

    public void setBasePackage(String basePackage) {
        if (basePackage == null || basePackage.isBlank()) {
            throw new IllegalArgumentException("basePackage is required");
        }
        BASE_PACKAGE = basePackage;
    }

    @Override
    public void setupDISystem() {
        setupDISystem(getClass().getClassLoader());
    }

    @Override
    public void setupDISystem(ClassLoader loader) {
        ClassLoader resolvedLoader = resolveClassLoader(loader);
        BootstrapKey bootstrapKey = bootstrapKey(resolvedLoader);
        if (BOOTSTRAPPED.contains(bootstrapKey) && isBootstrapCurrent(bootstrapKey)) {
            Log.info("[DI] Bootstrap already completed for " + BASE_PACKAGE + " using "
                    + resolvedLoader + ", skipping");
            return;
        }

        BOOTSTRAPPED.add(bootstrapKey);
        try {
            runBootstrap(bootstrapKey, resolvedLoader, false);
        } catch (RuntimeException | Error failure) {
            BOOTSTRAPPED.remove(bootstrapKey);
            REGISTERED_BINDINGS.remove(bootstrapKey);
            throw failure;
        }
    }

    @Override
    public void reloadDISystem() {
        reloadDISystem(getClass().getClassLoader());
    }

    @Override
    public void reloadDISystem(ClassLoader loader) {
        ClassLoader resolvedLoader = resolveClassLoader(loader);
        BootstrapKey bootstrapKey = bootstrapKey(resolvedLoader);
        BOOTSTRAPPED.remove(bootstrapKey);
        REGISTERED_BINDINGS.remove(bootstrapKey);
        runBootstrap(bootstrapKey, resolvedLoader, true);
        BOOTSTRAPPED.add(bootstrapKey);
    }

    private void runBootstrap(BootstrapKey bootstrapKey, ClassLoader loader, boolean reload) {
        Log.warn(reload
                ? "[DI] ===== DI System Reload Started ====="
                : "[DI] ===== DI System Initialization Started =====");

        loadedInterfaces.clear();
        loadedConcretes.clear();
        scanPackage(BASE_PACKAGE, loader);
        if (reload) {
            unregisterLoadedBindings();
        }
        registerDependenciesViaAnnotation();
        REGISTERED_BINDINGS.put(bootstrapKey, registeredBindingKeys());
        flushPendingCacheRegistryMetaData(loader);

        Log.warn(reload
                ? "[DI] ===== DI System Reload Ended ====="
                : "[DI] ===== DI System Initialization Ended =====");
    }

    @Override
    public void setupDISystem(Class<?> type) {
        if (type == null) {
            throw new IllegalArgumentException("type is required");
        }
        setupDISystem(type.getClassLoader());
    }

    @Override
    public void setupDISystem(Object entryPoint) {
        if (entryPoint == null) {
            throw new IllegalArgumentException("entryPoint is required");
        }
        setupDISystem(entryPoint.getClass());
    }

    private ClassLoader resolveClassLoader(ClassLoader loader) {
        if (loader != null) {
            return loader;
        }
        ClassLoader helperLoader = getClass().getClassLoader();
        if (helperLoader != null) {
            return helperLoader;
        }
        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        if (contextLoader != null) {
            return contextLoader;
        }
        throw new IllegalStateException("[DI] Unable to resolve a class loader for DI scanning");
    }

    private BootstrapKey bootstrapKey(ClassLoader loader) {
        return new BootstrapKey(BASE_PACKAGE, System.identityHashCode(loader));
    }

    private boolean isBootstrapCurrent(BootstrapKey bootstrapKey) {
        Set<Class<?>> expectedBindings = REGISTERED_BINDINGS.get(bootstrapKey);
        if (expectedBindings == null || expectedBindings.isEmpty()) {
            return false;
        }
        return expectedBindings.stream()
                .allMatch(DependencyMap.getDependencyMap()::isInstanceRegistered);
    }

    /**
     * Creates one metadata object per annotated concrete and maps every token to it.
     */
    @SuppressWarnings({"rawtypes", "unchecked", "deprecation"})
    public void registerDependenciesViaAnnotation() {
        Log.warn("[DI-Helper] ====== Beginning annotation-driven DI registration ======");

        int registeredBindings = 0;
        Set<IDependencyMetaData<?, ?>> registeredMetaData = new LinkedHashSet<>();
        Set<RegisteredBinding> seenBindings = new LinkedHashSet<>();

        for (Class<?> concreteType : loadedConcretes) {
            Set<Class<?>> delegatedTypes = validDelegatedTypes(concreteType);
            if (delegatedTypes.isEmpty()) {
                continue;
            }

            Class<?> primaryType = delegatedTypes.iterator().next();
            IDependencyInterface wrappedType = new DependencyInterface(primaryType);
            IDependencyInstance wrappedConcrete = new DependencyInstance(concreteType);
            IDependencyMetaData dependencyMetaData = new DependencyMetaData();
            dependencyMetaDataHelper.populateMetaData(
                    dependencyMetaData,
                    wrappedType,
                    wrappedConcrete);
            registeredMetaData.add(dependencyMetaData);

            for (Class<?> delegatedType : delegatedTypes) {
                RegisteredBinding binding = new RegisteredBinding(delegatedType, concreteType);
                if (!seenBindings.add(binding)) {
                    continue;
                }
                DependencyMap.getDependencyMap().registerDependency(delegatedType, dependencyMetaData);
                registeredBindings++;
            }
        }

        registeredMetaData.forEach(IDependencyMetaData::initializeDependencyInstance);
        Log.warn("[DI-Helper] Finished annotation DI registration, registered dependency bindings: "
                + registeredBindings);
    }

    private void unregisterLoadedBindings() {
        for (Class<?> concreteType : loadedConcretes) {
            validDelegatedTypes(concreteType)
                    .forEach(DependencyMap.getDependencyMap()::removeDependency);
        }
    }

    private Set<Class<?>> registeredBindingKeys() {
        Set<Class<?>> bindings = new LinkedHashSet<>();
        loadedConcretes.forEach(concreteType -> bindings.addAll(validDelegatedTypes(concreteType)));
        return bindings;
    }

    private Set<Class<?>> validDelegatedTypes(Class<?> concreteType) {
        Set<Class<?>> delegatedTypes = resolveDelegatedTypes(concreteType);
        for (Class<?> delegatedType : delegatedTypes) {
            if (!delegatedType.isAssignableFrom(concreteType)) {
                throw new IllegalArgumentException("@DelegatesTo token " + delegatedType.getName()
                        + " is not assignable from " + concreteType.getName());
            }
        }
        return delegatedTypes;
    }

    @SuppressWarnings("deprecation")
    private Set<Class<?>> resolveDelegatedTypes(Class<?> concreteType) {
        Set<Class<?>> delegatedTypes = new LinkedHashSet<>();
        DelegatesTo delegatesTo = concreteType.getAnnotation(DelegatesTo.class);
        DelegatesToInterface legacyDelegatesTo = concreteType.getAnnotation(DelegatesToInterface.class);

        if (delegatesTo != null && legacyDelegatesTo != null) {
            throw new IllegalStateException("Dependency concrete cannot declare both @DelegatesTo and "
                    + "@DelegatesToInterface: " + concreteType.getName());
        }
        if (delegatesTo != null) {
            delegatedTypes.add(concreteType);
            Arrays.stream(delegatesTo.value())
                    .forEach(type -> addDelegatedType(delegatedTypes, type));
            return delegatedTypes;
        }
        if (legacyDelegatesTo == null) {
            return delegatedTypes;
        }

        delegatedTypes.add(concreteType);
        addDelegatedType(delegatedTypes, legacyDelegatesTo.value());
        addDelegatedType(delegatedTypes, legacyDelegatesTo.getLinkedInterface());
        Arrays.stream(legacyDelegatesTo.getLinkedInterfaces())
                .forEach(type -> addDelegatedType(delegatedTypes, type));
        return delegatedTypes;
    }

    private boolean hasDelegationAnnotation(Class<?> type) {
        return type.isAnnotationPresent(DelegatesTo.class)
                || type.isAnnotationPresent(DelegatesToInterface.class);
    }

    private void addDelegatedType(Set<Class<?>> delegatedTypes, Class<?> delegatedType) {
        if (delegatedType != null && delegatedType != Void.class) {
            delegatedTypes.add(delegatedType);
        }
    }

    private void flushPendingCacheRegistryMetaData(ClassLoader loader) {
        try {
            Class<?> registryClass = Class.forName(
                    "org.tavall.abstractcache.semantic.stats.CacheStatsRegistry",
                    true,
                    loader);
            Object registry = registryClass.getMethod("getInstance").invoke(null);
            registryClass.getMethod("flushPendingCacheRegistryMetaData").invoke(registry);
        } catch (ClassNotFoundException ignored) {
            // Tavall Cache is optional for DI bootstrap.
        } catch (ReflectiveOperationException exception) {
            Log.exception(exception);
        }
    }

    public boolean doesConcreteImplementInterface(Class<?> concrete, Class<?> targetInterface) {
        if (concrete == null || targetInterface == null) {
            return false;
        }
        return targetInterface.isAssignableFrom(concrete);
    }

    public boolean isClassLoadable(Class<?> dependencyClass) {
        if (dependencyClass == null) {
            return false;
        }
        try {
            Class.forName(dependencyClass.getName(), false, dependencyClass.getClassLoader());
            dependencyClass.getDeclaredMethods();
            return true;
        } catch (ClassNotFoundException | NoClassDefFoundError failure) {
            return false;
        }
    }

    public boolean isClassLoadable(IDependencyInterface<INTERFACE> dependencyInterface) {
        return dependencyInterface != null && isClassLoadable(dependencyInterface.getClass());
    }

    public boolean isClassLoadable(IDependencyInstance<INSTANCE> dependencyInstance) {
        return dependencyInstance != null && isClassLoadable(dependencyInstance.getClass());
    }

    private void processDependencyClasses(String className, ClassLoader loader) {
        try {
            Class<?> rawClass = Class.forName(className, false, loader);
            if (!isClassLoadable(rawClass)) {
                return;
            }

            if (rawClass.isInterface()
                    && IDependencyInjectableInterface.class.isAssignableFrom(rawClass)) {
                loadedInterfaces.add(rawClass);
            } else if (isConcrete(rawClass) && hasDelegationAnnotation(rawClass)) {
                loadedConcretes.add(rawClass);
            }
        } catch (Throwable ignored) {
            // Optional platform classes may be absent in a given runtime.
        }
    }

    public void scanDirectory(String pkg, File directory, ClassLoader loader) {
        File[] files = directory == null ? null : directory.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(pkg + (pkg.isEmpty() ? "" : ".") + file.getName(), file, loader);
            } else if (file.getName().endsWith(".class")) {
                String className = pkg + (pkg.isEmpty() ? "" : ".")
                        + file.getName().replace(".class", "");
                processDependencyClasses(className, loader);
            }
        }
    }

    public void scanJar(File jarFile, String basePackage, ClassLoader loader) {
        if (jarFile == null || !jarFile.exists()) {
            return;
        }
        String prefix = basePackage.replace('.', '/') + "/";
        try (JarFile jar = new JarFile(jarFile)) {
            jar.stream()
                    .filter(entry -> !entry.isDirectory())
                    .filter(entry -> entry.getName().endsWith(".class"))
                    .filter(entry -> entry.getName().startsWith(prefix))
                    .forEach(entry -> processDependencyClasses(
                            entry.getName().replace('/', '.').replace(".class", ""),
                            loader));
        } catch (Throwable failure) {
            Log.error("[DI-Scan] Failed to scan JAR " + jarFile.getName());
            Log.exception(failure);
        }
    }

    public boolean isConcrete(Class<?> type) {
        if (type == null) {
            return false;
        }
        int modifiers = type.getModifiers();
        return !type.isInterface()
                && !Modifier.isAbstract(modifiers)
                && !type.isEnum()
                && !type.isAnnotation();
    }

    public void scanPackage(String basePackage, ClassLoader loader) {
        if (basePackage == null || basePackage.isBlank()) {
            return;
        }

        String path = basePackage.replace('.', '/');
        try {
            Enumeration<URL> resources = loader.getResources(path);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                switch (url.getProtocol()) {
                    case "file" -> scanDirectory(basePackage, new File(url.toURI()), loader);
                    case "jar", "zip", "wsjar", "war", "zipfs", "vfs", "vfszip",
                            "bundleresource", "bundle" -> {
                        File jarFile = extractJarFile(url);
                        if (jarFile != null) {
                            scanJar(jarFile, basePackage, loader);
                        }
                    }
                    case "jrt" -> scanModulePath(basePackage);
                    default -> Log.warn("[DI-Scan] Unsupported protocol: " + url.getProtocol() + " @ " + url);
                }
            }
        } catch (Throwable failure) {
            Log.error("[DI-Scan] Failed to scan package " + basePackage);
            Log.exception(failure);
        }

        Log.info("[DI-Scan] " + LogColor.GRAY + "Loaded " + loadedInterfaces.size()
                + " compatibility interfaces and " + loadedConcretes.size()
                + " annotated concretes for package " + basePackage);
    }

    private void scanModulePath(String basePackage) {
        try {
            for (Module module : ModuleLayer.boot().modules()) {
                if (!module.isNamed() || !module.getPackages().contains(basePackage)) {
                    continue;
                }
                try (InputStream ignored = module.getResourceAsStream(basePackage.replace('.', '/') + "/")) {
                    Log.warn("[DI-Scan] JRT scanning is not fully implemented for: " + basePackage);
                }
            }
        } catch (Throwable failure) {
            Log.error("[DI-Scan] Failed to scan JRT module path for " + basePackage);
            Log.exception(failure);
        }
    }

    private File extractJarFile(URL url) {
        try {
            String external = url.toExternalForm();
            if (external.startsWith("jar:")) {
                external = external.substring(4);
            }
            int separatorIndex = external.indexOf("!/");
            if (separatorIndex != -1) {
                external = external.substring(0, separatorIndex);
            }
            if (external.startsWith("file:")) {
                external = external.substring(5);
            }
            return new File(URLDecoder.decode(external, StandardCharsets.UTF_8));
        } catch (Throwable failure) {
            Log.exception(failure);
            return null;
        }
    }

    private record BootstrapKey(String basePackage, int classLoaderIdentity) {
    }

    private record RegisteredBinding(Class<?> dependencyType, Class<?> concreteType) {
    }
}
