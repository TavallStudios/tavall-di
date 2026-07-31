from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
JAVA_ROOT = ROOT / "src"
ANNOTATION_DIR = ROOT / "src/main/java/org/tavall/dependency/annotations"
OLD_ANNOTATION_PATH = ANNOTATION_DIR / "DelegatesToInterface.java"
NEW_ANNOTATION_PATH = ANNOTATION_DIR / "DelegatesTo.java"
HELPER_PATH = ROOT / "src/main/java/org/tavall/dependency/injection/helpers/DependencyInjectorHelper.java"
TEST_PATH = ROOT / "src/test/java/org/tavall/dependency/injection/helpers/DependencyInjectorHelperTest.java"
REDIS_FIXTURE_PATH = ROOT / "src/test/java/org/tavall/dependency/injection/helpers/fixtures/DelegatingRedisService.java"
LEGACY_FIXTURE_PATH = ROOT / "src/test/java/org/tavall/dependency/injection/helpers/legacyfixtures/LegacyDelegatingService.java"

OLD_IMPORT = "import org.tavall.dependency.annotations.DelegatesToInterface;"
NEW_IMPORT = "import org.tavall.dependency.annotations.DelegatesTo;"
ANNOTATION_PATTERN = re.compile(r"@DelegatesToInterface\s*\(")
CLASS_TOKEN_PATTERN = re.compile(r"(?:[A-Za-z_$][\w$]*\.)*[A-Za-z_$][\w$]*\.class")

NEW_ANNOTATION = '''/*
 * TJVD License (TJ Valentine’s Discretionary License) — Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package org.tavall.dependency.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares one or more dependency tokens that resolve to the annotated concrete instance.
 *
 * <p>A token may be an interface, the annotated concrete type, or any assignable supertype.
 * Every declared token is registered against the same dependency metadata and singleton instance.</p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DelegatesTo {

    /**
     * Returns the dependency tokens that should resolve to the annotated concrete.
     *
     * @return assignable interface or concrete dependency tokens
     */
    Class<?>[] value();
}
'''

DEPRECATED_ANNOTATION = '''/*
 * TJVD License (TJ Valentine’s Discretionary License) — Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package org.tavall.dependency.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a concrete class should be registered as the delegate for one or more supplied interface types.
 *
 * @deprecated Use {@link DelegatesTo}. This compatibility annotation remains supported while downstream modules migrate.
 */
@Deprecated(forRemoval = false)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DelegatesToInterface {

    /**
     * Primary interface token that should map to the annotated concrete.
     *
     * @return the primary interface token, or {@link Void} when unset
     */
    Class<?> value() default Void.class;

    /**
     * Returns the primary interface token that should map to the annotated concrete.
     *
     * @return the primary interface token, or {@link Void} when unset
     */
    Class<?> getLinkedInterface() default Void.class;

    /**
     * Returns any additional interface tokens that should map to the annotated concrete.
     *
     * @return the additional interface tokens
     */
    Class<?>[] getLinkedInterfaces() default {};
}
'''

LEGACY_FIXTURE = '''package org.tavall.dependency.injection.helpers.legacyfixtures;

import org.tavall.dependency.IDependencyInjectableConcrete;
import org.tavall.dependency.annotations.DelegatesToInterface;
import org.tavall.dependency.fixtures.contracts.interfaces.IRedis;

@SuppressWarnings("deprecation")
@DelegatesToInterface(IRedis.class)
public final class LegacyDelegatingService implements IRedis, IDependencyInjectableConcrete {
    @Override
    public void connectToRedis() {
    }

    @Override
    public void disconnectFromRedis() {
    }

    @Override
    public void publishRedisUpdate(String message) {
    }
}
'''

