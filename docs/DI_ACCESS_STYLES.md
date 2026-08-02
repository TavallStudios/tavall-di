# Tavall DI Access Styles

## Purpose

This document defines the production dependency-access patterns for Tavall DI.

The runtime model is intentionally small:

- `@DelegatesTo` declares DI-managed concretes and the tokens that resolve to them.
- `DependencyMap` is the authoritative token-to-metadata map.
- `DependencyMetaData` owns the actual dependency instance.
- `DependencyAccess<...>` is the typed authoring surface.
- Generated code exists only to satisfy Java's fixed generic-arity rules.

Access code must resolve the instance already stored in metadata. It must not rebuild dependencies, hydrate reflective records, or introduce a second container.

---

## Canonical Runtime Model

The map shape is:

```java
Class<?> -> IDependencyMetaData<?, ?>
```

Multiple delegated tokens may point to the same metadata object:

```java
IPlayerData.class -> metadata(PlayerData instance)
PlayerData.class  -> metadata(PlayerData instance)
```

`DependencyMap#getInstance(Class<T>)` must:

1. Find metadata for the supplied token.
2. Ask that metadata for the stored instance exposed through that token.
3. Return the same metadata-owned instance.
4. Fail clearly when the token is not registered or the stored instance is incompatible.

Conceptually:

```java
public <T> T getInstance(Class<T> dependencyType) {
    IDependencyMetaData<?, ?> metadata = get(dependencyType);
    if (metadata == null) {
        throw new IllegalStateException(
                "No dependency registered for " + dependencyType.getName()
        );
    }

    return metadata.getInstance(dependencyType);
}
```

The metadata contract should expose the typed instance lookup:

```java
<T> T getInstance(Class<T> dependencyType);
```

The instance lives in metadata. The map resolves metadata. Access layers only provide typed ergonomics over that path.

---

## `@DelegatesTo` Is the DI Declaration

A DI-managed implementation is declared with `@DelegatesTo`:

```java
@DelegatesTo(IPlayerData.class)
public final class PlayerData implements IPlayerData {
}
```

Multiple assignable tokens may delegate to the same concrete:

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

The annotation is sufficient to identify a managed concrete. Production classes and token interfaces do not need separate injectable marker interfaces.

The scanner and processor should reject declarations that cannot work, including:

- null or missing tokens
- a delegated token that is not assignable from the concrete
- conflicting delegation annotations
- metadata that cannot expose a compatible stored instance

The system does not need to forbid deliberate direct map access merely because it is dangerous. Tavall code should make the correct path obvious while leaving experienced developers room to operate.

---

# `DependencyAccess` Modes

`DependencyAccess` supports two authoring modes:

1. A single authored domain bundle.
2. Two to four directly expanded dependency types.

After four direct dependencies, introduce a domain bundle.

---

## Mode 1: Authored Domain Bundle

A declaration with one type parameter is treated as a bundle token:

```java
DependencyAccess<PlayerRewardDependencies>
```

The processor does not expand a single type parameter. `getInstance()` resolves the supplied bundle token through `DependencyMap`.

### Bundle Contract

Bundles are normal domain DI contracts. They are not reflective records and do not use a separate bundle factory.

```java
public interface PlayerRewardDependencies {

    IPlayerData playerData();

    IEconomyService economyService();

    IRewardAuditLogger rewardAuditLogger();

    IPlayerRewardResultBuilder rewardResultBuilder();
}
```

### Bundle Implementation

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

### Bundle Consumer

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

    @Override
    public PlayerRewardResult rewardPlayer(PlayerRewardRequest request) {
        getPlayerData().addReward(request.playerId(), request.amount());
        getEconomyService().recordTransaction(request.amount());
        return getInstance().rewardAuditLogger().auditReward(request);
    }
}
```

### Use a Bundle When

- The class requires more than four dependencies.
- The dependency group represents a reusable domain boundary.
- Several behaviors share the same dependency contract.
- The dependency set deserves explicit naming and documentation.

A bundle should be domain-specific. Avoid generic bags such as `CommonDependencies` that gradually become a storage unit for unrelated services.

---

## Mode 2: Expanded Direct Dependencies

For two to four dependencies, the class may declare them directly:

```java
@DelegatesTo(IPlayerRewardHandler.class)
public final class PlayerRewardHandler
        implements IPlayerRewardHandler,
                   DependencyAccess<
                           IPlayerData,
                           IEconomyService,
                           IRewardAuditLogger,
                           IPlayerRewardResultBuilder
                   > {

    private IPlayerData getPlayerData() {
        return getInstance().playerData();
    }

    private IEconomyService getEconomyService() {
        return getInstance().economyService();
    }
}
```

### Processor Activation

`@DelegatesTo` is the processor entrypoint.

When the processor finds an annotated concrete implementing `DependencyAccess`:

- one type parameter means bundle mode and requires no expansion
- two or more type parameters activate expansion
- parameters after the first produce the additional generated access types required by Java
- the generated access surface delegates every getter to `DependencyMap#getInstance(Class<?>)`
- generated runtime instances, when required by the lowered shape, are registered through the normal `@DelegatesTo` and metadata path

