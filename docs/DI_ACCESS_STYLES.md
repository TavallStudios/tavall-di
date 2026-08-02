# Tavall DI Access Styles

## Purpose

This document ranks the supported Tavall DI access styles and explains when each style belongs in production code, tests, bootstrap code, or framework internals.

It is an access-style guide. The authoritative runtime contract lives in [`TAVALL_DI_SYSTEM_FINAL.md`](TAVALL_DI_SYSTEM_FINAL.md).

The core rule is:

> `DependencyMap` and its mapped `IDependencyMetaData` remain the source of truth. Access styles are typed ergonomic layers over the same metadata-owned instances.

No access style may create a second container, rebuild dependencies, or maintain a competing instance cache.

---

## Current Direction

The target access model uses these core surfaces:

- `@DelegatesTo`
- `DependencyAccess<...>`
- `IDependencyAccess`
- `IDependencyMap`
- `DependencyMap`
- `IDependencyMetaData`
- generated access types for expanded generic declarations
- authored domain bundles for grouped dependency boundaries
- `DependencyLoader` only for tests, scoped fixtures, and compatibility during migration

`IDependencyInjectableConcrete` and `IDependencyInjectableInterface` are not part of the target production style. `@DelegatesTo` is sufficient to declare a managed concrete and its tokens.

---

## Access Resolution Rule

Every normal production lookup resolves through the real map:

```java
DependencyMap.getDependencyMap().getInstance(IPlayerData.class);
```

The map finds the metadata mapped to the token, and metadata returns the instance it owns:

```text
dependency token
    -> DependencyMap
    -> IDependencyMetaData
    -> metadata-owned instance
```

Multiple delegated tokens may point to the same metadata object:

```text
IPlayerData.class
IPlayerWallet.class
PlayerData.class
    -> same metadata
    -> same PlayerData instance
```

Generated accessors and authored bundles must use this path. They must not use `DependencyLoaderAccess` for production lookup.

---

## `DependencyAccess` Modes

`DependencyAccess` has two authoring modes.

### Single-Parameter Mode

```java
DependencyAccess<PlayerRewardDependencies>
```

One parameter is not expanded. It is resolved directly and is normally an authored domain bundle.

A normal single dependency token is technically valid as well, although direct map access may be clearer when no access surface is needed.

### Expanded Mode

```java
DependencyAccess<
        IPlayerData,
        IEconomyService,
        IRewardAuditLogger
>
```

Two or more parameters activate generated expansion.

The processor supports arbitrary arity. Four direct dependencies is an architecture preference, not a compiler limit:

- Two to four direct types are the normal expanded style.
- Five or more direct types remain valid.
- Review should prefer a domain bundle when the larger set forms a coherent boundary.

The framework should not confuse style guidance with a technical incapacity it does not have.

---

## Production Ranking Summary

| Rank | Style | Production Use | Why |
|---:|---|---|---|
| 1 | Expanded `DependencyAccess` + named getters | First-class | Best daily style for a small, readable dependency set. |
| 2 | Domain bundle + named getters | First-class | Best for larger or reusable domain boundaries. |
| 3 | Direct expanded or bundle access | First-class | Good when the method is already clear without local getters. |
| 4 | Direct `IDependencyMap#getInstance` | Supported | Best for infrastructure, one-off lookups, and framework composition. |
| 5 | Local method variable | Supported | Keeps method-scoped dependency use local. |
| 6 | Field-cached dependency | Conditional | Captures an instance and may not follow replacement. |
| 7 | Direct metadata access | Framework/Internal | Lifecycle, diagnostics, verification, graphing, and replacement tooling. |
| 8 | Wrapped interface access | Framework/Internal | Binding and wrapper verification. |
| 9 | Wrapped concrete access | Framework/Internal | Concrete identity and lifecycle tooling. |
| 10 | `DependencyLoader` access | Test/Compatibility | Scoped fixtures and legacy migration, not normal production lookup. |
| 11 | Optional lookup | Boundary/Test | Only for dependencies that are genuinely optional. |

---

## Test Ranking Summary

| Rank | Style | Test Use | Why |
|---:|---|---|---|
| 1 | Map or loader fixture registration | First-class setup | Creates deterministic test bindings and scopes. |
| 2 | Expanded `DependencyAccess` | First-class target | Verifies generated access and live map resolution. |
| 3 | Domain bundle access | First-class target | Verifies authored dependency boundaries. |
| 4 | Direct map lookup | First-class target | Verifies the primitive all higher styles use. |
| 5 | Metadata access | Strong target | Verifies ownership, identity, lifecycle, and replacement. |
| 6 | Wrapper access | Strong target | Verifies interface and concrete wrapper correctness. |
| 7 | Replacement access | Required where supported | Proves live getters observe new metadata-owned instances. |
| 8 | Field-cached access | Edge case | Proves intentional construction-time capture. |
| 9 | Optional lookup | Missing-dependency tests | Verifies legitimate fallback behavior. |
| 10 | Scoped loader access | Compatibility/scoped tests | Prevents fixture pollution while loader scopes remain supported. |

