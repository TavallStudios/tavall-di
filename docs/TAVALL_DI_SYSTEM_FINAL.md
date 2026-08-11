# About

Tavall DI owns dependency discovery, token delegation, metadata ownership, instance lifecycle, replacement, typed dependency access, and the source lowering required for expandable `DependencyAccess<...>` declarations.

This document is the authoritative final system contract. The detailed production and test access rankings live in [Tavall DI Access Styles](DI_ACCESS_STYLES.md).

Tavall DI does not own application-domain bundle contents, platform lifecycle policy, event routing, persistence rules, or consumer-module architecture.

# Design Sources

- [`@DelegatesTo`](../src/main/java/org/tavall/dependency/annotations/DelegatesTo.java)
- [`DependencyMap`](../src/main/java/org/tavall/dependency/maps/DependencyMap.java)
- [`IDependencyMap`](../src/main/java/org/tavall/dependency/maps/interfaces/IDependencyMap.java)
- [`DependencyMetaData`](../src/main/java/org/tavall/dependency/metadata/DependencyMetaData.java)
- [`IDependencyMetaData`](../src/main/java/org/tavall/dependency/metadata/interfaces/IDependencyMetaData.java)
- [`IDependencyAccess`](../src/main/java/org/tavall/dependency/IDependencyAccess.java)
- [`DependencyAccess`](../src/main/java/org/tavall/dependency/DependencyAccess.java)
- [`DependencyAccessSourceLowerer`](../src/main/java/org/tavall/dependency/access/DependencyAccessSourceLowerer.java)
- [`DependencyAccessSourceLowererMain`](../src/main/java/org/tavall/dependency/access/DependencyAccessSourceLowererMain.java)
- [Tavall DI Access Styles](DI_ACCESS_STYLES.md)

Separate Final Design: Not created. The accepted design is implemented and captured directly by this Final System document.

# System Guide

