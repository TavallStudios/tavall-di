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

## Quick Start

Add the published artifact to a Gradle project:

```kotlin
dependencies {
    implementation("org.tavall:tavall-di:<version>")
}
```

Use the exact published version and repository access configured for your project. See the links below for API and contribution details.

## Project Structure

`tavall-di/` (root Gradle project)
- **Root Java source/build module** ← This Module — `src/main/java/`, `build.gradle.kts`

Module Type: `LIBRARY`; Runtime Owner: `None`.

## Documentation

| Document | Purpose |
| --- | --- |
| [Module Progression](docs/progression/TAVALL_DI_PROGRESSION.md) | Audited module implementation, integration, validation, and history. |
| [Tavall DI System](docs/TAVALL_DI_SYSTEM_FINAL.md) | System ownership and contract. |
| [DI Access Styles](docs/DI_ACCESS_STYLES.md) | Supported production and test access patterns. |
| [Contributing](CONTRIBUTING.md) | Contribution and development notes. |

## Module Development

- **Module Type:** `LIBRARY`
- **Runtime Owner:** `None`
- **Current PR Stack:** README and module Progression [#17](https://github.com/TavallStudios/tavall-di/pull/17); platform integration [#12](https://github.com/TavallStudios/tavall-di/pull/12) (draft to `main`); feature/architecture work [#10](https://github.com/TavallStudios/tavall-di/pull/10), [#11](https://github.com/TavallStudios/tavall-di/pull/11), [#14](https://github.com/TavallStudios/tavall-di/pull/14), and CI localization [#13](https://github.com/TavallStudios/tavall-di/pull/13) remain unmerged against `staging/platform`.
- **Module-local CI Definition:** Missing from current `main`: `.tavallci/ci.yaml` (required for a Tavall source/build module). This documentation PR records the gap and does not change CI configuration.

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