---

# Style 1: Expanded `DependencyAccess` with Named Getters

## Production Rank

**#1: Default production style for a small dependency set.**

## Shape

```java
@DelegatesTo(IPlayerRewardHandler.class)
public final class PlayerRewardHandler
        implements IPlayerRewardHandler,
                   DependencyAccess<
                           IPlayerData,
                           IEconomyService,
                           IRewardAuditLogger
                   > {

    private IPlayerData getPlayerData() {
        return getInstance().playerData();
    }

    private IEconomyService getEconomyService() {
        return getInstance().economyService();
    }

    private IRewardAuditLogger getRewardAuditLogger() {
        return getInstance().rewardAuditLogger();
    }

    @Override
    public void handlePlayerReward(long amount) {
        getPlayerData().addCoins(amount);
        getEconomyService().recordTransaction(amount);
        getRewardAuditLogger().recordReward(amount);
    }
}
```

## Runtime Meaning

The authored generic declaration is lowered to the final generated access type. `getInstance()` resolves that generated access object from `DependencyMap`. Each generated getter resolves its dependency through `DependencyMap#getInstance(Class<?>)`.

The handler does not receive a handler-specific zero-argument `getInstance()` implementation. The method belongs to the lowered `DependencyAccess<ACCESS>` contract.

## Why It Wins

- The dependency boundary is visible on the class declaration.
- Business methods use meaningful names.
- No dependency fields are required.
- Every call can observe the current metadata-owned instance.
- Generated code solves Java's generic-arity problem without becoming a second runtime.

## Use When

- The class has a small dependency set.
- Dependencies are used across several methods.
- Direct dependency names are clearer than introducing a domain bundle.
- Replacement or reload should be visible through future getter calls.

## Style Boundary

Two to four direct dependencies is the preferred range.

More than four remains supported. Introduce a bundle when the dependency set represents one domain boundary or the declaration becomes harder to understand than the behavior it supports.

## Test Coverage Needed

- two, three, four, five, and ten-or-more direct parameters compile
- generated getter names are correct
- getters return metadata-owned instances
- replacement is visible without recreating the consumer
- no generated getter calls `DependencyLoaderAccess`
- duplicate generated names fail clearly

---

# Style 2: Domain Bundle with Named Getters

## Production Rank

**#2: Default production style for larger or reusable dependency boundaries.**

## Bundle Contract

Bundles are normal DI contracts, not reflective records hydrated by a special factory:

```java
public interface PlayerRewardDependencies {

    IPlayerData playerData();

    IEconomyService economyService();

    IRewardAuditLogger rewardAuditLogger();

    IPlayerRewardResultBuilder rewardResultBuilder();
}
```

## Bundle Implementation

```java
@DelegatesTo(PlayerRewardDependencies.class)
public final class DefaultPlayerRewardDependencies
        implements PlayerRewardDependencies, IDependencyAccess {

    @Override
    public IPlayerData playerData() {
        return getDependencyMap().getInstance(IPlayerData.class);
    }

    @Override
    public IEconomyService economyService() {
        return getDependencyMap().getInstance(IEconomyService.class);
    }

    @Override
    public IRewardAuditLogger rewardAuditLogger() {
        return getDependencyMap().getInstance(IRewardAuditLogger.class);
    }

    @Override
    public IPlayerRewardResultBuilder rewardResultBuilder() {
        return getDependencyMap().getInstance(IPlayerRewardResultBuilder.class);
    }
}
```

## Consumer

```java
@DelegatesTo(IPlayerRewardHandler.class)
public final class PlayerRewardHandler
        implements IPlayerRewardHandler,
                   DependencyAccess<PlayerRewardDependencies> {

    private IPlayerData getPlayerData() {
        return getInstance().playerData();
    }

    private IEconomyService getEconomyService() {
        return getInstance().economyService();
    }
}
```

## Why It Wins

The bundle names a coherent domain boundary and can be reused by several behaviors. It is registered, instantiated, replaced, and resolved through the same metadata path as every other dependency.

There is no:

- `DependencyBundleFactory`
- reflective record construction
- bundle-only instance cache
- alternate bundle lifecycle

## Use When

- The class usually needs more than four dependencies.
- Several classes share the same dependency boundary.
- The dependency group deserves a stable domain name.
- Explicit authored component names avoid generated-name collisions.
- The bundle itself benefits from targeted tests or documentation.

## Avoid When

- The bundle would contain unrelated services.
- The class has only a couple of obvious dependencies.
- The bundle exists solely to satisfy a style number without improving the domain model.