NEW_HELPER_BLOCK = '''    /**
     * Registers annotated concrete classes against their declared dependency tokens.
     */
    @SuppressWarnings({"unchecked", "deprecation"})
    public void registerDependenciesViaAnnotation() {

        Log.warn("[DI-Helper] ====== Beginning annotation-driven DI registration ======");

        int skipped = 0;
        int registeredBindings = 0;
        Set<IDependencyMetaData<INTERFACE, INSTANCE>> registeredMetaData = new LinkedHashSet<>();
        Set<RegisteredBinding> seenBindings = new LinkedHashSet<>();

        for (Class<? extends INSTANCE> rawScannedConcrete : loadedConcretes) {
            Set<Class<?>> validDelegatedTypes = validDelegatedTypes(rawScannedConcrete);
            if (validDelegatedTypes.isEmpty()) {
                if (hasDelegationAnnotation(rawScannedConcrete)) {
                    Log.warn("[DI-Helper] No valid delegated dependency tokens found for "
                            + rawScannedConcrete.getSimpleName() + ", skipping...");
                    skipped++;
                }
                continue;
            }
            if (!IDependencyInjectableConcrete.class.isAssignableFrom(rawScannedConcrete)) {
                Log.warn("[DI-Helper] " + rawScannedConcrete.getSimpleName()
                        + " is annotated for DI but does not implement IDependencyInjectableConcrete, skipping...");
                skipped++;
                continue;
            }

            Class<? extends INTERFACE> primaryDependencyType =
                    (Class<? extends INTERFACE>) validDelegatedTypes.iterator().next();
            IDependencyInterface<INTERFACE> wrappedDependencyType =
                    new DependencyInterface<>(primaryDependencyType);
            IDependencyInstance<INSTANCE> wrappedLinkedConcrete = new DependencyInstance<>(rawScannedConcrete);
            IDependencyMetaData<INTERFACE, INSTANCE> dependencyMetaData = new DependencyMetaData<>();
            dependencyMetaDataHelper.populateMetaData(
                    dependencyMetaData,
                    wrappedDependencyType,
                    wrappedLinkedConcrete
            );
            registeredMetaData.add(dependencyMetaData);

            for (Class<?> delegatedType : validDelegatedTypes) {
                RegisteredBinding binding = new RegisteredBinding(delegatedType, rawScannedConcrete);
                if (!seenBindings.add(binding)) {
                    Log.info("[DI-Helper] Duplicate scanned binding for "
                            + delegatedType.getName() + " -> " + rawScannedConcrete.getName() + ", skipping");
                    continue;
                }
                DependencyMap.getDependencyMap().registerDependency(delegatedType, dependencyMetaData);
                Object registeredInstance = DependencyLoaderAccess.findInstance((Class<Object>) delegatedType);
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
                + ", registered dependency bindings: " + registeredBindings);
    }

    private void unregisterLoadedBindings() {
        for (Class<? extends INSTANCE> rawScannedConcrete : loadedConcretes) {
            for (Class<?> delegatedType : validDelegatedTypes(rawScannedConcrete)) {
                DependencyMap.getDependencyMap().removeDependency(delegatedType);
            }
        }
    }

    private Set<Class<?>> registeredBindingKeys() {
        Set<Class<?>> bindings = new LinkedHashSet<>();
        for (Class<? extends INSTANCE> rawScannedConcrete : loadedConcretes) {
            bindings.addAll(validDelegatedTypes(rawScannedConcrete));
        }
        return bindings;
    }

    private Set<Class<?>> validDelegatedTypes(Class<? extends INSTANCE> rawScannedConcrete) {
        Set<Class<?>> validTypes = new LinkedHashSet<>();
        for (Class<?> delegatedType : resolveDelegatedTypes(rawScannedConcrete)) {
            if (!delegatedType.isAssignableFrom(rawScannedConcrete)) {
                Log.warn("[DI-Helper] " + delegatedType.getName()
                        + " is not assignable from " + rawScannedConcrete.getName() + ", skipping...");
                continue;
            }
            validTypes.add(delegatedType);
        }
        return validTypes;
    }

    @SuppressWarnings("deprecation")
    private Set<Class<?>> resolveDelegatedTypes(Class<?> rawScannedConcrete) {
        Set<Class<?>> delegatedTypes = new LinkedHashSet<>();
        DelegatesTo delegatesTo = rawScannedConcrete.getAnnotation(DelegatesTo.class);
        DelegatesToInterface legacyDelegatesTo =
                rawScannedConcrete.getAnnotation(DelegatesToInterface.class);

        if (delegatesTo != null && legacyDelegatesTo != null) {
            throw new IllegalStateException("Dependency concrete cannot declare both @DelegatesTo and "
                    + "@DelegatesToInterface: " + rawScannedConcrete.getName());
        }
        if (delegatesTo != null) {
            Arrays.stream(delegatesTo.value())
                    .forEach(delegatedType -> addDelegatedType(delegatedTypes, delegatedType));
            return delegatedTypes;
        }
        if (legacyDelegatesTo == null) {
            return delegatedTypes;
        }

        addDelegatedType(delegatedTypes, legacyDelegatesTo.value());
        addDelegatedType(delegatedTypes, legacyDelegatesTo.getLinkedInterface());
        Arrays.stream(legacyDelegatesTo.getLinkedInterfaces())
                .forEach(delegatedType -> addDelegatedType(delegatedTypes, delegatedType));
        return delegatedTypes;
    }

    private boolean hasDelegationAnnotation(Class<?> rawScannedConcrete) {
        return rawScannedConcrete.isAnnotationPresent(DelegatesTo.class)
                || rawScannedConcrete.isAnnotationPresent(DelegatesToInterface.class);
    }

    private void addDelegatedType(Set<Class<?>> delegatedTypes, Class<?> delegatedType) {
        if (delegatedType == null || delegatedType == Void.class) {
            return;
        }
        delegatedTypes.add(delegatedType);
    }

'''


