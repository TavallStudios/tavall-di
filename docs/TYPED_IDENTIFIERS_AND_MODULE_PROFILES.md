# Typed identifiers and module profiles

Tavall DI provides two small generic building blocks for systems that select groups of runtime modules without falling back to magic string APIs.

## CustomEnum

`CustomEnum<T>` is an enum-like identifier base class for domains whose value set needs to remain extensible. A domain defines one concrete type and then declares constants or constructs values at its own boundary:

```java
public final class ModuleId extends CustomEnum<ModuleId> {
    private ModuleId(String value) {
        super(value);
    }

    public static ModuleId of(String value) {
        return new ModuleId(value);
    }
}
```

Application code uses `ModuleId`, not `String`. `value()` exists for serialization/configuration boundaries. Equality is domain-aware, so a `ModuleId("lobby")` is not equal to an unrelated identifier domain carrying the same text.

This is deliberately not a registry and not a Java `enum`: adding a profile or module identifier must not require modifying a central Tavall DI enum.

## Module profiles

`ModuleProfile<M, P>` declares the modules explicitly requested by a named profile. It does not encode platform policy or product names.

`ModuleProfileResolver` accepts:

- a profile;
- the caller-owned set of available modules;
- a caller-owned function that returns each module's required technical dependencies.

It produces an `EffectiveModuleProfile` containing the requested module set and a deduplicated dependency-first load order. Unknown modules and required-dependency cycles fail explicitly.

Optional dependencies and product-specific profile composition remain caller policy. Tavall DI must not know that a Minecraft deployment has concepts such as Lobby, FFA, Kingdoms, Paper, or Velocity.

Example caller-side profile:

```java
ModuleProfile<ModuleId, ModuleProfileId> lobby = ModuleProfile.of(
        ModuleProfileIds.LOBBY,
        ModuleIds.LOBBY,
        ModuleIds.ESSENTIALS
);

EffectiveModuleProfile<ModuleId, ModuleProfileId> effective = resolver.resolve(
        lobby,
        moduleCatalog.ids(),
        moduleCatalog::requiredDependencies
);
```

This separates **selection policy** (the profile) from **technical correctness** (required dependency closure), while keeping both strongly typed.
