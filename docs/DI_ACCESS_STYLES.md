# Tavall DI Access Styles

## Purpose

This document ranks the dependency access styles available in the current Tavall DI/runtime metadata system and explains which styles should be used in production code versus tests.

The system already has the real core pieces:

- `DependencyMetaData<INTERFACE, INSTANCE>`
- `IDependencyMetaData<INTERFACE, INSTANCE>`
- `IDependencyInterface<INTERFACE>`
- `IDependencyInstance<INSTANCE>`
- `DependencyLoaderAccess`
- scoped and default dependency loaders
- `@DelegatesTo`
- `DependencySource`
- `IContext<INTERFACE>`
- class-token based dependency lookup
- interface-first binding
- dependency metadata as the mapped value

The important design rule is simple:

> The metadata map stays the source of truth. Access styles are just ergonomic layers over the same registered dependency metadata.

No competing container. No renamed architecture. No “surprise, it’s Spring now” nonsense wearing a Tavall hoodie.

---

## Existing Static Loader Context

`DependencyLoaderAccess` provides static access to the active dependency loader and supports both default-scope and named-scope operations.

The important available operations are:

```java
DependencyLoaderAccess.findInstance(IType.class);
DependencyLoaderAccess.findOptionalInstance(IType.class);
DependencyLoaderAccess.requireInstance(IType.class);
DependencyLoaderAccess.findMetaData(IType.class);
DependencyLoaderAccess.isInstanceRegistered(IType.class);

DependencyLoaderAccess.registerInstance(IType.class, instance);
DependencyLoaderAccess.registerInstance(IType.class, supplier);
DependencyLoaderAccess.registerInstance(IType.class, instance, DependencySource.TEST);
DependencyLoaderAccess.registerInstance(IType.class, supplier, DependencySource.RUNTIME);

DependencyLoaderAccess.replaceInstance(IType.class, Supplier);
DependencyLoaderAccess.clear();
```

Scoped variants also exist:

```java
DependencyLoaderAccess.findInstance("test-scope", IType.class);
DependencyLoaderAccess.requireInstance("test-scope", IType.class);
DependencyLoaderAccess.findMetaData("test-scope", IType.class);
DependencyLoaderAccess.registerInstance("test-scope", IType.class, instance);
DependencyLoaderAccess.replaceInstance("test-scope", IType.class, Supplier);
DependencyLoaderAccess.clear("test-scope");
```

This means the DI system already has a clean static bootstrap/test/runtime access layer. Production code can still prefer composed access interfaces and bundles, while tests can use static registration/clear operations directly for fixture setup.

---

## Core Design Model

### Metadata Map

The map shape is effectively:

```java
Class<?> -> DependencyMetaData<INTERFACE, INSTANCE>
```

The key is usually an interface type:

```java
IPlayerData.class -> DependencyMetaData<IPlayerData, PlayerData>
```

The metadata value knows the interface side, instance side, lifecycle state, source context, role, retry state, source metadata, and underlying instance.

### Metadata Shape

Current metadata shape:

```java
public class DependencyMetaData<INTERFACE, INSTANCE>
        implements IDependencyMetaData<INTERFACE, INSTANCE> {
}
```

Important fields:

```java
private Set<INTERFACE> subDependencies = new HashSet<>();
private int priority;
private int depth;
private DependencyRole dependencyRole = DependencyRole.ISOLATED;
private final Map<LifecycleType, Method> lifecycleMethods = new EnumMap<>(LifecycleType.class);
private final Map<LifecycleType, Boolean> lifecycleSuccess = new EnumMap<>(LifecycleType.class);
private int retryCount;
private IContext<INTERFACE> sourceContext;
private Supplier<INSTANCE> dependencySupplier;
private IDependencyInterface<INTERFACE> wrappedInterface;
private IDependencyInstance<INSTANCE> wrappedInstance;
private Class<? extends INTERFACE> primaryInterfaceType;
private Class<? extends INSTANCE> concreteType;
private INSTANCE dependencyInstance;
```

That means access styles should not rebuild the dependency. They should route to the already-registered metadata and pull the correct typed side.

---

## Production Ranking Summary

