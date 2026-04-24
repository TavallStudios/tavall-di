/*
 * TJVD License (TJ Valentine’s Discretionary License) — Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package com.tjxjnoobie.api.dependency.injection.helpers;

import com.tjxjnoobie.api.dependency.DependencyLoaderAccess;
import com.tjxjnoobie.api.dependency.IDependencyInjectableConcrete;
import com.tjxjnoobie.api.dependency.IDependencyInjectableInterface;
import com.tjxjnoobie.api.dependency.annotations.DelegatesToInterface;
import com.tjxjnoobie.api.dependency.injection.helpers.interfaces.IDependencyInjectorHelper;
import com.tjxjnoobie.api.dependency.maps.DependencyMap;
import com.tjxjnoobie.api.dependency.metadata.DependencyMetaData;
import com.tjxjnoobie.api.dependency.metadata.DependencyMetaDataHelper;
import com.tjxjnoobie.api.dependency.metadata.interfaces.IDependencyMetaData;
import com.tjxjnoobie.api.dependency.metadata.interfaces.IDependencyMetaDataHelper;
import com.tjxjnoobie.api.dependency.metadata.wrappers.DependencyInstance;
import com.tjxjnoobie.api.dependency.metadata.wrappers.DependencyInterface;
import com.tjxjnoobie.api.dependency.metadata.wrappers.interfaces.IDependencyInstance;
import com.tjxjnoobie.api.dependency.metadata.wrappers.interfaces.IDependencyInterface;
import com.tjxjnoobie.api.platform.global.console.Log;
import com.tjxjnoobie.api.platform.global.console.style.LogColor;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;

/**
 * Scans packages for annotated DI concretes and registers them against interface tokens.
 *
 * @param <INTERFACE> the injectable interface token type
 * @param <INSTANCE> the injectable concrete instance type
 */
