# Generation-owned dependency access

> **Notion Twin:** [Generation-owned access](https://app.notion.com/p/3f838458ddfd81488d29f49a329f1789)

`GenerationOwnedDependencyAccess` is canonical Tavall DI infrastructure. A managed consumer extends it, implements its meaningful interface and typed `DependencyAccess<...>`, and declares aliases with `@DelegatesTo`. Ordinary source has no map field, map parameter or static locator. `DependencyMap` binds the instance to its owning generation before initialization.

Annotation bootstrap uses the same generation contract. Newly created annotated consumers bind to their metadata's owning map before declared-input initialization or lifecycle callbacks. Publication remains two-phase: binding does not run lifecycle work early. Reused live metadata is not rebound. Normal reload, map clear and failed-start rollback retire the old/new owned instance through the existing map lifecycle; no second container or consumer fallback is involved.

Concrete and declared interface aliases share one metadata owner. Registering a second supported alias for the same instance reuses that metadata. Replacement, removal, and clear permanently retire the generation binding. A retired generation-owned instance cannot be rebound, and a replacement generation requires a fresh managed instance. Stale handles retained across remove, clear, or replacement fail closed and cannot be resurrected by same-object registration. Attempting replacement with the same object or an already-retired instance rejects before metadata mutation, preserving the live mapping without corruption. Subclasses may invalidate their own nonblocking generation state through `onDependencyGenerationUnbound`; that hook must not block, perform dependency lookup or mutate a replacement. Runtime teardown precedes map removal.

Existing `registerDependency` composes an explicit reference to parent metadata. The object remains parent-owned. Child clearing does not close it, child removal does not retire it, and replacement through the child map is rejected. No second dependency container, provider registry or module lifecycle engine is introduced. Compatibility global lookup remains for existing legacy consumers; it is not used by generation-owned access.

The Filesystem and NLEW extraction consumes this contract to remove direct map storage from ordinary handlers and features. The contract was introduced as an additive `1.1.0` candidate. The current Gradle default remains `1.2.1`; `tavallDiVersion` permits exact-source CI to bind a job-private source version independently of the producer's immutable release identity.


## Annotation bootstrap repair evidence

[Tavall DI PR #27](https://github.com/TavallStudios/tavall-di/pull/27) corrects the metadata publication path at source `aa5fe3c52df55a7265abce398b1687fa2790c1be`. The new lifecycle regression fails on preceding main `d16c1de2c205f3c72fe63cd5a2765c3b1bf55368` because its consumer is unbound. After the repair, all 107 owning DI cases pass with zero failures/errors/skips. Four regressions cover lifecycle access and idempotence, reload, clear/fresh bootstrap and failed-lifecycle rollback. The actual MC command consumer passes all 34 proxy cases, 86 Runtime API cases and the canonical architecture gate (121 cases total) with this exact candidate DI source and an explicit consumer source graph; MC final source pinning and full/native acceptance are recorded separately.

## Documentation Update State

- Owning GitHub document: this path; implementation evidence is linked through PR #27.
- Notion: current owning Technical amendment saved and read back; broader workspace reconciliation remains separate.
- Updated: 2026-10-26 02:24 PM PDT.