| Rank | Style | Production Use | Why |
|---:|---|---|---|
| 1 | Dependency Bundle / Mini-Context | First-class | Defines the dependency contract as a reusable record. Best as the declared boundary for handlers/services. |
| 2 | Bundle + Named Getters | First-class | Uses the bundle contract, then adds handler-local getter names for cleaner business logic. Best implementation style for larger handlers. |
| 3 | Named Getter Lookup | First-class | Best everyday style for small and medium classes. |
| 4 | Primitive Lookup | Supported | Best for one-off calls or small utilities. Too noisy if overused. |
| 5 | Local Method Variable | Supported | Good when dependency is only needed inside one method. |
| 6 | Field-Cached Dependency | Conditional | Good for stable hot paths. Risky with replacement/reload behavior. |
| 7 | Direct Metadata Access | Framework/Internal | Use for lifecycle, concrete instance, verification, diagnostics, graphing. |
| 8 | Wrapped Interface Access | Framework/Internal | Use when wrapper identity or interface binding metadata matters. |
| 9 | Wrapped Instance Access | Framework/Internal | Use when concrete wrapper and concrete-only methods are intentionally needed. |
| 10 | Static Loader Direct Access | Bootstrap/Test/Infrastructure | Great for bootstrapping and tests. Avoid spraying it across domain code. |
| 11 | Optional Lookup | Boundary/Test | Useful for conditional behavior and assertions. Avoid hiding missing required deps in production. |

---

## Test Ranking Summary

| Rank | Style | Test Use | Why |
|---:|---|---|---|
| 1 | Static Loader Registration | First-class test setup | `registerInstance`, `clear`, scoped registration, and `DependencySource.TEST` make tests deterministic. |
| 2 | Dependency Bundle Access | First-class integration test target | Bundles are first-class production, so they need first-class tests. |
| 3 | Named Getter Lookup | First-class test target | Verifies normal service style. |
| 4 | Primitive Lookup | First-class test target | Verifies the base primitive all other styles depend on. |
| 5 | Direct Metadata Access | Strong test target | Validates metadata identity, lifecycle state, role, source context, concrete/interface mapping. |
| 6 | Wrapped Interface / Instance Access | Strong test target | Validates wrapper correctness and instance identity. |
| 7 | Replacement Access | Conditional | Test when replacement/reload is supported. |
| 8 | Field-Cached Access | Edge test | Should prove construction-time capture behavior. |
| 9 | Optional Lookup | Useful for missing-dependency tests | Good for fallback paths. |
| 10 | Scoped Loader Access | Required if scopes are used | Prevents cross-test pollution and proves scoped isolation. |

---

# Style 1: Dependency Bundle / Mini-Context

## Production Rank

**#1: First-class production dependency declaration style.**

Bundles should be the preferred way to declare the dependency set for handlers/services that need multiple dependencies.

This style is about the **dependency contract**, not necessarily the prettiest handler method body.

## Shape

```java
// This record is the mini-context for player reward logic.
// It declares every dependency the reward handler is allowed to use.
public record PlayerRewardDependencies(
        IPlayerData playerData,
        IEconomyService economyService
) implements IDependencyBundle {
}
```

```java
@DelegatesTo(IPlayerRewardHandler.class)
public final class PlayerRewardHandler
        implements IPlayerRewardHandler, IDependencyBundleAccess<PlayerRewardDependencies> {

    @Override
    public void handlePlayerReward(long amount) {
        // Direct bundle access is valid when the method is small
        // and the dependency names are already obvious.
        dependencies().playerData().addCoins(amount);
        dependencies().economyService().recordTransaction(amount);
    }
}
```

## Why It Wins

The bundle is a mini-context. It declares what the handler needs without forcing slot names like `dependencyOne()`.

```java
// The record component names become the dependency names.
dependencies().playerData();
dependencies().economyService();
```

This is readable, typed, and enforceable.

## Use When

- A handler/service needs several dependencies.
- You want the dependency boundary visible.
- You want validation tooling.
- You want dependency graphing.
- You want clean domain-level dependency contracts.
- You want to avoid scattered lookup calls everywhere.

## Avoid When

- The class only needs one dependency once.
- The bundle would have only one dependency and adds ceremony.
- The code is tiny bootstrapping glue.

## Test Coverage Needed

Test that:

- record components hydrate from the DI metadata map
- each component returns the registered interface-facing instance
- missing bundle component fails clearly
- non-record bundle types fail clearly
- bundle access works inside a handler
- bundle access works with named getters

# Style 2: Bundle + Named Getters

## Production Rank

**#2: First-class production implementation style.**

This style still uses the bundle from Style 1, but the handler adds small getters so business methods read cleanly.