def find_annotation_end(text: str, open_parenthesis: int) -> int:
    depth = 0
    for index in range(open_parenthesis, len(text)):
        character = text[index]
        if character == "(":
            depth += 1
        elif character == ")":
            depth -= 1
            if depth == 0:
                return index + 1
    raise RuntimeError("Unbalanced @DelegatesToInterface annotation")


def migrate_java_annotation(text: str, path: Path) -> str:
    text = text.replace(OLD_IMPORT, NEW_IMPORT)
    search_from = 0
    while True:
        match = ANNOTATION_PATTERN.search(text, search_from)
        if match is None:
            break
        open_parenthesis = text.find("(", match.start())
        end = find_annotation_end(text, open_parenthesis)
        body = text[open_parenthesis + 1:end - 1]
        tokens = CLASS_TOKEN_PATTERN.findall(body)
        if not tokens:
            raise RuntimeError(f"No class tokens found in annotation: {path}")
        line_start = text.rfind("\n", 0, match.start()) + 1
        indentation = text[line_start:match.start()]
        if len(tokens) == 1:
            replacement = f"@DelegatesTo({tokens[0]})"
        else:
            token_lines = (",\n" + indentation + "        ").join(tokens)
            replacement = (
                "@DelegatesTo({\n"
                + indentation
                + "        "
                + token_lines
                + "\n"
                + indentation
                + "})"
            )
        text = text[:match.start()] + replacement + text[end:]
        search_from = match.start() + len(replacement)
    return text


def replace_once(text: str, old: str, new: str, description: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"Expected one {description}, found {count}")
    return text.replace(old, new, 1)


def update_helper() -> None:
    text = HELPER_PATH.read_text(encoding="utf-8")
    text = replace_once(
        text,
        OLD_IMPORT,
        NEW_IMPORT + "\n" + OLD_IMPORT,
        "legacy annotation import",
    )
    text = replace_once(
        text,
        "REGISTERED_BINDINGS.put(bootstrapKey, registeredInterfaceKeys());",
        "REGISTERED_BINDINGS.put(bootstrapKey, registeredBindingKeys());",
        "registered binding call",
    )
    start_marker = "    /**\n     * Registers annotated concrete classes against their declared interface tokens.\n     */"
    end_marker = "    private void flushPendingCacheRegistryMetaData"
    start = text.find(start_marker)
    end = text.find(end_marker)
    if start < 0 or end < 0 or end <= start:
        raise RuntimeError("Unable to locate DependencyInjectorHelper registration block")
    text = text[:start] + NEW_HELPER_BLOCK + text[end:]
    text = text.replace(
        "Scans packages for annotated DI concretes and registers them against interface tokens.",
        "Scans packages for annotated DI concretes and registers them against dependency tokens.",
    )
    HELPER_PATH.write_text(text, encoding="utf-8")