| Area | Section |
|---|---|
| Runtime ownership | [Runtime Model](#runtime-model) |
| Binding declaration | [`@DelegatesTo` Pattern](#delegatesto-pattern) |
| Metadata and lookup | [Metadata-Owned Instance Pattern](#metadata-owned-instance-pattern) |
| Direct access | [Dependency Map Pattern](#dependency-map-pattern) |
| Expanded access | [Generated Dependency Access Pattern](#generated-dependency-access-pattern) |
| Domain grouping | [Domain Bundle Pattern](#domain-bundle-pattern) |
| Replacement | [Replacement Pattern](#replacement-pattern) |
| Migration | [Compatibility Surfaces](#compatibility-surfaces) |
| Failures | [Failure Rules](#failure-rules) |
| Evidence | [Production Verification](#production-verification) |

# Runtime Model

Tavall DI has one runtime authority:

```text
dependency token
    -> DependencyMap
    -> IDependencyMetaData
    -> metadata-owned instance
```

Generated access classes, authored bundles, compatibility loaders, and direct map calls must resolve to that same metadata-owned instance. They must not create a second container or instance cache.

`DependencyMap` remains a `ConcurrentHashMap<Class<?>, IDependencyMetaData<?, ?>>`. Inherited map mutation stays available for advanced or framework use. Named Tavall DI APIs validate coherent registration and replacement; raw inherited operations remain intentionally sharp.

# `@DelegatesTo` Pattern

`@DelegatesTo` marks a DI-managed concrete. The annotated concrete token is implicit.

```java
@DelegatesTo
public final class AchievementPointTitleResolver {
}
```

Additional values are assignable interface or supertype tokens:

```java
@DelegatesTo({
        IPlayerData.class,
        IPlayerWallet.class,
        IPlayerPreferences.class
})
public final class PlayerData
        implements IPlayerData, IPlayerWallet, IPlayerPreferences {
}
```

The scanner registers:

```text
PlayerData.class
IPlayerData.class
IPlayerWallet.class
IPlayerPreferences.class
    -> same metadata
    -> same instance
```

Rules:

- annotation values are optional;
- the concrete type is always included;
- duplicate tokens are deduplicated;
- every additional token must be assignable from the concrete;
- one concrete produces one metadata object;
- lifecycle initialization occurs once per metadata object;
- `@DelegatesToInterface` remains deprecated compatibility only.

Injectable interface and concrete marker interfaces are not required for new code.

# Metadata-Owned Instance Pattern

`IDependencyMetaData` owns the created or bound dependency instance.

```java
<T> T findInstance(Class<T> dependencyType);

<T> T getInstance(Class<T> dependencyType);
```

`findInstance` returns `null` when the metadata does not expose a compatible instance.

`getInstance` fails when the requested token is incompatible.

Metadata lookup returns the stored instance. It does not reconstruct the dependency, hydrate a bundle, consult another cache, or manufacture a token-specific wrapper instance.

# Dependency Map Pattern

`IDependencyMap` exposes nullable and required lookup:

```java
<T> T findInstance(Class<T> dependencyType);

<T> T getInstance(Class<T> dependencyType);
```

Required lookup follows:

```text
Class<T>
    -> mapped metadata
    -> metadata.getInstance(Class<T>)
    -> stored compatible instance
```

`IDependencyAccess` uses the owning map:

```java
default IDependencyMap getDependencyMap() {
    return DependencyMap.getDependencyMap();
}

default <T> T getInstance(Class<T> dependencyType) {
    return getDependencyMap().getInstance(dependencyType);
}
```

Consumers may override `getDependencyMap()` to use a module, request, or test-owned map.

`requireInstance` remains a deprecated compatibility alias for `getInstance`.

# Single Dependency Access Pattern

After source lowering, `DependencyAccess` has one runtime type parameter:

```java
public interface DependencyAccess<ACCESS> extends IDependencyAccess {
    default ACCESS getInstance() {
        return getDependencyMap().getInstance(getDependencyAccessType());
    }
}
```

A one-parameter authored declaration is not expanded:

```java
DependencyAccess<IPlayerData>
DependencyAccess<PlayerRewardDependencies>
```

`getInstance()` resolves that authored token directly.

# Generated Dependency Access Pattern

Two or more authored parameters activate source lowering:

```java
@DelegatesTo
public final class PlayerRewardHandler
        implements DependencyAccess<
                IPlayerData,
                IEconomyService,
                IRewardAuditLogger
        > {
}
```

The lowerer rewrites the consumer to:

```java
DependencyAccess<PlayerRewardHandlerDependencyAccess>
```

It also generates a map-backed access class:

```java
@DelegatesTo
public final class PlayerRewardHandlerDependencyAccess
        implements IDependencyAccess {
    private final IDependencyMap dependencyMap;

    public IPlayerData playerData() {
        return dependencyMap.getInstance(IPlayerData.class);
    }

    public IEconomyService economyService() {
        return dependencyMap.getInstance(IEconomyService.class);
    }
}
```

Generated access classes support:

- a no-argument global-map constructor;
- an `IDependencyMap` constructor for scoped consumers;
- live dependency lookup on every getter;
- technically unbounded dependency arity.

Four direct dependencies is an architecture review preference, not a parser, compiler, processor, or runtime limit.

Generated accessor names:

```text
IPlayerData -> playerData()
IEconomyService -> economyService()
PlayerProfileCache -> playerProfileCache()
```

The lowerer rejects:

- raw `DependencyAccess`;
- wildcards;
- unresolved owner type variables;
- parameterized types that cannot become class literals;
- duplicate dependency types;
- generated accessor-name collisions;
- expanded access without `@DelegatesTo`;
- generated type-name collisions.

# Build Lowering Pattern

Expandable syntax is not legal Java after ordinary generic arity checking. The build runs source lowering before Javac.

The command-line entry point is:

```text
org.tavall.dependency.access.DependencyAccessSourceLowererMain
    --output <generated-source-directory>
    --source <authored-source-directory>
```

The lowerer copies authored source into the generated source root, rewrites expanded consumers, and adds generated access classes. The consuming build compiles that generated source root instead of the authored Java root.

JSR 269 processing cannot replace an already parsed class signature, so annotation processing alone is not the lowering mechanism.

# Domain Bundle Pattern

A domain bundle is an ordinary DI contract used as a one-parameter access token:

```java
public interface PlayerRewardDependencies {
    IPlayerData playerData();

    IEconomyService economyService();

    IRewardAuditLogger rewardAuditLogger();
}
```

Its implementation is a normal managed concrete and may itself use expanded dependency access.

Bundles are appropriate when dependencies form a reusable domain boundary or require authored component names. Bundles do not require reflective record hydration, bundle-specific factories, or separate lifecycle ownership.

# Replacement Pattern

Replacement validates every token mapped to the metadata before publication:

```text
replacement supplier
    -> create replacement
    -> verify every alias token
    -> initialize replacement
    -> publish metadata-owned instance
```

An existing generated access object observes the replacement because its getter resolves through the map each time.

Field-cached dependencies intentionally retain their captured instance and are governed by the access-style guide.

# Lifecycle Pattern

Metadata owns:

- construction or direct binding;
- pre-construction callbacks;
- field injection;
- post-construction callbacks;
- replacement initialization;
- lifecycle diagnostics and retry state.

Multiple delegated tokens do not cause repeated construction or lifecycle execution.

A failed required injection or lifecycle callback fails registration rather than publishing a partially initialized dependency as healthy.

# Compatibility Surfaces

The following surfaces remain temporarily for downstream migration:

- `DependencyLoader` and `DependencyLoaderAccess` for tests, named scopes, and legacy composition;
- `@DelegatesToInterface` as a deprecated annotation alias;
- injectable marker interfaces for existing consumers;
- reflective bundle classes for existing consumers;
- grant annotations, metadata emitters, and the historical grant processor.

New generated access does not use the loader or grant metadata path.

Compatibility removal requires downstream usage search and migration. Existing public classes are not deleted merely because the newer design is less embarrassing.

# Failure Rules

## Registration

Fail clearly for:

- null tokens or metadata through named APIs;
- an additional delegated token not assignable from the concrete;
- a bound instance incompatible with its token;
- incompatible replacement across any shared alias token;
- conflicting legacy and current delegation annotations.

## Lookup

`getInstance` includes the requested token in missing or incompatible binding diagnostics.

`findInstance` returns `null` only for intentional nullable lookup.

A mapped token whose metadata cannot expose a compatible instance is a broken binding, not an ordinary absence.

## Source Lowering

Diagnostics identify the owning consumer and invalid dependency declaration. Accessor collisions recommend an authored domain bundle rather than inventing numbered method names.

## Direct Map Mutation

Inherited map mutation may bypass lifecycle and validation. Named lookup must still fail clearly if raw mutation leaves incoherent metadata.

# Production Verification

The active implementation includes:

| Behavior | Evidence |
|---|---|
| Implicit concrete and multiple token delegation | [`DelegatesToTest`](../src/test/java/org/tavall/dependency/annotations/DelegatesToTest.java) |
| Marker-free annotation scanning | [`DependencyInjectorHelper`](../src/main/java/org/tavall/dependency/injection/helpers/DependencyInjectorHelper.java) |
| Metadata-owned typed lookup | [`DependencyMetaData`](../src/main/java/org/tavall/dependency/metadata/DependencyMetaData.java) |
| Required and nullable map lookup | [`DependencyMap`](../src/main/java/org/tavall/dependency/maps/DependencyMap.java) |
| Single-token runtime access | [`DependencyAccess`](../src/main/java/org/tavall/dependency/DependencyAccess.java) |
| Direct expanded source generation | [`DependencyAccessSourceLowerer`](../src/main/java/org/tavall/dependency/access/DependencyAccessSourceLowerer.java) |
| Build-facing lowering CLI | [`DependencyAccessSourceLowererMain`](../src/main/java/org/tavall/dependency/access/DependencyAccessSourceLowererMain.java) |
| Unbounded generation and collision diagnostics | [`DependencyAccessSourceLowererTest`](../src/test/java/org/tavall/dependency/access/DependencyAccessSourceLowererTest.java) |
| Generated runtime lookup and live replacement | [`DependencyAccessFiveDependencyIntegrationTest`](../src/test/java/org/tavall/dependency/access/DependencyAccessFiveDependencyIntegrationTest.java) |

Java 25 validation passed on branch head `e99f287bc366f312d231dc70d48df603b3f4720e` with:

```text
./gradlew clean check
```

Publication validation uses the authenticated GitHub Packages repository configured
by the Gradle build. The workflow's manual publication path runs:

```text
./gradlew -PtavallVersion=<version> clean check publish
```

Remaining work is downstream migration and eventual compatibility-surface removal, not implementation of the active access path.