This is the preferred handler implementation style when the class has multiple methods or repeated dependency usage.

## Shape

```java
@DelegatesTo(IPlayerRewardHandler.class)
public final class PlayerRewardHandler
        implements IPlayerRewardHandler, IDependencyBundleAccess<PlayerRewardDependencies> {

    private IPlayerData getPlayerData() {
        // This does not create or cache a dependency.
        // It only gives the bundle component a handler-local name.
        return dependencies().playerData();
    }

    private IEconomyService getEconomyService() {
        // Same dependency, same bundle, cleaner call site.
        return dependencies().economyService();
    }

    @Override
    public void handlePlayerReward(long amount) {
        // Business logic reads through named dependency getters.
        getPlayerData().addCoins(amount);
        getEconomyService().recordTransaction(amount);
    }

    @Override
    public long getPlayerCoins() {
        // No local variables or fields needed.
        return getPlayerData().getCoins();
    }
}
```

## Why It Wins

The bundle declares the dependency set:

```java
// The class declares its mini-context once.
IDependencyBundleAccess<PlayerRewardDependencies>
```

The getters give readable method bodies:

```java
// The getter names describe the dependency being accessed.
getPlayerData().addCoins(amount);
```

No dependency fields. No local dependency variables. No facade methods. No string invocation. Just naming the dependencies already inside the bundle.

## Use When

- A handler has several methods using the same dependencies.
- You want method bodies to read cleanly.
- You want the bundle as the formal dependency contract.
- You do not want cached dependency fields.
- You want `@DelegatesTo` to make the handler itself DI-managed.

## Avoid When

- The class uses each dependency once.
- The handler is so small that `dependencies().playerData()` is already clear.

## Test Coverage Needed

Test that:

- getters return bundle components
- getters expose real interface methods
- no dependency state is cached in fields
- replacement behavior follows bundle policy if replacement is supported

# Style 3: Named Getter Lookup

## Production Rank

**#3: First-class daily style.**

This is the best style for classes that do not need a formal bundle.

## Shape

```java
public final class PlayerRewardHandler implements IDependencyLookupAccess {

    private IPlayerData getPlayerData() {
        return dependency(IPlayerData.class);
    }

    private IEconomyService getEconomyService() {
        return dependency(IEconomyService.class);
    }

    public void handlePlayerReward(long amount) {
        getPlayerData().addCoins(amount);
        getEconomyService().recordTransaction(amount);
    }
}
```

## Why It Works

It keeps access readable while still using the metadata map.

```java
getPlayerData()
```

is just a meaningful name for:

```java
dependency(IPlayerData.class)
```

No method facades. No fields. No dependency constructor arguments. No “which slot was this again?” nonsense.

## Use When

- The class has a few dependencies.
- You want readable method bodies.
- You do not need a formal bundle record.
- Dependencies should resolve live from the map instead of being cached.

## Avoid When

- A class has a large dependency set.
- You want to enforce a strict dependency contract.
- You need dependency graph tooling from a bundle record.

## Test Coverage Needed

Test that:

- getter returns dependency from map
- getter can call real interface methods
- getter sees replacement if replacement is supported
- missing dependency fails clearly

---

# Style 4: Primitive Lookup

## Production Rank

**#4: Supported production style.**

This is the base primitive every other lookup style builds on.

## Shape

```java
dependency(IPlayerData.class).addCoins(500L);
dependency(IEconomyService.class).recordTransaction(500L);
```

## Why It Exists

It is flexible and minimal. It should be available everywhere a class has dependency lookup access.

## Use When

- You need a dependency once.
- You are writing small glue code.
- You are prototyping a handler before giving dependencies names.
- You are building another access layer.

## Avoid When

- Calls repeat often.
- The method becomes noisy with class tokens.
- The class has many dependencies.
- You want a formal dependency contract.

## Test Coverage Needed

Test that:

- dependency resolves the registered interface
- real methods can be called
- missing dependency fails clearly
- scoped lookup works when scope is provided

---

# Style 5: Local Method Variable

## Production Rank

**#5: Supported style.**

Local variable access is clean when a dependency is method-scoped.

## Shape

```java
public void handlePlayerReward(long amount) {
    IPlayerData playerData = dependency(IPlayerData.class);
    IEconomyService economyService = dependency(IEconomyService.class);

    playerData.addCoins(amount);
    economyService.recordTransaction(amount);
}
```

