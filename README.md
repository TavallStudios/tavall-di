# Tavall DI

A Java dependency-injection and dependency-access library for Tavall applications.

Tavall DI owns dependency discovery, metadata, instance lifecycle, typed dependency access, delegation, and source lowering for expanded DependencyAccess declarations.

## Why Tavall DI

- Keeps dependency metadata and managed instances under one source of truth.
- Supports typed access and explicit delegation across application boundaries.
- Documents production and test access styles separately from the system contract.

## Features

- Dependency loading and lifecycle hooks
- Typed dependency access and delegation
- Dependency bundles and scoped access
- Annotation processing and source lowering

## Architectural Doctrine

> **Tavall-owned behavior is interface-first and DI-managed by default.**

```text
behavior → interface / contract → tavall-di → implementation
```

Consumers depend on interfaces/contracts, not concrete implementations. Component graphs are wired and accessed via typed `DependencyAccess<...>`, while direct `DependencyMap` access is reserved for infrastructure and composition roots.

## Quick Start

Add the published artifact to a Gradle project:

```kotlin
dependencies {
    implementation("org.tavall:tavall-di:1.2.1")
}
```

### 1. Define the Interface Contract

```java
public interface PlayerRewardService {
    void rewardPlayer(UUID playerId, long amount);
}
```

### 2. Implement and Register with `@DelegatesTo`

```java
@DelegatesTo(PlayerRewardService.class)
public final class PlayerRewardServiceImpl
        implements PlayerRewardService,
                   DependencyAccess<EconomyService, AuditService> {

    @Override
    public void rewardPlayer(UUID playerId, long amount) {
        getInstance().economyService().credit(playerId, amount);
        getInstance().auditService().logReward(playerId, amount);
    }
}
```

### 3. Consume via Contract

```java
@DelegatesTo(PlayerRewardHandler.class)
public final class PlayerRewardHandlerImpl
        implements PlayerRewardHandler,
                   DependencyAccess<PlayerRewardService> {

    @Override
    public void onEvent(RewardEvent event) {
        getInstance().rewardPlayer(event.playerId(), event.amount());
    }
}
```

## Project Structure

`tavall-di/` (root Gradle project)
- **Root Java source/build module** ← This Module — `src/main/java/`, `build.gradle.kts`

Module Type: `SYSTEM`; Secondary Role: `LIBRARY`; Runtime Owner: consuming runtime.

## Documentation

| Document | Purpose |
| --- | --- |
| [Module Progression](docs/progression/TAVALL_DI_PROGRESSION.md) | Audited module implementation, integration, validation, and history. |
| [Tavall DI System](docs/TAVALL_DI_SYSTEM_FINAL.md) | System ownership and contract. |
| [DI Access Styles](docs/DI_ACCESS_STYLES.md) | Supported production and test access patterns. |
| [Module Profiles](docs/module-profiles/README.md) | Per-deployment module profile architecture, identity boundaries, and resolution semantics. |
| [Typed Identifiers & Profiles](docs/TYPED_IDENTIFIERS_AND_MODULE_PROFILES.md) | Extensible CustomEnum identifiers and profile contracts. |
| [Contributing](CONTRIBUTING.md) | Contribution and development notes. |

## Module Profiles and Deployment Composition

Tavall DI provides `DeploymentModuleProfile`, `ModuleProfile`, and the interface-first `IModuleProfileResolver` (`ModuleProfileResolver`) to resolve per-deployment module composition and topological load ordering.

### Identity Boundaries

- **Artifact Identity:** Immutable bytes produced by CI (JAR digest / artifact coordinates).
- **Deployment Identity:** Designated execution host or runtime target (`RuntimeTargetId`).
- **Module Identity:** Functional capability identified by a canonical `CustomEnum` (`ModuleId`).
- **Module Profile:** Desired composition (`DeploymentModuleProfile` / `ModuleProfile`), specifying schema version, configuration generation, explicitly enabled modules, and explicitly disabled modules.
- **Runtime State:** Observed activation and lifecycle health within the runtime host (`tavall-registry`).

> [!NOTE]
> Profile resolution computes desired module composition and topological load order. It does not perform durable persistence, runtime host process supervision, or dynamic live module state transitions.


## Module Development

- **Module Type:** `SYSTEM`
- **Secondary Role:** `LIBRARY`
- **Runtime Owner:** Consuming runtime modules own composition; `tavall-di` has no independent executable runtime.
- **Current PR Stack:** PRs [#10](https://github.com/TavallStudios/tavall-di/pull/10), [#19](https://github.com/TavallStudios/tavall-di/pull/19), [#13](https://github.com/TavallStudios/tavall-di/pull/13), and [#14](https://github.com/TavallStudios/tavall-di/pull/14) merged through staging root [#26](https://github.com/TavallStudios/tavall-di/pull/26) to `main` on 2026-10-18 at `6e45442bcd89c0efdc9505d0e48975c7aefe8450`. `staging/platform` is synchronized to the same commit.
- **Module-local CI Definition:** `.tavallci/ci.yaml` is present on `main` and defines four required checks with exact source dependencies. Local Java 25 / Gradle 9.6.1 validation passes on the integrated source; the Tavall Bot published a local exact-source check. Hosted TCI remains unrun because no active CONTROL developer environment is registered. GitHub Actions are disabled.

## Requirements / Compatibility

Java 25.

## Building From Source

```bash
./gradlew check
```

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## License

No tracked license file is present in the current repository tree.

## Documentation Update State

<details>
<summary>Documentation Update State</summary>

### Current Locations

| Surface | Sync State | Location | Last Updated | Evidence |
| --- | --- | --- | --- | --- |
| GitHub | PRIMARY | TavallStudios/tavall-di/README.md | 2026-09-27 5:33 PM PDT| PR [#17](https://github.com/TavallStudios/tavall-di/pull/17) updated to correct table formatting. |
| Notion | NOT_APPLICABLE | — | 2026-09-27 12:29 PM PDT | README files are not synchronized as Notion twins. |

### Update History

| Timestamp | Surface | Event | Location | Previous Location | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| 2026-09-27 12:29 PM PDT | GitHub | UPDATED | TavallStudios/tavall-di/README.md | Same path | https://github.com/TavallStudios/tavall-di/pull/17. | Reworked the public README to describe the current project, module boundary, usage, and documentation. |
| 2026-09-27 5:32 PM PDT | GitHub | UPDATED | TavallStudios/tavall-di/README.md | Same path | PR [#17](https://github.com/TavallStudios/tavall-di/pull/17) | Added the required root-module Progression route, visible module marker, classification, PR stack, and CI-definition state. |
| 2026-09-27 5:33 PM PDT | GitHub | UPDATED | TavallStudios/tavall-di/README.md | Same path | PR [#17](https://github.com/TavallStudios/tavall-di/pull/17) | Corrected the Documentation table row order; also corrected PR stack punctuation. |

</details>

## Generation ownership

See [Generation-owned dependency access](docs/GENERATION_OWNED_DEPENDENCY_ACCESS.md) for managed registration, aliases, parent references and stale-reference rejection. Root CI is [`.tavallci/ci.yaml`](.tavallci/ci.yaml). This module is a `SYSTEM` with a secondary `LIBRARY` role, composed by its consuming runtime; it owns no independent process.

## Provided dependencies

Third-party factories implement `IDependencyFactory<T>.create()` and use the existing typed `DependencyAccess` and named getters for inputs. Consumers use the same pattern. See [Provided dependencies](docs/PROVIDED_DEPENDENCIES.md) for the factory/consumer example, generated access, bootstrap ordering and scoped lifecycle contract.