def update_test() -> None:
    text = TEST_PATH.read_text(encoding="utf-8")
    text = replace_once(
        text,
        "import org.tavall.dependency.injection.helpers.multiplefixtures.DelegatingMultiInterfaceService;",
        "import org.tavall.dependency.injection.helpers.multiplefixtures.DelegatingMultiInterfaceService;\n"
        "import org.tavall.dependency.injection.helpers.legacyfixtures.LegacyDelegatingService;",
        "legacy fixture import anchor",
    )
    text = replace_once(
        text,
        "    private static final String MULTI_FIXTURE_PACKAGE = \"org.tavall.dependency.injection.helpers.multiplefixtures\";",
        "    private static final String MULTI_FIXTURE_PACKAGE = \"org.tavall.dependency.injection.helpers.multiplefixtures\";\n"
        "    private static final String LEGACY_FIXTURE_PACKAGE = \"org.tavall.dependency.injection.helpers.legacyfixtures\";",
        "fixture package anchor",
    )
    concrete_test = '''
    @Test
    void registersConcreteDependencyTokenAgainstTheSameSingleton() throws Throwable {
        TestableDependencyInjectorHelper helper = new TestableDependencyInjectorHelper();
        helper.BASE_PACKAGE = FIXTURE_PACKAGE;

        helper.setupDISystem();

        IRedis redis = DependencyLoaderAccess.requireInstance(IRedis.class);
        DelegatingRedisService concrete = DependencyLoaderAccess.requireInstance(DelegatingRedisService.class);
        assertSame(redis, concrete);
    }

    @SuppressWarnings("deprecation")
    @Test
    void deprecatedDelegatesToInterfaceRemainsSupported() throws Throwable {
        TestableDependencyInjectorHelper helper = new TestableDependencyInjectorHelper();
        helper.BASE_PACKAGE = LEGACY_FIXTURE_PACKAGE;

        helper.setupDISystem();

        IRedis redis = DependencyLoaderAccess.requireInstance(IRedis.class);
        assertTrue(redis instanceof LegacyDelegatingService);
    }

'''
    insertion_anchor = "    private void runBootstrapAndAssertFixtureBindings(ThrowingHelperBootstrap bootstrap) throws Throwable {"
    if insertion_anchor not in text:
        raise RuntimeError("Unable to locate test insertion anchor")
    text = text.replace(insertion_anchor, concrete_test + insertion_anchor, 1)
    TEST_PATH.write_text(text, encoding="utf-8")


def update_docs() -> None:
    for path in ROOT.rglob("*.md"):
        if any(part in {".git", "build"} for part in path.parts):
            continue
        text = path.read_text(encoding="utf-8")
        migrated = text.replace("DelegatesToInterface", "DelegatesTo")
        if migrated != text:
            path.write_text(migrated, encoding="utf-8")


def main() -> None:
    ANNOTATION_DIR.mkdir(parents=True, exist_ok=True)
    NEW_ANNOTATION_PATH.write_text(NEW_ANNOTATION, encoding="utf-8")
    OLD_ANNOTATION_PATH.write_text(DEPRECATED_ANNOTATION, encoding="utf-8")

    update_helper()

    for path in JAVA_ROOT.rglob("*.java"):
        if path in {OLD_ANNOTATION_PATH, NEW_ANNOTATION_PATH, HELPER_PATH}:
            continue
        text = path.read_text(encoding="utf-8")
        migrated = migrate_java_annotation(text, path)
        if migrated != text:
            path.write_text(migrated, encoding="utf-8")

    redis_fixture = REDIS_FIXTURE_PATH.read_text(encoding="utf-8")
    redis_fixture = replace_once(
        redis_fixture,
        "@DelegatesTo(IRedis.class)",
        "@DelegatesTo({IRedis.class, DelegatingRedisService.class})",
        "Redis fixture annotation",
    )
    REDIS_FIXTURE_PATH.write_text(redis_fixture, encoding="utf-8")

    LEGACY_FIXTURE_PATH.parent.mkdir(parents=True, exist_ok=True)
    LEGACY_FIXTURE_PATH.write_text(LEGACY_FIXTURE, encoding="utf-8")
    update_test()
    update_docs()

    stale_java = [
        str(path.relative_to(ROOT))
        for path in JAVA_ROOT.rglob("*.java")
        if path not in {OLD_ANNOTATION_PATH, LEGACY_FIXTURE_PATH}
        and "DelegatesToInterface" in path.read_text(encoding="utf-8")
    ]
    if stale_java:
        raise RuntimeError("Unmigrated Java annotation usages: " + ", ".join(stale_java))


if __name__ == "__main__":
    main()