## Why It Exists

It keeps the lookup near usage and avoids class-level getter bloat.

## Use When

- The dependency is only used in one method.
- You want a readable local name.
- The method is a contained workflow.
- You do not need the dependency elsewhere in the class.

## Avoid When

- Several methods need the same dependency.
- The dependency is part of the handler’s core identity.
- You want a bundle contract.

## Test Coverage Needed

Test that:

- local variables resolve typed dependencies
- real methods work
- multiple local dependencies resolve correctly
- local variable captures the instance at lookup time

---

# Style 6: Field-Cached Dependency

## Production Rank

**#6: Conditional production style.**

Field-cached access is allowed, but it has lifecycle risk.

## Shape

```java
public final class PlayerRewardHandler implements IDependencyLookupAccess {

    private final IPlayerData playerData = dependency(IPlayerData.class);
    private final IEconomyService economyService = dependency(IEconomyService.class);

    public void handlePlayerReward(long amount) {
        playerData.addCoins(amount);
        economyService.recordTransaction(amount);
    }
}
```

## Why It Exists

It is short and fast. It avoids repeated map lookup.

## Main Tradeoff

A field captures the dependency at construction time.

A getter resolves from the map each time.

```java
private final IPlayerData playerData = dependency(IPlayerData.class);
```

means the field does not automatically follow replacement/reload unless the instance itself is a proxy or mutable wrapper.

## Use When

- Dependency graph is stable after boot.
- Handler is created after DI hydration.
- Dependency replacement is not expected.
- You are in a hot path and lookup overhead matters.

## Avoid When

- Dependency replacement/retry/reload is supported.
- Tests need replacement semantics.
- Handler may be constructed before DI hydration.
- You want all reads to reflect the current map.

## Test Coverage Needed

Test that:

- field can call real methods
- field captures construction-time instance
- replacement behavior is explicitly documented/tested
- construction before hydration fails clearly

---

# Style 7: Direct Metadata Access

## Production Rank

**#7: Framework/internal style.**

Metadata access is powerful, but normal domain logic should not live here.

## Shape

```java
DependencyMetaData<IPlayerData, PlayerData> playerDataMetaData =
        dependencyMetaData(IPlayerData.class);

playerDataMetaData.dependencyInstance().resetCoins();
```

## Why It Exists

Metadata access gives the full dependency record:

- interface type
- concrete type
- wrapped interface
- wrapped instance
- source context
- dependency role
- priority
- depth
- lifecycle methods
- lifecycle success state
- retry count
- supplier
- sub-dependencies

## Use When

- building dependency tooling
- validating graph state
- inspecting lifecycle state
- resolving concrete-only behavior intentionally
- debugging dependency source/context
- writing tests for metadata correctness

## Avoid When

- normal service logic only needs interface methods
- you are bypassing interface-first design for convenience
- concrete access becomes the default path

## Test Coverage Needed

Test that:

- metadata lookup returns mapped value
- primary interface type is correct
- concrete type is correct
- dependency instance identity is correct
- lifecycle maps are accessible
- source context is correct
- role/depth/priority are correct

---

# Style 8: Wrapped Interface Access

## Production Rank

**#8: Framework/internal style.**

## Shape

```java
dependencyMetaData(IPlayerData.class)
        .wrappedInterface()
        .dependencyInterface()
        .addCoins(500L);
```

## Why It Exists

This gives access to the interface wrapper, not just the raw interface instance.

## Use When

- verifying interface binding
- testing wrapper correctness
- building diagnostic tools
- checking interface-side metadata
- validating `@DelegatesTo` behavior

## Avoid When

- normal domain logic just needs `IPlayerData`
- the wrapper chain makes code noisy
- you are bypassing simpler access styles

## Test Coverage Needed

Test that:

- wrapper exists
- wrapper interface type matches key
- wrapper returns same instance as primitive lookup
- real interface methods work through wrapper

---

# Style 9: Wrapped Instance Access

## Production Rank

**#9: Framework/internal style.**

## Shape

```java
dependencyMetaData(IPlayerData.class)
        .wrappedInstance()
        .dependencyInstance()
        .resetCoins();
```

## Why It Exists

This gives access to the concrete wrapper and concrete instance.

## Use When

- concrete-only methods are intentionally needed
- lifecycle tooling needs the real instance
- tests need to assert concrete identity
- diagnostics need concrete type/source

## Avoid When

