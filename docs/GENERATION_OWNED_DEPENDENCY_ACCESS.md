# Generation-owned dependency access

> **Notion Twin:** [Generation-owned access](https://app.notion.com/p/3f838458ddfd81488d29f49a329f1789)

`GenerationOwnedDependencyAccess` is canonical Tavall DI infrastructure. A managed consumer extends it, implements its meaningful interface and typed `DependencyAccess<...>`, and declares aliases with `@DelegatesTo`. Ordinary source has no map field, map parameter or static locator. `DependencyMap` binds the instance to its owning generation before initialization.

Concrete and declared interface aliases share one metadata owner. Registering a second supported alias for the same instance reuses that metadata. Replacement/removal/clear retire the old binding, so retained consumers fail closed rather than falling back to the compatibility global map. Subclasses may invalidate their own nonblocking generation state through `onDependencyGenerationUnbound`; that hook must not block, perform dependency lookup or mutate a replacement. Runtime teardown precedes map removal.

Existing `registerDependency` composes an explicit reference to parent metadata. The object remains parent-owned. Child clearing does not close it, and replacement through the child map is rejected. No second dependency container, provider registry or module lifecycle engine is introduced. Compatibility global lookup remains for existing legacy consumers; it is not used by generation-owned access.

The Filesystem and NLEW extraction consumes this contract to remove direct map storage from ordinary handlers and features. The repository publishes an additive `1.1.0` candidate; `tavallDiVersion` permits exact-source CI to bind a job-private source version independently of the producer's immutable release identity.