Generated code must not maintain a second instance cache. Every generated getter resolves the metadata-owned instance from the map.

### Expansion Limit

Use direct expansion for at most four dependencies:

```java
DependencyAccess<A, B>
DependencyAccess<A, B, C>
DependencyAccess<A, B, C, D>
```

For five or more dependencies, author a domain bundle:

```java
DependencyAccess<PlayerRewardDependencies>
```

The four-dependency limit is a style boundary, not an excuse to build a hostile runtime guard. Tooling may warn when the direct form exceeds four, while review should move the class to a domain bundle.

---

# Recommended Production Pattern

## One to Four Dependencies

Use expanded `DependencyAccess` with handler-local getters when dependencies repeat:

```java
@DelegatesTo(IPlayerRewardHandler.class)
public final class PlayerRewardHandler
        implements IPlayerRewardHandler,
                   DependencyAccess<IPlayerData, IEconomyService> {

    private IPlayerData getPlayerData() {
        return getInstance().playerData();
    }

    private IEconomyService getEconomyService() {
        return getInstance().economyService();
    }
}
```

The local getters do not cache dependencies. They give readable names to live map-backed lookups.

## More Than Four Dependencies

Create a domain bundle and consume it through a single type parameter:

```java
DependencyAccess<PlayerRewardDependencies>
```

## One-Off Infrastructure Lookup

Direct map access is valid when it is clearer than introducing a dependency-access declaration:

```java
IPlayerData playerData =
        DependencyMap.getDependencyMap().getInstance(IPlayerData.class);
```

Use the smallest pattern that keeps the dependency boundary understandable.

---

# Production Ranking

| Rank | Style | Use |
|---:|---|---|
| 1 | Expanded `DependencyAccess` with named getters | Default for two to four dependencies. |
| 2 | Domain bundle through `DependencyAccess<Bundle>` | Default after four dependencies or for reusable domains. |
| 3 | Direct expanded access | Good for small methods where `getInstance().dependency()` is already clear. |
| 4 | Direct `DependencyMap#getInstance` | Good for infrastructure, bootstrap, and isolated lookups. |
| 5 | Metadata access | Framework, lifecycle, diagnostics, and tests. |
| 6 | Cached dependency fields | Only when construction-time capture is intentional. |

---

# Map Mutation Policy

`DependencyMap` may continue extending `ConcurrentHashMap`.

The framework should not hide every sharp edge from developers. Direct mutation remains possible for infrastructure, testing, recovery, and advanced runtime work.

The framework should still reject operations that are structurally invalid and would immediately corrupt resolution, including:

- null tokens
- null metadata
- incompatible token and instance types
- replacement instances that cannot satisfy the registered token
- generated accessors whose target token cannot resolve

Normal application code should prefer the named DI methods because they preserve metadata and lifecycle behavior. Direct `put`, `remove`, or `clear` calls are intentional low-level operations and carry the corresponding responsibility.

---

# Deprecated or Removed Access Paths

The following are not part of the preferred production design:

- `IDependencyInjectableConcrete`
- `IDependencyInjectableInterface`
- reflective `DependencyBundleFactory`
- `IDependencyBundleFactory`
- reflective `IDependencyBundleAccess`
- record hydration as dependency composition
- production lookup through `DependencyLoaderAccess`

`DependencyLoader` and static loader access may remain for tests or compatibility while consumers migrate. Production dependency access should resolve through `IDependencyMap` and the metadata-owned instance.

---

# Testing Requirements

## Delegation

Test that:

- every `@DelegatesTo` token resolves
- all tokens delegated by one concrete return the same stored instance
- concrete-class tokens and interface tokens share metadata identity
- invalid assignability fails clearly

## Metadata Ownership

Test that:

- `DependencyMap#getInstance` returns the instance stored in metadata
- replacement updates the metadata-owned instance
- every delegated token sees the replacement
- lookup never constructs a second instance

## Bundle Mode

Test that:

- `DependencyAccess<Bundle>` does not activate expansion
- `getInstance()` resolves the authored bundle from the map
- bundle getters return live metadata-owned dependencies

## Expansion Mode

Test that:

- two, three, and four direct dependencies generate successfully
- generated getters use `DependencyMap#getInstance`
- the first parameter establishes the access root and later parameters expand it
- missing dependencies fail at the getter that requests them
- generated access does not cache instances separately

## Direct Mutation

Test that supported low-level map mutations remain possible and that obviously invalid token-instance combinations are rejected.