- interface methods are enough
- normal service logic starts depending on concrete types
- concrete leakage would weaken interface-first design

## Test Coverage Needed

Test that:

- wrapper exists
- wrapper concrete type is correct
- concrete instance identity is correct
- concrete-only methods work
- wrapper instance matches metadata instance

---

# Style 10: Static Loader Direct Access

## Production Rank

**#10: Bootstrap/test/infrastructure style.**

`DependencyLoaderAccess` is extremely useful, but domain code should not spray static calls everywhere unless that is the intended access layer for that module.

## Shape

```java
IPlayerData playerData = DependencyLoaderAccess.requireInstance(IPlayerData.class);
playerData.addCoins(500L);
```

```java
DependencyLoaderAccess.registerInstance(IPlayerData.class, new PlayerData(), DependencySource.RUNTIME);
```

```java
DependencyLoaderAccess.clear("test-scope");
```

## Why It Exists

It gives the system a direct global/static loader API for bootstrap, tests, scopes, replacements, and metadata access.

## Use In Production When

- bootstrapping the dependency graph
- registering core infrastructure
- bridging old code into the DI system
- writing platform adapter glue
- implementing access interfaces internally

## Avoid In Production When

- normal handlers/services can use `IDependencyLookupAccess`
- bundle access is available
- static calls would hide dependency contracts

## Use In Tests When

- registering fixtures
- clearing scopes
- checking optional/missing dependency behavior
- replacing instances
- asserting metadata registration
- building scoped test environments

## Test Coverage Needed

Test that:

- default scope registration works
- named scope registration works
- requireInstance fails clearly
- findOptionalInstance returns empty when missing
- findMetaData returns registered metadata
- replaceInstance updates mapped instance
- clear resets scope
- DependencySource values are preserved in metadata when supported

---

# Style 11: Optional Lookup

## Production Rank

**#11: Boundary/test style.**

## Shape

```java
Optional<IPlayerData> playerData =
        DependencyLoaderAccess.findOptionalInstance(IPlayerData.class);
```

## Why It Exists

Optional lookup is useful when a dependency is genuinely optional.

## Use When

- a feature module may not be installed
- a platform adapter may not be active
- fallback behavior is legitimate
- tests need missing-dependency assertions

## Avoid When

- the dependency is required for correctness
- optional lookup hides a boot failure
- the code silently skips important behavior

## Test Coverage Needed

Test that:

- optional is present when dependency exists
- optional is empty when missing
- optional behavior does not hide required dependency failures

---

# `@DelegatesTo` Ranking

## Production Rank

**Core registration/verification feature.**

This annotation should be treated as the deterministic binding declaration.

## Shape

```java
@DelegatesTo(IPlayerData.class)
public final class PlayerData implements IPlayerData {
}
```

Multi-interface:

```java
@DelegatesTo({
        IPlayerData.class,
        IPlayerWallet.class
})
public final class PlayerData implements IPlayerData, IPlayerWallet {
}
```

## Why It Matters

It avoids repeated hierarchy crawling and makes bindings explicit.

That helps with:

- performance
- deterministic registration
- cleaner metadata map construction
- better dependency verification
- interface-first architecture
- multi-interface concrete binding

## Use When

- any concrete should be bound into DI
- a concrete implements multiple dependency interfaces
- the system should verify interface-to-concrete mapping
- dependency graph tooling needs clean bindings

## Test Coverage Needed

Test that:

- single-interface binding works
- multi-interface binding works
- concrete actually supports annotated interfaces
- duplicate binding behavior is deterministic
- annotation path is preferred over hierarchy scanning where applicable

---

# Recommended Production Defaults

## For handlers with several dependencies

Use bundle + getters.

```java
@DelegatesTo(IPlayerRewardHandler.class)
public final class PlayerRewardHandler
        implements IPlayerRewardHandler, IDependencyBundleAccess<PlayerRewardDependencies> {

    private IPlayerData getPlayerData() {
        // Getter only names the dependency from the bundle.
        // It does not store, create, or replace the dependency.
        return dependencies().playerData();
    }

    private IEconomyService getEconomyService() {
        // Keep the dependency access readable inside handler methods.
        return dependencies().economyService();
    }

    @Override
    public void handlePlayerReward(long amount) {
        // Actual business logic uses the named dependency getters.
        getPlayerData().addCoins(amount);
        getEconomyService().recordTransaction(amount);
    }
}
```

