# About

Tavall DI owns dependency discovery, token delegation, metadata ownership, instance lifecycle, replacement, typed dependency access, and the compile-time lowering required to support expandable `DependencyAccess<...>` declarations.

This document is the authoritative final system contract for Tavall DI. It separates current production foundations from planned changes required by the approved design.

Tavall DI owns:

- `@DelegatesTo` binding declarations
- token-to-metadata registration
- metadata-owned dependency instances
- lifecycle initialization and replacement
- `DependencyMap` and `IDependencyMap`
- `IDependencyAccess`
- single-parameter bundle access
- expanded dependency-access generation
- access-name validation
- compatibility migration from deprecated DI surfaces
- DI-focused tests and production verification

Tavall DI does not own:

- application-domain bundle contents
- application startup order outside DI lifecycle callbacks
- platform event registration
- Bukkit, Velocity, Spring, or other framework lifecycle policy
- cache, registry, database, or event-bus domain rules
- consumer-module architecture beyond the DI contract they import

Implementation history is not maintained in this document. A separate progression document is not currently required.

# Design Sources

- [Tavall DI Access Styles](DI_ACCESS_STYLES.md)
- [`@DelegatesTo`](../src/main/java/org/tavall/dependency/annotations/DelegatesTo.java)
- [`DependencyMap`](../src/main/java/org/tavall/dependency/maps/DependencyMap.java)
- [`IDependencyMap`](../src/main/java/org/tavall/dependency/maps/interfaces/IDependencyMap.java)
- [`IDependencyMetaData`](../src/main/java/org/tavall/dependency/metadata/interfaces/IDependencyMetaData.java)
- [`IDependencyAccess`](../src/main/java/org/tavall/dependency/IDependencyAccess.java)
- [`DependencyAccess`](../src/main/java/org/tavall/dependency/access/DependencyAccess.java)
- [`DependencyAccessSourceLowerer`](../src/main/java/org/tavall/dependency/access/DependencyAccessSourceLowerer.java)
- [`DependencyAccessGrantProcessor`](../src/main/java/org/tavall/dependency/processor/DependencyAccessGrantProcessor.java)

Separate Final Design: Not created. The approved design decisions are captured directly by this Final System document and the access-style support document.

# System Guide