public class DependencyInjectorHelper<
        INTERFACE extends IDependencyInjectableInterface,
        INSTANCE extends IDependencyInjectableConcrete>
        implements IDependencyInjectorHelper<INTERFACE, INSTANCE> {
    private static final Set<BootstrapKey> BOOTSTRAPPED = ConcurrentHashMap.newKeySet();
    private static final ConcurrentHashMap<BootstrapKey, Set<Class<?>>> REGISTERED_BINDINGS = new ConcurrentHashMap<>();

    private final Set<Class<? extends INTERFACE>> loadedInterfaces = ConcurrentHashMap.newKeySet();
    private final Set<Class<? extends INSTANCE>> loadedConcretes = ConcurrentHashMap.newKeySet();
    private final IDependencyMetaDataHelper<INTERFACE, INSTANCE> dependencyMetaDataHelper =
            new DependencyMetaDataHelper<>();

    String BASE_PACKAGE = "com.tjxjnoobie";

    /**
     * Replaces the package prefix scanned by this helper.
     *
     * @param basePackage the package prefix to scan
     */
    public void setBasePackage(String basePackage) {
        if (basePackage == null || basePackage.isBlank()) {
            throw new IllegalArgumentException("basePackage is required");
        }
        this.BASE_PACKAGE = basePackage;
    }

    /**
     * Scans and registers dependencies using the helper's own class loader.
     */
    @Override
    public void setupDISystem() {
        setupDISystem(getClass().getClassLoader());
    }

    /**
     * Scans and registers dependencies using the supplied class loader.
     *
     * @param loader the class loader used for reflective package scanning
     */
    @Override
    public void setupDISystem(ClassLoader loader) {
        ClassLoader resolvedLoader = resolveClassLoader(loader);
        BootstrapKey bootstrapKey = bootstrapKey(resolvedLoader);

        if (BOOTSTRAPPED.contains(bootstrapKey) && isBootstrapCurrent(bootstrapKey)) {
            Log.info("[DI] Bootstrap already completed for " + BASE_PACKAGE + " using " + resolvedLoader + ", skipping");
            return;
        }

        BOOTSTRAPPED.add(bootstrapKey);

        try {
            runBootstrap(bootstrapKey, resolvedLoader, false);
        } catch (RuntimeException | Error e) {
            BOOTSTRAPPED.remove(bootstrapKey);
            REGISTERED_BINDINGS.remove(bootstrapKey);
            throw e;
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

    private void runBootstrap(BootstrapKey bootstrapKey, ClassLoader resolvedLoader, boolean reload) {
        if (reload) {
            Log.warn("[DI] ===== DI System Reload Started =====");
        } else {
            Log.warn("[DI] ===== DI System Initialization Started =====");
        }

        Log.info(" %YELLOW% [DI] --- Phase 1: Class Scanning ");
        loadedInterfaces.clear();
        loadedConcretes.clear();
        scanPackage(BASE_PACKAGE, resolvedLoader);
        if (reload) {
            Log.info("[DI] --- Phase 1b: Removing Existing Package Bindings ");
            unregisterLoadedBindings();
        }
        Log.info("[DI] --- Phase 2: Annotation Scanning & Map Registration ");
        registerDependenciesViaAnnotation();
        REGISTERED_BINDINGS.put(bootstrapKey, registeredInterfaceKeys());

        if (reload) {
            Log.warn("[DI] ===== DI System Reload Ended =====");
        } else {
            Log.warn("[DI] ===== DI System Initialization Ended =====");
        }
    }

    /**
     * Scans and registers dependencies using the supplied type's class loader.
     *
     * @param type the type whose class loader should be used for scanning
     */
    @Override
    public void setupDISystem(Class<?> type) {
        if (type == null) {
            throw new IllegalArgumentException("type is required");
        }
        setupDISystem(type.getClassLoader());
    }

    /**
     * Scans and registers dependencies using the supplied entrypoint object's class loader.
     *
     * @param entryPoint the object whose class loader should be used for scanning
     */
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

        for (Class<?> binding : expectedBindings) {
            if (!DependencyMap.getDependencyMap().isInstanceRegistered(binding)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Registers annotated concrete classes against their declared interface tokens.
     */
    @SuppressWarnings("unchecked")
    public void registerDependenciesViaAnnotation() {

        Log.warn("[DI-Helper] ====== Beginning annotation-driven DI registration ======");

        int skipped = 0;
        int registeredBindings = 0;
        Set<IDependencyMetaData<INTERFACE, INSTANCE>> registeredMetaData = new LinkedHashSet<>();
        Set<RegisteredBinding> seenBindings = new LinkedHashSet<>();

        for (Class<? extends INSTANCE> rawScannedConcrete : loadedConcretes) {
            DelegatesToInterface concreteAnnotation = rawScannedConcrete.getAnnotation(DelegatesToInterface.class);
            if (concreteAnnotation == null) {
                continue;
            }
            if (!IDependencyInjectableConcrete.class.isAssignableFrom(rawScannedConcrete)) {
                Log.warn("[DI-Helper] " + rawScannedConcrete.getSimpleName()
                        + " is annotated for DI but does not implement IDependencyInjectableConcrete, skipping...");
                skipped++;
                continue;
            }

            Set<Class<? extends INTERFACE>> validInterfaces = new LinkedHashSet<>();
            for (Class<?> linkedInterface : resolveLinkedInterfaces(concreteAnnotation)) {
                if (!linkedInterface.isInterface()) {
                    Log.warn("[DI-Helper] " + linkedInterface.getName() + " is not an interface token, skipping...");
                    continue;
                }
                if (!IDependencyInjectableInterface.class.isAssignableFrom(linkedInterface)) {
                    Log.warn("[DI-Helper] " + linkedInterface.getName()
                            + " does not implement IDependencyInjectableInterface, skipping...");
                    continue;
                }
                if (!doesConcreteImplementInterface(rawScannedConcrete, linkedInterface)) {
                    continue;
                }
                validInterfaces.add((Class<? extends INTERFACE>) linkedInterface);
            }

            if (validInterfaces.isEmpty()) {
                Log.warn("[DI-Helper] No valid delegated interfaces found for "
                        + rawScannedConcrete.getSimpleName() + ", skipping...");
                skipped++;
                continue;
            }

            Class<? extends INTERFACE> primaryInterface = validInterfaces.iterator().next();
            IDependencyInterface<INTERFACE> wrappedLinkedInterface = new DependencyInterface<>(primaryInterface);
            IDependencyInstance<INSTANCE> wrappedLinkedConcrete = new DependencyInstance<>(rawScannedConcrete);
            IDependencyMetaData<INTERFACE, INSTANCE> dependencyMetaData = new DependencyMetaData<>();
            dependencyMetaDataHelper.populateMetaData(dependencyMetaData, wrappedLinkedInterface, wrappedLinkedConcrete);
            registeredMetaData.add(dependencyMetaData);

            for (Class<? extends INTERFACE> linkedInterface : validInterfaces) {
                RegisteredBinding binding = new RegisteredBinding(linkedInterface, rawScannedConcrete);
                if (!seenBindings.add(binding)) {
                    Log.info("[DI-Helper] Duplicate scanned binding for "
                            + linkedInterface.getName() + " -> " + rawScannedConcrete.getName() + ", skipping");
                    continue;
                }
                DependencyMap.getDependencyMap().registerDependency(linkedInterface, dependencyMetaData);
                Object registeredInstance = DependencyLoaderAccess.findInstance(
                        (Class<IDependencyInjectableInterface>) linkedInterface);
                if (registeredInstance != null) {
                    Log.critical("" + registeredInstance.getClass().getSimpleName());
                }
                registeredBindings++;
            }
        }

        for (IDependencyMetaData<INTERFACE, INSTANCE> dependencyMetaData : registeredMetaData) {
            dependencyMetaData.initializeDependencyInstance();
        }

        Log.warn("[DI-Helper] Finished annotation DI registration, skipped classes: " + skipped
                + ", registered interface bindings: " + registeredBindings);
    }

    private void unregisterLoadedBindings() {
        for (Class<? extends INSTANCE> rawScannedConcrete : loadedConcretes) {
            DelegatesToInterface concreteAnnotation = rawScannedConcrete.getAnnotation(DelegatesToInterface.class);
            if (concreteAnnotation == null) {
                continue;
            }

            for (Class<?> linkedInterface : resolveLinkedInterfaces(concreteAnnotation)) {
                if (linkedInterface == null || !linkedInterface.isInterface()) {
                    continue;
                }
                DependencyMap.getDependencyMap().removeDependency(linkedInterface);
            }
        }
    }

    private Set<Class<?>> registeredInterfaceKeys() {
        Set<Class<?>> bindings = new LinkedHashSet<>();
        for (Class<? extends INSTANCE> rawScannedConcrete : loadedConcretes) {
            DelegatesToInterface concreteAnnotation = rawScannedConcrete.getAnnotation(DelegatesToInterface.class);
            if (concreteAnnotation == null) {
                continue;
            }
            bindings.addAll(resolveLinkedInterfaces(concreteAnnotation));
        }
        return bindings;
    }

    private Set<Class<?>> resolveLinkedInterfaces(DelegatesToInterface concreteAnnotation) {
        Set<Class<?>> linkedInterfaces = new LinkedHashSet<>();
        if (concreteAnnotation == null) {
            return linkedInterfaces;
        }

        addLinkedInterface(linkedInterfaces, concreteAnnotation.getLinkedInterface());
        Arrays.stream(concreteAnnotation.getLinkedInterfaces())
                .forEach(linkedInterface -> addLinkedInterface(linkedInterfaces, linkedInterface));
        return linkedInterfaces;
    }

    private void addLinkedInterface(Set<Class<?>> linkedInterfaces, Class<?> linkedInterface) {
        if (linkedInterface == null || linkedInterface == Void.class) {
            return;
        }
        linkedInterfaces.add(linkedInterface);
    }

    /**
     * Checks whether a concrete class implements the supplied interface token.
     *
     * @param concrete the concrete class to inspect
     * @param targetInterface the interface token to verify
     * @return {@code true} when the concrete implements the token
     */
    public boolean doesConcreteImplementInterface(Class<?> concrete, Class<?> targetInterface) {

        if (concrete == null || targetInterface == null) {
            Log.error("[DI-Helper] doesConcreteImplementInterface received null parameters");
            return false;
        }

        boolean result = targetInterface.isAssignableFrom(concrete);

        if (result) {
            Log.success("[DI-Helper] " + concrete.getSimpleName()
                    + " implements/extends " + targetInterface.getSimpleName());
        } else {
            Log.warn("[DI-Helper] " + concrete.getSimpleName()
                    + " does NOT implement/extend " + targetInterface.getSimpleName() + " skipping...");
        }

        return result;
    }

    /**
     * Checks whether the supplied class can be loaded safely in the current runtime.
     *
     * @param dependencyClass the class to inspect
     * @return {@code true} when the class can be loaded without missing dependencies
     */
    public boolean isClassLoadable(Class<?> dependencyClass) {
        if (dependencyClass == null) {
            Log.warn("[DI-Helper] dependencyClass is null");
            return false;
        }
        try {
            Class.forName(dependencyClass.getName(), false, dependencyClass.getClassLoader());
            dependencyClass.getDeclaredMethods();
            return true;
        } catch (NoClassDefFoundError e) {
            Log.error("[SCAN] " + dependencyClass.getSimpleName() + " is not a loadable class in the runtime, skipping");
            return false;
        } catch (ClassNotFoundException e) {
            Log.exception(e);
        }
        return false;
    }

    /**
     * Checks whether the supplied wrapper class can be loaded safely.
     *
     * @param dependencyInterface the wrapper to inspect
     * @return {@code true} when the wrapper class can be loaded
     */
    public boolean isClassLoadable(IDependencyInterface<INTERFACE> dependencyInterface) {
        return isClassLoadable(dependencyInterface.getClass());
    }

    /**
     * Checks whether the supplied concrete wrapper class can be loaded safely.
     *
     * @param dependencyInstance the wrapper to inspect
     * @return {@code true} when the wrapper class can be loaded
     */
    public boolean isClassLoadable(IDependencyInstance<INSTANCE> dependencyInstance) {
        return isClassLoadable(dependencyInstance.getClass());
    }

    private void processDependencyClasses(String className, ClassLoader loader) {
        try {
            Class<?> rawClass = Class.forName(className, false, loader);

            if (!isClassLoadable(rawClass)) {
                return;
            }

            if (rawClass.isInterface() && IDependencyInjectableInterface.class.isAssignableFrom(rawClass)) {
                loadedInterfaces.add((Class<? extends INTERFACE>) rawClass);
                Log.success("[DI-Scan] Loaded interface: " + rawClass.getSimpleName());
            } else if (isConcrete(rawClass) && IDependencyInjectableConcrete.class.isAssignableFrom(rawClass)) {
                loadedConcretes.add((Class<? extends INSTANCE>) rawClass);
                Log.success("[DI-Scan] Loaded concrete: " + rawClass.getSimpleName());
            }

        } catch (Throwable ignored) {
        }
    }

    /**
     * Recursively scans a directory for loadable classes.
     *
     * @param pkg the package currently being scanned
     * @param dir the directory that maps to the package
     * @param loader the class loader used to resolve classes
     */
    public void scanDirectory(String pkg, File dir, ClassLoader loader) {
        for (File file : Objects.requireNonNull(dir.listFiles())) {

            if (file.isDirectory()) {
                scanDirectory(pkg + (pkg.isEmpty() ? "" : ".") + file.getName(), file, loader);
                continue;
            }

            if (!file.getName().endsWith(".class")) {
                continue;
            }

            String className = pkg + (pkg.isEmpty() ? "" : ".") + file.getName().replace(".class", "");
            processDependencyClasses(className, loader);
        }
    }

    /**
     * Scans a jar file for loadable classes under the supplied base package.
     *
     * @param jarFile the jar to scan
     * @param basePackage the base package to restrict scanning to
     * @param loader the class loader used to resolve classes
     */
    public void scanJar(File jarFile, String basePackage, ClassLoader loader) {
        if (jarFile == null || !jarFile.exists()) {
            return;
        }

        String prefix = basePackage.replace('.', '/') + "/";

        try (JarFile jar = new JarFile(jarFile)) {
            jar.stream()
                    .filter(e -> !e.isDirectory())
                    .filter(e -> e.getName().endsWith(".class"))
                    .filter(e -> e.getName().startsWith(prefix))
                    .forEach(entry -> {
                        String className = entry.getName()
                                .replace('/', '.')
                                .replace(".class", "");

                        processDependencyClasses(className, loader);
                    });

        } catch (Throwable e) {
            Log.error("[DI-Scan] Failed to scan JAR " + jarFile.getName());
            Log.exception(e);
        }
    }

    /**
     * Checks whether the supplied type is a non-abstract concrete class.
     *
     * @param clazz the class to inspect
     * @return {@code true} when the class is concrete
     */
    public boolean isConcrete(Class<?> clazz) {
        int mods = clazz.getModifiers();
        return !clazz.isInterface()
                && !Modifier.isAbstract(mods)
                && !clazz.isEnum()
                && !clazz.isAnnotation();
    }

    /**
     * Scans a package for injectable interfaces and concretes.
     *
     * @param basePackage the package prefix to scan
     * @param loader the class loader used to resolve classes
     */
    public void scanPackage(String basePackage, ClassLoader loader) {

        if (basePackage == null || basePackage.isBlank()) {
            return;
        }

        String path = basePackage.replace('.', '/');

        try {
            Enumeration<URL> resources = loader.getResources(path);

            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                String protocol = url.getProtocol();

                switch (protocol) {
                    case "file": {
                        File directory = new File(url.toURI());
                        scanDirectory(basePackage, directory, loader);
                        break;
                    }

                    case "jar":
                    case "zip":
                    case "wsjar":
                    case "war":
                    case "zipfs":
                    case "vfs":
                    case "vfszip":
                    case "bundleresource":
                    case "bundle": {
                        File jar = extractJarFile(url);
                        if (jar != null) {
                            scanJar(jar, basePackage, loader);
                        }
                        break;
                    }

                    case "jrt": {
                        scanModulePath(basePackage, loader);
                        break;
                    }

                    default:
                        Log.warn("[DI-Scan] Unsupported protocol: " + protocol + " @ " + url);
                        break;
                }
            }

        } catch (Throwable e) {
            Log.error("[DI-Scan] Failed to scan package " + basePackage);
            Log.exception(e);
        }

        Log.info("[DI-Scan] " + LogColor.GRAY
                + "Loaded " + loadedInterfaces.size() + " interfaces and "
                + loadedConcretes.size() + " concretes for package " + basePackage);
    }

    private void scanModulePath(String basePackage, ClassLoader loader) {
        try {
            ModuleLayer layer = ModuleLayer.boot();

            for (Module module : layer.modules()) {
                if (module.isNamed() && module.getPackages().contains(basePackage)) {

                    try (InputStream in = module.getResourceAsStream(basePackage.replace('.', '/') + "/")) {
                        if (in == null) {
                            continue;
                        }

                        Log.warn("[DI-Scan] JRT scanning is not fully implemented for: " + basePackage);
                    }
                }
            }
        } catch (Throwable e) {
            Log.error("[DI-Scan] Failed to scan JRT module path for " + basePackage);
            Log.exception(e);
        }
    }

    private File extractJarFile(URL url) {
        try {
            String external = url.toExternalForm();

            if (external.startsWith("jar:")) {
                external = external.substring(4);
            }

            int idx = external.indexOf("!/");
            if (idx != -1) {
                external = external.substring(0, idx);
            }

            if (external.startsWith("file:")) {
                external = external.substring(5);
            }

            return new File(URLDecoder.decode(external, StandardCharsets.UTF_8));
        } catch (Throwable e) {
            Log.error("[DI-Scan] Failed to extract JAR path from URL: " + url);
            Log.exception(e);
            return null;
        }
    }

    private record BootstrapKey(String basePackage, int classLoaderIdentity) {
    }

    private record RegisteredBinding(Class<?> interfaceType, Class<?> concreteType) {
    }
}