## For small classes with 1-3 dependencies

Use named getters.

```java
private IPlayerData getPlayerData() {
    return dependency(IPlayerData.class);
}
```

## For one-off dependency calls

Use primitive lookup.

```java
dependency(IPlayerData.class).addCoins(amount);
```

## For tests and bootstrapping

Use static loader access.

```java
DependencyLoaderAccess.registerInstance(IPlayerData.class, new PlayerData(), DependencySource.TEST);
```

## For framework/runtime tooling

Use metadata/wrapper access.

```java
dependencyMetaData(IPlayerData.class).wrappedInstance().dependencyInstance();
```

---

# Production Anti-Patterns

## Anti-Pattern 1: Using Static Loader Everywhere

Bad:

```java
public void handlePlayerReward(long amount) {
    DependencyLoaderAccess.requireInstance(IPlayerData.class).addCoins(amount);
    DependencyLoaderAccess.requireInstance(IEconomyService.class).recordTransaction(amount);
}
```

Why bad:

- hides dependency contract
- harder to graph
- harder to test design boundaries
- bypasses bundle/handler dependency declarations

Use instead:

```java
getPlayerData().addCoins(amount);
getEconomyService().recordTransaction(amount);
```

or:

```java
dependencies().playerData().addCoins(amount);
```

## Anti-Pattern 2: Concrete Access by Default

Bad:

```java
PlayerData playerData =
        dependencyMetaData(IPlayerData.class).dependencyInstance();

playerData.resetCoins();
```

Unless concrete behavior is genuinely required, use:

```java
dependency(IPlayerData.class).addCoins(amount);
```

## Anti-Pattern 3: Field-Cached Dependencies in Reloadable Systems

Risky:

```java
private final IPlayerData playerData = dependency(IPlayerData.class);
```

If dependency replacement is supported, this can hold stale instances.

Use getter access unless you intentionally want construction-time capture.

## Anti-Pattern 4: Optional Lookup for Required Dependencies

Bad:

```java
DependencyLoaderAccess.findOptionalInstance(IPlayerData.class)
        .ifPresent(playerData -> playerData.addCoins(amount));
```

If `IPlayerData` is required, fail fast instead.

```java
DependencyLoaderAccess.requireInstance(IPlayerData.class).addCoins(amount);
```

or use the normal access layer.

---

# Production/Test Matrix

| Style | Production | Tests | Notes |
|---|---:|---:|---|
| Bundle / Mini-Context | Excellent | Excellent | First-class production style. |
| Bundle + Getters | Excellent | Excellent | Best serious handler style. |
| Named Getters | Excellent | Excellent | Best daily small-class style. |
| Primitive Lookup | Good | Excellent | Base primitive. |
| Local Variables | Good | Good | Best for method-scoped use. |
| Field-Cached Dependencies | Conditional | Edge-case | Test stale/capture behavior. |
| Direct Metadata | Internal | Excellent | Great for assertions/tooling. |
| Wrapped Interface | Internal | Excellent | Wrapper validation. |
| Wrapped Instance | Internal | Excellent | Concrete identity/lifecycle validation. |
| Static Loader | Infrastructure | Excellent | Best test/bootstrap API. |
| Optional Lookup | Boundary only | Good | Use only when dependency is actually optional. |

---

# Final Official Guidance

## Production First-Class Styles

1. **Dependency bundles / mini-contexts**
2. **Bundle + named getters**
3. **Named getters over primitive lookup**
4. **Primitive lookup for one-off access**

## Production Conditional Styles

5. **Local variable lookup**
6. **Field-cached lookup**

## Framework/Internal Styles

7. **Direct metadata access**
8. **Wrapped interface access**
9. **Wrapped instance access**

## Test/Bootstrap Styles

10. **Static loader access**
11. **Optional lookup**
12. **Scoped static loader access**
13. **Replacement access**

---

# Final Take

Bundles should be the production flagship.

They are not just an access style. They are dependency mini-contexts. They make handler dependency boundaries visible, testable, and enforceable.

Named getters are still the best daily local ergonomics.

Static loader access is great for tests and bootstrap, but should not become the lazy global call path inside every handler unless the module is explicitly infrastructure-level.

Metadata and wrapper access are powerful and should stay available, but they are runtime/framework tools, not normal business logic toys. Otherwise every feature method turns into a dependency autopsy report, and nobody needs that kind of crime scene energy in production Java.