| System Area | Section |
|---|---|
| Runtime ownership | [Technical Architecture](#technical-architecture) |
| Current implementation | [Active Production Foundations](#active-production-foundations) |
| Binding declarations | [`@DelegatesTo` Registration](#delegatesto-registration) |
| Instance authority | [Metadata-Owned Instances](#metadata-owned-instances) |
| Map behavior | [Dependency Map](#dependency-map) |
| Access API | [Dependency Access Contract](#dependency-access-contract) |
| Generated access | [Expanded Dependency Access](#expanded-dependency-access) |
| Domain bundles | [Authored Domain Bundles](#authored-domain-bundles) |
| Lifecycle and replacement | [Lifecycle and Replacement](#lifecycle-and-replacement) |
| Compatibility cleanup | [Migration and Deprecated Surfaces](#migration-and-deprecated-surfaces) |
| Invalid state | [Failure Rules](#failure-rules) |
| Evidence and missing work | [Production Verification](#production-verification) |

# Technical Architecture

Tavall DI is a Java-only module. It must not depend on Bukkit, Velocity, Spring, or application-domain code.

The canonical runtime flow is:

```text
@DelegatesTo concrete
    -> scanner validates delegated tokens
    -> one IDependencyMetaData owns the concrete instance
    -> DependencyMap stores token -> metadata entries
    -> IDependencyMap#getInstance resolves metadata
    -> metadata returns its stored compatible instance
```

Typed access is layered over that runtime:

```text
DependencyAccess<Bundle>
    -> resolves the authored bundle token

DependencyAccess<A, B, C, ...>
    -> compile-time expansion
    -> final generated access token
    -> generated getter
    -> IDependencyMap#getInstance(dependency token)
```

There is one instance authority: metadata. Generated access types, bundles, maps, and compatibility loaders may route to that authority but may not create competing instance ownership.

## Runtime Layers

| Layer | Responsibility |
|---|---|
| Annotation declaration | Defines the concrete and every token that may resolve to it. |
| Scanner and registration | Finds annotated concretes, validates assignability, creates metadata, and maps tokens. |
| Metadata | Owns the concrete type, instance, supplier, wrappers, lifecycle state, and replacement behavior. |
| Dependency map | Maps tokens to metadata and provides authoritative typed lookup. |
| Access contract | Exposes map-backed lookup to consumers and generated access types. |
| Source lowerer and processor | Convert expandable authored syntax into valid Java types and generated access surfaces. |
| Compatibility loader | Supports tests, scopes, and migration without becoming the production access authority. |

# Active Production Foundations

The repository currently contains these production foundations:

- `@DelegatesTo` accepts one or more dependency tokens and documents that all tokens resolve to the same metadata and singleton instance.
- `DependencyMap` extends `ConcurrentHashMap<Class<?>, IDependencyMetaData<?, ?>>`.
- `IDependencyMap` exposes registration, lookup, replacement, removal, and map-state operations.
- `DependencyMap#findInstance` resolves the instance through mapped metadata.
- `IDependencyMetaData` owns interface and concrete wrappers, the dependency instance, supplier, lifecycle state, source context, depth, role, priority, and replacement behavior.
- `IDependencyAccess` currently provides local-first lookup and delegates global behavior through `DependencyLoader`.
- `DependencyAccess` currently exposes a fixed four-parameter source shape.
- The source lowerer and grant processor currently lower variable-looking generic declarations into grant metadata rather than the final generated access model.
- A reflective bundle-access stack currently exists.

These foundations do not prove that the approved access-generation design is implemented.

# `@DelegatesTo` Registration

## Final Contract

A managed concrete is declared with `@DelegatesTo`:

```java
@DelegatesTo(IPlayerData.class)
public final class PlayerData implements IPlayerData {
}
```

Multiple assignable tokens may resolve to one concrete:

```java
@DelegatesTo({
        IPlayerData.class,
        IPlayerWallet.class,
        PlayerData.class
})
public final class PlayerData
        implements IPlayerData, IPlayerWallet {
}
```

The scanner must:

1. Discover concrete classes carrying `@DelegatesTo`.
2. Reject a missing or empty token declaration.
3. Verify every token is assignable from the concrete.
4. Create one metadata object for the concrete.
5. Register every delegated token against that same metadata object.
6. Initialize the metadata-owned instance through the normal lifecycle.
7. Produce deterministic diagnostics for conflicting registrations.

Marker interfaces are not required to identify injectable interfaces or concretes.

## Processor Activation

`@DelegatesTo` is also the activation boundary for expanded `DependencyAccess`.

The processor runs access expansion when:

- the declaration is a concrete class
- the class carries `@DelegatesTo`
- the class implements `DependencyAccess`
- the declaration supplies two or more dependency type parameters

One type parameter is left as a normal access token and is not expanded.

# Metadata-Owned Instances

`IDependencyMetaData` is the authoritative owner of the dependency instance.

The final metadata contract must expose typed lookup:

```java
<T> T findInstance(Class<T> dependencyType);

<T> T getInstance(Class<T> dependencyType);
```

`findInstance` returns `null` when the stored instance cannot satisfy the requested token.

`getInstance` fails clearly when metadata does not own a compatible instance.

Every delegated token mapped to the same metadata must return the same object identity until replacement occurs.

Metadata lookup must not:

- reflectively reconstruct the dependency
- ask a generated access object to own the dependency
- consult a second instance cache
- return a wrapper when the caller requested the dependency token
- silently cast an incompatible instance

# Dependency Map

`DependencyMap` remains the authoritative token-to-metadata map and may continue extending `ConcurrentHashMap`.

The design intentionally leaves direct map mutation available. Tavall DI provides safe named methods and validation for normal operations but does not forbid advanced developers from deliberately using inherited map operations.

The map contract must provide:

```java
<T> IDependencyMetaData<?, ?> findMetaData(Class<T> dependencyType);

<T> T findInstance(Class<T> dependencyType);

<T> T getInstance(Class<T> dependencyType);
```

`getInstance` must:

1. Resolve metadata for the token.
2. Ask that metadata for the compatible stored instance.
3. Return the metadata-owned instance.
4. Fail with the requested token when the binding is missing or incompatible.

Registration and replacement methods must reject structurally incoherent state, including an instance that cannot satisfy its token.

Inherited raw map operations are intentionally sharp. They do not receive a fictional guarantee that direct mutation preserves DI lifecycle or metadata coherence.

# Dependency Access Contract

`IDependencyAccess` is the shared map-backed access surface.

The final contract should expose:

```java
default IDependencyMap getDependencyMap() {
    return DependencyMap.getDependencyMap();
}

default <T> T findInstance(Class<T> dependencyType) {
    return getDependencyMap().findInstance(dependencyType);
}

default <T> T getInstance(Class<T> dependencyType) {
    return getDependencyMap().getInstance(dependencyType);
}
```

Production lookup must prefer `IDependencyMap`.

`DependencyLoader` remains a test, scope, and compatibility surface during migration. It must not be the lookup path generated into production access classes.

# Expanded Dependency Access

## Authored Form

Two or more parameters activate expansion:

```java
@DelegatesTo(IPlayerRewardHandler.class)
public final class PlayerRewardHandler
        implements IPlayerRewardHandler,
                   DependencyAccess<
                           IPlayerData,
                           IEconomyService,
                           IRewardAuditLogger
                   > {
}
```

## Arity

Expansion is technically unbounded.

```java
DependencyAccess<A, B>
DependencyAccess<A, B, C, D>
DependencyAccess<A, B, C, D, E, F, G, H>
```

The processor generates the access structure required for every supplied type.

Four direct dependencies is the preferred code-review boundary. It is not:

- a parser limit
- a processor limit
- a compiler error
- a truncation point
- a required warning

A larger direct declaration is valid when it remains clearer than a bundle.

## Generated Shape

The lowerer must rewrite the authored variable-arity declaration into a valid single access token.

The processor generates one access layer for each dependency parameter and links those layers through normal Java inheritance or an equivalent nominal type structure.

The final generated access type exposes every accessor and is registered through normal `@DelegatesTo` and metadata behavior.

Conceptually:

```text
Access1 -> playerData()
Access2 extends Access1 -> economyService()
Access3 extends Access2 -> rewardAuditLogger()
```

The consumer is lowered to:

```java
DependencyAccess<FinalGeneratedAccess>
```

`DependencyAccess<ACCESS>#getInstance()` resolves `ACCESS` through `IDependencyMap`.

Generated access instances may be stable. Their dependency getters must resolve live from the map and must not cache dependency instances.

## Accessor Naming

Generated accessor names derive from simple type names:

```text
IPlayerData -> playerData()
IEconomyService -> economyService()
PlayerProfileCache -> playerProfileCache()
```

Rules:

1. Strip leading `I` only when followed by an uppercase letter.
2. Lowercase the first remaining character.
3. Preserve the remainder of the simple type name.
4. Reject duplicate dependency types.
5. Reject accessor-name collisions.
6. Reject an incompatible existing or inherited method with the generated name.

The processor must not create numbered names to conceal collisions.

## Invalid Expanded Declarations

Reject:

- raw `DependencyAccess`
- wildcard parameters
- unresolved type variables that cannot produce class tokens
- duplicate direct dependency types
- generated accessor-name collisions
- expansion on a class without `@DelegatesTo`
- generated access types that cannot be registered or resolved

# Authored Domain Bundles

A one-parameter declaration is not expanded:

```java
DependencyAccess<PlayerRewardDependencies>
```

Bundles are ordinary domain DI contracts:

```java
public interface PlayerRewardDependencies {

    IPlayerData playerData();

    IEconomyService economyService();

    IRewardAuditLogger rewardAuditLogger();
}
```

The bundle implementation is a normal managed concrete:

```java
@DelegatesTo(PlayerRewardDependencies.class)
public final class DefaultPlayerRewardDependencies
        implements PlayerRewardDependencies, IDependencyAccess {
}
```

The bundle:

- receives normal metadata
- receives normal lifecycle behavior
- may be replaced through normal metadata replacement
- resolves its member dependencies through `IDependencyMap`
- may be reused by several consumers
- may define authored names that avoid generated-name collisions

Bundles must not require reflective record hydration or a bundle-specific factory.

# Lifecycle and Replacement

Metadata owns creation, initialization, lifecycle callbacks, and replacement.

The final replacement flow is:

```text
replacement request
    -> mapped metadata
    -> validate supplier result
    -> initialize replacement
    -> update metadata-owned instance
    -> future map lookups observe replacement
```

Generated accessors and authored bundle methods perform live map lookup. They therefore observe the current dependency instance after replacement.

Field-cached dependencies intentionally retain the instance captured at construction time and are governed by the access-style guide.

Replacement must preserve shared-token identity: every token mapped to one metadata object observes the same replacement instance.

# Source Lowering and Processing

The feature requires pre-Javac source lowering because ordinary JSR 269 processing cannot mutate an already parsed class signature.

The build must run lowering before Java compilation.

The source lowerer owns:

- identifying `DependencyAccess<...>` declarations
- distinguishing one-parameter and expanded modes
- validating source-level declaration shape
- rewriting the consumer to its final access token
- preserving unrelated annotations, interfaces, imports, and source structure

The annotation processor owns:

- generating access layers
- generating typed map-backed getters
- applying the generated delegation declaration
- producing deterministic compile diagnostics
- avoiding duplicate output across rounds

Runtime grant annotations and generated metadata companions are not required when no runtime feature consumes them.

# Migration and Deprecated Surfaces

## Planned Removal

Remove after downstream migration confirms there are no required consumers:

- `IDependencyInjectableConcrete`
- `IDependencyInjectableInterface`
- `DependencyBundleFactory`
- `IDependencyBundleFactory`
- `IDependencyBundleAccess`
- `IDependencyBundle`
- reflective bundle hydration tests and examples

## Planned Simplification

Review and remove when no runtime consumer remains:

- `@GrantDependencyAccess`
- `@GrantedDependencyAccess`
- `@GrantedDependencyAccesses`
- grant metadata companions
- runtime grant handlers and emitters

## Compatibility

- `@DelegatesToInterface` may remain deprecated while consumers migrate to `@DelegatesTo`.
- `requireInstance` may remain as a deprecated alias while `getInstance` becomes canonical.
- `DependencyLoader` may remain for tests, named scopes, and compatibility.
- Compatibility surfaces must delegate to the same metadata-owned instance model.

# Cross-System Integration

Tavall DI exposes:

- annotation-driven dependency registration
- token-based instance lookup
- metadata inspection
- lifecycle and replacement behavior
- generated or bundled typed access

Consumer systems own:

- which domain interfaces exist
- which bundles group them
- when platform hooks call DI-managed behavior
- whether a dependency is required or optional
- domain-specific failure recovery

Tavall DI must remain usable by Java-only modules and platform adapters without importing those platforms.

# Failure Rules

## Registration Failures

Fail clearly for:

- missing delegated tokens
- unassignable delegated tokens
- conflicting token ownership that cannot be resolved deterministically
- null metadata passed through named registration APIs
- an instance or replacement that cannot satisfy its token

## Lookup Failures

`getInstance` must include the requested token in its diagnostic.

`findInstance` may return `null` for intentional nullable lookup.

A mapped token whose metadata cannot expose a compatible instance is a broken binding, not a normal missing dependency.

## Processing Failures

Compile diagnostics must identify:

- the consumer class
- the invalid dependency parameter
- the generated accessor name when relevant
- the conflicting declaration or method
- the corrective action when a bundle can resolve the ambiguity

## Lifecycle Failures

Failed initialization or replacement must not publish an uninitialized replacement as the active metadata-owned instance.

Diagnostics must preserve the owning token, concrete type, and lifecycle phase.

## Direct Map Mutation

Direct inherited map mutation is allowed and may bypass lifecycle guarantees. Tavall DI does not pretend otherwise.

Named lookup must still fail clearly when direct mutation leaves a token mapped to incompatible or incomplete metadata.

# Production Verification

## Current Production Foundations

| Foundation | Evidence |
|---|---|
| Multi-token delegation annotation | [`DelegatesTo.java`](../src/main/java/org/tavall/dependency/annotations/DelegatesTo.java) |
| Authoritative concurrent metadata map | [`DependencyMap.java`](../src/main/java/org/tavall/dependency/maps/DependencyMap.java) |
| Public map contract | [`IDependencyMap.java`](../src/main/java/org/tavall/dependency/maps/interfaces/IDependencyMap.java) |
| Metadata ownership and lifecycle contract | [`IDependencyMetaData.java`](../src/main/java/org/tavall/dependency/metadata/interfaces/IDependencyMetaData.java) |
| Shared access contract | [`IDependencyAccess.java`](../src/main/java/org/tavall/dependency/IDependencyAccess.java) |
| Existing source-lowering foundation | [`DependencyAccessSourceLowerer.java`](../src/main/java/org/tavall/dependency/access/DependencyAccessSourceLowerer.java) |
| Existing processor foundation | [`DependencyAccessGrantProcessor.java`](../src/main/java/org/tavall/dependency/processor/DependencyAccessGrantProcessor.java) |

## Missing Required Behavior

The approved final contract is not complete until production code provides:

- metadata `getInstance(Class<T>)`
- map `getInstance(Class<T>)`
- map-backed `IDependencyAccess`
- annotation-only scanner eligibility
- unbounded expanded lowering and generation
- single-parameter non-expanded access
- generated final access registration
- generated live getters
- collision diagnostics
- removal or migration of the reflective bundle stack
- removal or migration of injectable marker requirements
- build integration that runs lowering before Javac

## Required Validation

Tests must prove:

- one-parameter bundle access
- two, three, four, five, and ten-or-more expanded parameters
- exact generated accessor names
- duplicate and collision diagnostics
- all delegated tokens share metadata and instance identity
- map lookup returns the metadata-owned instance
- replacement is visible through every token and generated getter
- generated access does not cache dependencies
- generated code does not use production loader lookup
- direct inherited map operations remain available
- invalid named registrations and replacements fail clearly
- downstream Tavall consumers compile after marker, bundle, and loader migration

A source file existing is not production verification. The relevant build and tests must pass against the active branch.