## Test Coverage Needed

- one-parameter `DependencyAccess<Bundle>` does not activate expansion
- `getInstance()` returns the metadata-owned bundle
- bundle getters resolve current metadata-owned dependencies
- bundle replacement follows normal DI replacement behavior
- no reflective bundle factory is invoked

---

# Style 3: Direct Expanded or Bundle Access

## Production Rank

**#3: First-class for compact methods.**

## Expanded Shape

```java
getInstance().playerData().addCoins(amount);
getInstance().economyService().recordTransaction(amount);
```

## Bundle Shape

```java
getInstance().playerData().addCoins(amount);
getInstance().rewardAuditLogger().recordReward(amount);
```

The call site is identical. The declaration decides whether `getInstance()` returns a generated access type or an authored bundle.

## Use When

- The method is short.
- The dependency getter already reads clearly.
- Adding a private getter would merely repeat the generated or authored component name.

## Avoid When

- Long chains obscure business logic.
- Several methods repeatedly use the same dependencies.
- A local name communicates domain intent better.

---

# Style 4: Direct Map Lookup

## Production Rank

**#4: Supported production primitive.**

## Shape

```java
IPlayerData playerData =
        DependencyMap.getDependencyMap().getInstance(IPlayerData.class);
```

Or through `IDependencyAccess`:

```java
IPlayerData playerData =
        getDependencyMap().getInstance(IPlayerData.class);
```

## Use When

- Infrastructure needs a one-off dependency.
- A generated access declaration would add more ceremony than clarity.
- Framework code is composing metadata or runtime services.
- Bootstrap code needs the authoritative map directly.

## Avoid When

- Repeated class-token calls hide a stable dependency boundary.
- A handler would become easier to read with expanded access or a bundle.
- The caller is bypassing metadata APIs to manipulate raw mapped values unintentionally.

Direct map mutation remains available because `DependencyMap` extends `ConcurrentHashMap`. Prefer named APIs when metadata and lifecycle coherence matter. Raw `put`, `remove`, `compute`, and `clear` are deliberate low-level operations, not forbidden magic.

---

# Style 5: Local Method Variables

## Production Rank

**#5: Supported for method-scoped use.**

```java
public void handlePlayerReward(long amount) {
    IPlayerData playerData =
            getDependencyMap().getInstance(IPlayerData.class);
    IEconomyService economyService =
            getDependencyMap().getInstance(IEconomyService.class);

    playerData.addCoins(amount);
    economyService.recordTransaction(amount);
}
```

## Use When

- Dependencies are used only in one method.
- A local name improves readability.
- The method intentionally captures the current instance once.

## Tradeoff

A local variable observes the instance at lookup time. A later replacement during the same method does not alter the local reference.

---

# Style 6: Field-Cached Dependencies

## Production Rank

**#6: Conditional.**

```java
public final class PlayerRewardHandler implements IDependencyAccess {

    private final IPlayerData playerData =
            getDependencyMap().getInstance(IPlayerData.class);
}
```

## Use When

- Construction occurs after DI hydration.
- The dependency graph is stable.
- Construction-time capture is intentional.
- A measured hot path justifies avoiding repeated lookup.

## Avoid When

- Replacement, reload, or retry is supported.
- The object may be created before dependency initialization.
- Tests expect live replacement semantics.

A field caches the instance. Expanded accessors and bundle methods normally resolve live from the map.

---

# Style 7: Direct Metadata Access

## Production Rank

**#7: Framework and internal tooling.**

```java
IDependencyMetaData<?, ?> metadata =
        DependencyMap.getDependencyMap().findMetaData(IPlayerData.class);
```

Metadata access is appropriate for:

- lifecycle execution
- replacement
- graph inspection
- diagnostics
- concrete/interface identity checks
- source context
- role, depth, priority, and retry state
- testing that multiple tokens share metadata identity

Normal domain behavior should request the dependency instance, not conduct a full autopsy on its registration record.

---

# Style 8: Wrapped Interface Access

## Production Rank

**#8: Framework and verification.**

Use the interface wrapper when wrapper identity, declared token information, or interface-side metadata matters.

Avoid wrapper chains in ordinary feature code when `getInstance(IType.class)` already supplies the required API.

---

# Style 9: Wrapped Concrete Access

## Production Rank

**#9: Framework and verification.**

Use the concrete wrapper when:

- lifecycle tooling needs the concrete type
- tests assert concrete identity
- a framework operation intentionally requires concrete-only behavior

Concrete access should not quietly become the default because someone discovered an implementation method that was easier than fixing the interface.

---

# Style 10: `DependencyLoader` Access

## Production Rank

**#10: Tests, scopes, and compatibility.**

`DependencyLoader` and `DependencyLoaderAccess` may remain while scoped tests and legacy consumers migrate.

