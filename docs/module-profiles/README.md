# Per-Deployment Module Profiles

Tavall DI provides strongly-typed, interface-first module profile definitions and resolution for composing systems and features across distinct runtime targets.

## Architectural Boundaries

Per-deployment module profile resolution strictly separates five core identities:

| Concept | Identity Scope | Authority / Owner | Description |
| --- | --- | --- | --- |
| **Artifact Identity** | Immutable bytes | Build & CI (`tavall-ci`) | Immutable build artifacts, JAR digests, and published Maven packages. Contains all compiled systems and features. |
| **Deployment Identity** | Target environment | Orchestration / CD (`tavall-cd`) | The designated execution host or release target (e.g. `web-commerce`, `content-autopost-node`, `mc-velocity`). |
| **Module Identity** | Functional capability | Module domain (`CustomEnum`) | Typed, extensible identifier for each discrete SYSTEM or FEATURE module. |
| **Module Profile** | Desired composition | Deployment configuration (`tavall-di`) | Typed specification of desired module composition (`DeploymentModuleProfile`), including schema version, configuration generation, explicitly enabled modules, and explicitly disabled modules. |
| **Runtime State** | Observed activation | Runtime host (`tavall-registry` / host) | Dynamic observation of live modules (loaded, enabled, healthy, degraded, failed, stopped). |

> [!IMPORTANT]
> **Fencing Guarantee:** Profile resolution in Tavall DI determines **pure desired composition** and topological load order. It does **not** pretend to implement durable persistence, runtime process management, or dynamic live module enable/disable state transitions. Those responsibilities belong strictly to `tavall-registry` and consuming runtime hosts.

## Core Models

This reconciles the unreleased profile API in PR #11. Consumers of that draft must migrate `org.tavall.identity.CustomEnum` to the canonical `org.tavall.util.CustomEnum` registration API. The accepted DI 1.1.0 release is unchanged; these new profile classes have not been published as that Maven release. Development CI identifies the candidate by exact source and artifact digest.

### `DeploymentModuleProfile`

An immutable record representing a versioned deployment profile:

- `schemaVersion`: Positive schema version index (current supported version: `1L`).
- `profileId`: Typed profile identity extending `org.tavall.util.CustomEnum`.
- `runtimeTarget`: Typed runtime target identity extending `org.tavall.util.CustomEnum`.
- `configurationGeneration`: Non-negative configuration generation index.
- `enabledModules`: Immutable set of explicitly enabled module identifiers.
- `disabledModules`: Immutable set of explicitly disabled module identifiers.

Validation rules:
- Enabled and disabled sets are mutually exclusive; any overlap throws `IllegalArgumentException`.
- Null arguments or null collection elements are rejected.
- Schema version must be positive; configuration generation must be non-negative.
- Downstream composition retains the full typed `DeploymentModuleProfile`. Lossy conversion to untargeted, unversioned module profiles is intentionally omitted to preserve deployment authority constraints.

### `IModuleProfileResolver` and `ModuleProfileResolver`

Interface-first, DI-managed resolver registered under `@DelegatesTo(IModuleProfileResolver.class)`.

Expands a requested deployment profile into an `EffectiveDeploymentModuleProfile`:
- Computes dependency-first topological load order for active modules.
- **Schema Version Verification:** Verifies against supported schema versions (`isSupportedSchemaVersion`); rejects unsupported versions with `IllegalArgumentException`.
- **Disabled Module Catalog Validation:** Every explicitly disabled module ID must exist in the available artifact catalog. Typos or references to nonexistent module IDs fail closed with `IllegalArgumentException`.
- **Fail-Closed Safety:** If an enabled module requires a dependency that is explicitly disabled, resolution throws `IllegalStateException` without silently enabling the disabled module.
- **Unavailable Modules:** Unknown modules in enabled sets or required dependency graphs outside the caller-supplied catalog are rejected with `IllegalArgumentException`.
- **Cycles:** Circular required dependencies are rejected with `IllegalStateException`.
