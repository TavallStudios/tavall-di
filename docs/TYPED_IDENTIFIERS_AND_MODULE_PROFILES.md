# Typed Identifiers and Module Profiles

Tavall DI provides strongly typed, interface-first generic building blocks for systems that select groups of runtime modules without falling back to magic string APIs.

## CustomEnum

`CustomEnum<T>` from `org.tavall.util.CustomEnum` (`org.tavall:tavall-custom-enum-java:1.0.0`) is the canonical base class for extensible enum-like identifier domains. Domains define a concrete type with a private constructor and bind registration through the canonical factory:

```java
public final class ModuleId extends CustomEnum<ModuleId> {
    private ModuleId(String name) {
        super(name);
    }

    public static ModuleId of(String name) {
        return register(ModuleId.class, name, ModuleId::new);
    }
}
```

Application code uses `ModuleId`, not raw `String`. Registration guarantees canonical identity equality (`id1 == id2`), eliminating string comparisons across module boundaries.

## Module Profiles and Deployment Profiles

### `ModuleProfile<M, P>`

Declares the modules requested by a profile. Preserves the simplified selection API:

```java
ModuleProfile<ModuleId, ProfileId> profile = ModuleProfile.of(
        ProfileId.of("lobby"),
        ModuleId.of("tavall-paper-core"),
        ModuleId.of("tavall-lobby")
);
```

### `DeploymentModuleProfile<M, P, R>`

Immutable typed per-deployment profile specification containing:
- Positive schema version (`DEFAULT_SCHEMA_VERSION = 1L`)
- Profile identity (`profileId` / `id`)
- Runtime target identity (`runtimeTarget`)
- Configuration generation index (`configurationGeneration`)
- Explicitly enabled modules
- Explicitly disabled modules

Enabled and disabled module sets are mutually exclusive; conflicts are rejected upon instantiation. Lossy projection to unversioned profiles is intentionally omitted so downstream runtime composition preserves all deployment constraints.

```java
DeploymentModuleProfile<ModuleId, ProfileId, RuntimeTargetId> profile = DeploymentModuleProfile.<ModuleId, ProfileId, RuntimeTargetId>builder()
        .schemaVersion(1L)
        .profileId(ProfileId.of("web-commerce-stripe"))
        .runtimeTarget(RuntimeTargetId.of("web-commerce"))
        .configurationGeneration(10L)
        .enable(ModuleId.of("tavall-pay"), ModuleId.of("tavall-pay-stripe"))
        .disable(ModuleId.of("tavall-pay-paypal"))
        .build();
```

## Resolver: `IModuleProfileResolver` and `ModuleProfileResolver`

The resolver is interface-first and DI-managed, annotated with `@DelegatesTo(IModuleProfileResolver.class)`.

It produces an `EffectiveDeploymentModuleProfile` (or `EffectiveModuleProfile`) containing the validated configuration, requested modules, disabled modules, and a deduplicated dependency-first `loadOrder`.

### Safety Guarantees
- **Schema Version Validation:** Rejects unsupported schema versions (`IllegalArgumentException`). Only supported schema versions (currently `1L`) pass resolution.
- **Disabled Module Catalog Validation:** All explicitly disabled module IDs must exist in the available artifact catalog. Typos or missing disabled modules fail closed (`IllegalArgumentException`).
- **Disabled Required Dependencies:** If an enabled module requires a dependency that is explicitly disabled, resolution fails closed with `IllegalStateException` and never silently enables the disabled module.
- **Enabled/Disabled Conflict:** Profiles with overlapping enabled and disabled sets are rejected.
- **Unavailable Modules:** Enabled modules not present in the catalog fail with `IllegalArgumentException`.
- **Cycles:** Dependency cycles fail with `IllegalStateException`.

### Architectural Fences
Profile resolution is strictly a desired-composition calculator. It does not perform durable storage persistence, runtime host process control, or dynamic live module state transitions. Those responsibilities belong to `tavall-registry` and consumer runtime hosts.