## Valid Uses

```java
DependencyLoaderAccess.registerInstance(
        IPlayerData.class,
        new TestPlayerData(),
        DependencySource.TEST
);
```

```java
DependencyLoaderAccess.clear("test-scope");
```

Use loader access for:

- test fixture registration
- named test scopes
- legacy compatibility
- migration bridges
- loader-specific behavior that has not yet moved to the map

Generated production accessors, authored bundles, and normal handlers should resolve through `IDependencyMap`.

---

# Style 11: Optional Lookup

## Production Rank

**#11: Boundary and tests only.**

Optional lookup is valid when absence is part of the contract:

- an optional module is not installed
- a platform adapter may be unavailable
- a fallback is intentional
- a test is asserting missing state

Do not turn required dependencies into optional values to hide boot failures. Missing required dependencies should fail at the lookup boundary with the token and owning access type in the diagnostic.

---

# `@DelegatesTo` Registration Style

`@DelegatesTo` is the authoritative declaration for managed concretes:

```java
@DelegatesTo(IPlayerData.class)
public final class PlayerData implements IPlayerData {
}
```

Multiple assignable tokens may share the same metadata and instance:

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

The annotation replaces production reliance on:

- `IDependencyInjectableConcrete`
- `IDependencyInjectableInterface`
- `@DelegatesToInterface` for new code

The deprecated annotation may remain temporarily as a compatibility alias.

Registration must reject:

- missing tokens
- tokens not assignable from the concrete
- incompatible stored or replacement instances
- conflicting declarations that cannot produce deterministic ownership

---

# Accessor Naming

Generated accessor names derive from type names:

| Dependency Type | Generated Accessor |
|---|---|
| `IPlayerData` | `playerData()` |
| `IEconomyService` | `economyService()` |
| `IRewardAuditLogger` | `rewardAuditLogger()` |
| `PlayerProfileCache` | `playerProfileCache()` |

Rules:

1. Strip a leading `I` only when it is followed by an uppercase letter.
2. Lowercase the first remaining character.
3. Preserve the rest of the simple type name.
4. Reject duplicate dependency types.
5. Reject generated-name collisions.

Do not silently append numbers such as `playerData2()`. A domain bundle allows the developer to author meaningful distinct component names.

---

# Production Anti-Patterns

## Reflective Bundle Hydration

Do not create dependency records by reflecting over record components and invoking constructors. Bundles are ordinary DI-managed domain contracts.

## Generated Instance Caches

Generated accessors must not cache dependency instances separately from metadata.

## Production Loader Lookup

Do not generate:

```java
DependencyLoaderAccess.requireInstance(IPlayerData.class);
```

Generate map-backed access instead.

## Marker-Interface Registration

Do not require every token and concrete to implement injectable marker interfaces when `@DelegatesTo` already declares the binding.

## Optional Required Dependencies

Do not silently skip behavior because a required dependency was looked up optionally.

## Accidental Field Capture

Do not use dependency fields unless stale-after-replacement behavior is intentional and tested.

---

# Production/Test Matrix

| Style | Production | Tests | Notes |
|---|---:|---:|---|
| Expanded access + getters | Excellent | Excellent | Default small-set style. |
| Domain bundle + getters | Excellent | Excellent | Default larger/reusable boundary. |
| Direct expanded/bundle access | Excellent | Excellent | Best for compact methods. |
| Direct map lookup | Good | Excellent | Authoritative primitive. |
| Local variables | Good | Good | Method-scoped capture. |
| Field-cached dependency | Conditional | Edge case | Prove capture semantics. |
| Direct metadata | Internal | Excellent | Ownership and lifecycle assertions. |
| Wrapped interface | Internal | Excellent | Binding verification. |
| Wrapped concrete | Internal | Excellent | Concrete/lifecycle verification. |
| Loader access | Compatibility | Excellent | Fixtures, scopes, migration. |
| Optional lookup | Boundary only | Good | Absence must be legitimate. |

---

# Official Guidance

## Production First-Class Styles

1. Expanded `DependencyAccess` with named getters.
2. Domain bundle through `DependencyAccess<Bundle>`, usually with named getters.
3. Direct expanded or bundle access for compact methods.
4. Direct map lookup for infrastructure and isolated calls.

## Conditional Styles

5. Local method variables.
6. Field-cached dependencies.

## Framework/Internal Styles

7. Direct metadata access.
8. Wrapped interface access.
9. Wrapped concrete access.

## Test/Compatibility Styles

10. Loader registration and scoped access.
11. Optional lookup.
12. Replacement and direct metadata assertions.

The style guide recommends a bundle after roughly four direct dependencies because that is usually where a domain boundary becomes visible. The processor remains unbounded because architecture guidance belongs in review, not in a fabricated compiler limitation.
