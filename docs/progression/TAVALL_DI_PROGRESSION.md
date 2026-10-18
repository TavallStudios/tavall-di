# tavall-di Progression

> **Notion Twin:** [Tavall DI — PROGRESSION](https://app.notion.com/p/3f838458ddfd814a9175c68d3526c266)

> **Status:** Active progression record  
> **Document Type:** `PROGRESSION`  
> **Progression Scope:** `MODULE`  
> **Module Type:** `SYSTEM`<br>
> **Secondary Role:** `LIBRARY`<br>
> **Owning System:** `tavall-di`  
> **Owns:** Audited implementation, integration, validation, and historical progression for the root `tavall-di` library module  
> **Does Not Own:** Product/design rules, aggregate system progression, deployment history, or Git workflow policy  
> **Audited Against:** `TavallStudios/tavall-di@d8ecc02302522e3acecc586c3f9e918007c82ca3`  
> **Last Reconciled:** `2026-10-17 11:08 PM PDT`

## About

The root Gradle library owns dependency discovery, metadata and instance lifecycle, delegation, typed access, dependency bundles, and source lowering for expanded `DependencyAccess` declarations. Its progression measures shared API and implementation maturity, test evidence, and consumer compatibility; the system contract remains in `docs/TAVALL_DI_SYSTEM_FINAL.md`.

## Module Context

| Field | Value |
| --- | --- |
| Repository | [TavallStudios/tavall-di](https://github.com/TavallStudios/tavall-di) |
| Module | Root Gradle project (`tavall-di`) |
| Module Type | `SYSTEM` |
| Secondary Role | `LIBRARY` |
| Owning System | `tavall-di` |
| Runtime Owner | Consuming runtime modules own composition; `tavall-di` has no independent executable runtime |
| Primary Consumers | Not established by this module-focused audit |
| Current Branch / PR Stack | PRs [#10](https://github.com/TavallStudios/tavall-di/pull/10), [#19](https://github.com/TavallStudios/tavall-di/pull/19), [#13](https://github.com/TavallStudios/tavall-di/pull/13), and [#14](https://github.com/TavallStudios/tavall-di/pull/14) merged to `main` through staging root [#26](https://github.com/TavallStudios/tavall-di/pull/26) at `6e45442bcd89c0efdc9505d0e48975c7aefe8450`. `staging/platform` is synchronized to the same commit; no child PRs from this integration remain open. |
| Audited Revision | [`6e45442bcd89c0efdc9505d0e48975c7aefe8450`](https://github.com/TavallStudios/tavall-di/commit/6e45442bcd89c0efdc9505d0e48975c7aefe8450) on `main` |

## Current Status

| Field | State |
| --- | --- |
| Overall State | `PARTIAL` |
| Current Phase | Main source integration complete; release and named consumer acceptance remain separate |
| Implementation | Main includes provided dependency factories, lowerer fixes, constructor policy, and source-owned CI configuration |
| Integration | PRs #10, #19, #13, and #14 are integrated on main; named consumer acceptance remains unestablished |
| Validation | Java 25.0.1 / Gradle 9.6.1 local integrated check passed: 103 tests, zero failures/errors/skips, and `verifyJarContents`; Tavall Bot local exact-source check passed on the same source tree |
| Runtime / Consumer Acceptance | No independent DI runtime; named consumer acceptance is not established by this mainline audit |
| Deployment Verification | `N/A` — non-deployable library |
| Unrun Acceptance | Hosted TCI has not executed because CONTROL has no active developer environment; package release and named consumer acceptance are also unrun |
| Next Slice | Restore the supported CONTROL environment path, run hosted TCI, and validate named consumers before an approved package release |

## Progression Timeline

| Date / Time | State | Progression | Evidence | Result / Remaining Work |
| --- | --- | --- | --- | --- |
| 2026-04-06 3:28 AM PDT | `HISTORICAL_EVIDENCE` | Legacy dependency-access contracts, annotations, metadata, loading, and lifecycle injection entered the module history. | [fa79047d9c0e](https://github.com/TavallStudios/tavall-di/commit/fa79047d9c0e), [63a6b25d025e](https://github.com/TavallStudios/tavall-di/commit/63a6b25d025e), [be1a987c59fc](https://github.com/TavallStudios/tavall-di/commit/be1a987c59fc), [20c6fd64048d](https://github.com/TavallStudios/tavall-di/commit/20c6fd64048d) | These commits establish the initial DI capability history; the current system contract is documented separately. |
| 2026-05-20 11:09 PM PDT | `IN_PROGRESS` | Canonical injectable contracts and lifecycle were declared. | [e56e7fa4f1eb](https://github.com/TavallStudios/tavall-di/commit/e56e7fa4f1eb), [6a3fcf74d124](https://github.com/TavallStudios/tavall-di/commit/6a3fcf74d124) | The current implementation includes lifecycle hooks and injectable metadata; consumer acceptance is not established here. |
| 2026-07-23 11:23 AM PDT | `IN_PROGRESS` | The standalone Gradle build and Java 25 toolchain were established. | [fac0992cdf61](https://github.com/TavallStudios/tavall-di/commit/fac0992cdf61), [330b41890e4f](https://github.com/TavallStudios/tavall-di/commit/330b41890e4f) | The root build configures JUnit 5 and locks dependencies; test execution is not evidenced by this audit. |
| 2026-08-01 10:43 PM PDT | `IN_PROGRESS` | Typed `DependencyAccess` generation and a build entry point for source lowering were added and refined. | [54bbaf743156](https://github.com/TavallStudios/tavall-di/commit/54bbaf743156), [acc75007aefc](https://github.com/TavallStudios/tavall-di/commit/acc75007aefc) | The production source contains access lowering and the tracked suite includes source-generation/integration cases; no pass result is claimed. |
| 2026-08-10 12:48 AM PDT | `IN_PROGRESS` | Generated access acronym handling and the DI test-runtime guard were corrected. | [2ca56b179cad](https://github.com/TavallStudios/tavall-di/commit/2ca56b179cad), [439e4fd6b908](https://github.com/TavallStudios/tavall-di/commit/439e4fd6b908) | Test files are present; this audit did not execute them. |
| 2026-08-10 5:35 PM PDT | `IN_PROGRESS` | Package resolution was changed to authenticated GitHub Packages configuration. | [6e3ea2855a21](https://github.com/TavallStudios/tavall-di/commit/6e3ea2855a21) | Current build declares `tavall-logging` as an API dependency; artifact publication/consumer resolution is unverified. |

## Validation State

| Validation | State | Evidence | Remaining Work |
| --- | --- | --- | --- |
| Architecture / module boundary | Locally validated | Integrated main tree `6e45442bcd89c0efdc9505d0e48975c7aefe8450`; constructor policy and the existing source-lowering boundary passed | Re-run against later source changes |
| Unit | Passed locally | 103 tests, zero failures/errors/skips; Java 25.0.1 / Gradle 9.6.1; exact Logging and Custom Enum source composites | Run hosted TCI when a CONTROL developer environment is active |
| Integration | DI staging-to-main tree passed locally | Bot check `tavall-local/exact-source` on staging head `87d35f8fee060128d82943c4d18b1a6aa06c6774`; main merge `6e45442` has the same tree | Validate named consumer integration and compatibility |
| Consumer / Runtime | Not established | No named runtime owner or accepted consumer evidence recorded in this audit | Identify and validate runtime consumers |
| End-to-End | N/A | Root module is a non-deployable library | Validate through owning runtime when one is identified |

## Dependencies and Integration

| Dependency / Consumer | Relationship | State | Evidence |
| --- | --- | --- | --- |
| Java 25 | Build/runtime API baseline | Declared by the root Gradle toolchain and validated locally | `build.gradle.kts` on main `6e45442` |
| Module implementation | Current boundary | Main includes provided dependency factories, source lowering, and managed-constructor architecture enforcement. | Main source tree at [`6e45442`](https://github.com/TavallStudios/tavall-di/tree/6e45442bcd89c0efdc9505d0e48975c7aefe8450/src/main) |
| tavall-logging | API dependency `org.tavall:tavall-logging:1.0.0` | Exact source `e047172ab7c2be005baf0b2c4b46e230ead78417` used in local validation | `.tavallci/ci.yaml` on main |
| tavall-custom-enum-java | API dependency `org.tavall:tavall-custom-enum-java:1.0.0` | Exact source `2284168910f755a0d13a68fbc821cedc14d0a49b` used in local validation | `.tavallci/ci.yaml` on main |

## Remaining Acceptance Work

| Open acceptance item | Impact | Next action |
| --- | --- | --- |
| Hosted TCI is unrun because CONTROL has no active developer environment | No hosted Tavall CI result is claimed; local exact-source checks and Bot status are available | Restore the supported CONTROL environment path and run TCI on current main |
| Named consumer and package-release acceptance remain unrun | Compatibility and published-artifact acceptance are not established by this library-level merge | Validate consuming repositories, then publish only through the approved release workflow |

## Next Slice

Restore the supported CONTROL developer environment, run hosted TCI, then validate named consumers before an approved package release. Continue source fixes and independent PR work while those acceptance layers are prepared.

## Related Documentation

| Type | Document |
| --- | --- |
| Module README | [`README.md`](../../README.md) |
| Build and source | [`build.gradle.kts`](../../build.gradle.kts), [`src/main`](../../src/main) |
| System / technical | [`TAVALL_DI_SYSTEM_FINAL.md`](../TAVALL_DI_SYSTEM_FINAL.md), [`DI_ACCESS_STYLES.md`](../DI_ACCESS_STYLES.md) |
| Deployment | `N/A` — non-deployable `LIBRARY` module |

## Documentation Update State

<details>
<summary>Documentation Update State</summary>

### Current Locations

| Surface | Sync State | Location | Last Updated | Evidence |
| --- | --- | --- | --- | --- |
| GitHub | `PRIMARY` | `TavallStudios/tavall-di/docs/progression/TAVALL_DI_PROGRESSION.md` | 2026-10-18 14:11 UTC | PR #13 merged at `01ec52f58e9870590b50232a4fad8cdd2cce0e67`; PR #14 merged at `87d35f8fee060128d82943c4d18b1a6aa06c6774`; staging root #26 merged to main at `6e45442bcd89c0efdc9505d0e48975c7aefe8450`; main and staging are synchronized. |
| Notion | `PENDING_SYNC` | [Tavall DI — PROGRESSION](https://app.notion.com/p/3f838458ddfd814a9175c68d3526c266) | Last verified 2026-10-18 06:32 UTC | Sync the current main merge, local exact-source result, and Bot check after the GitHub progression update. |

### Update History

| Timestamp | Surface | Event | Location | Previous Location | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| 2026-09-27 5:30 PM PDT | GitHub | `CREATED` | `docs/progression/TAVALL_DI_PROGRESSION.md` | — | PR [#17](https://github.com/TavallStudios/tavall-di/pull/17) at the current documentation branch; audited baseline `d8ecc02302522e3acecc586c3f9e918007c82ca3` | Created module-scoped Progression from GitHub source, build, history, and documentation evidence. |
| 2026-10-17 8:05 PM PDT | GitHub + Notion | `SYNCED` | GitHub Progression ↔ [Notion twin](https://app.notion.com/p/3f838458ddfd814a9175c68d3526c266) | 2026-09-27 5:30 PM PDT | PR #10 code source `525fef5d9f03ba81fd5218368a1d8328776b3827`; report artifact commit `cbc65ecfd04a8459b2dcc8a4c53cd6f574bec6fd`; 96 local tests; hosted request `38dd54fc-f37d-4ea2-953a-c9c6982f34af` rejected before source execution; post-fetch verified | Corrected classification/runtime ownership and PR stack, added immutable local-results report, and recorded separate hosted and staging acceptance boundaries. |
| 2026-10-17 10:08 PM PDT | GitHub + Notion | `SYNCED` | GitHub Progression ↔ [Notion twin](https://app.notion.com/p/3f838458ddfd814a9175c68d3526c266) | 2026-10-17 8:05 PM PDT | PR #10 staging merge `e68919729689affef8407bde07904722a2408bb3`; DI PR #19 tested source `0a3b05993c4b4aef7a133ce4ef5f385c45927879`; [results report](evidence/2026-10-18-delegates-to-array-lowerer/results.json); post-fetch verified | Recorded the current staging lineage, array-form `@DelegatesTo` regression and local exact-source results while keeping hosted, package, runtime and deployment acceptance separate. |
| 2026-10-17 10:23 PM PDT | GitHub + Notion | `SYNCED` | GitHub Progression ↔ [Notion twin](https://app.notion.com/p/3f838458ddfd814a9175c68d3526c266) | 2026-10-17 10:08 PM PDT | Codex P2 comment `4305835422`; tested code source `1bee6d6222f19d81258274250eac8d9729c8e541`; 98 local tests; [corrected results report](evidence/2026-10-18-delegates-to-array-lowerer/results.json); post-fetch verified | Masked annotation strings/comments for type-boundary detection, added focused regression coverage, and kept the pending remote review/hosted boundaries explicit. |
| 2026-10-17 10:36 PM PDT | GitHub + Notion | `SYNCED` | GitHub Progression ↔ [Notion twin](https://app.notion.com/p/3f838458ddfd814a9175c68d3526c266) | 2026-10-17 10:23 PM PDT | Tavall PR Review Agent ROLE finding/fix `e2d97a59d0fef95bc320b9e9f5b26276e44dd14d`; 98 exact-source tests; [current results report](evidence/2026-10-18-delegates-to-array-lowerer/results.json); post-fetch verified | Masked comment/string class tokens before declaration matching; recorded that the dedicated GitHub Bot review provider is unavailable and kept hosted/staging gates separate. |
| 2026-10-17 10:50 PM PDT | GitHub + Notion | `SYNCED` | GitHub Progression ↔ [Notion twin](https://app.notion.com/p/3f838458ddfd814a9175c68d3526c266) | 2026-10-17 10:36 PM PDT | PR #19 merge commit `8a9b90145daad94331e1ee7a6af3a1cecc9a75a9`; tested source `e2d97a59d0fef95bc320b9e9f5b26276e44dd14d`; [verified results report](evidence/2026-10-18-delegates-to-array-lowerer/results.json); post-fetch verified | Recorded child integration into persistent staging while keeping hosted exact-source CI, composed staging, package, runtime and deployment acceptance separate. |
| 2026-10-17 11:08 PM PDT | GitHub + Notion | `UPDATED` | GitHub Progression ↔ [Notion twin](https://app.notion.com/p/3f838458ddfd814a9175c68d3526c266) | 2026-10-17 10:50 PM PDT | PR #13 current source `8160c7dc828899a593947d7744492548b3a989a5`; 98 local exact-source tests; direct TCI request rejected before source execution for missing CONTROL environment context; [PR #13 results](evidence/2026-10-18-di-pr13-ci-migration/results.json); post-fetch verified | Reconciled current staging dependencies/checks and documented the difference between local source validation, unavailable Bot/TCI execution, and package/runtime acceptance. |
| 2026-10-18 06:30 UTC | GitHub + Notion | `SYNCED` | GitHub Progression ↔ [Notion twin](https://app.notion.com/p/3f838458ddfd814a9175c68d3526c266) | 2026-10-17 10:50 PM PDT | DI PR #14 tested source `83f6107605e5c4d8e45d34942861a5e6c2307baf`; 103 Java 25 tests; TCI request `593e7f86-2090-4737-b07d-e304c45c69e4` rejected before source execution; post-fetch verified 2026-10-18T06:30:26Z | Reconciled PR #14 with current staging and replaced the Builder call-site exception with the documented blanket managed-constructor prohibition. |
| 2026-10-18 14:05 UTC | GitHub | `MERGED_TO_STAGING` | `TavallStudios/tavall-di` PRs #13 and #14 → `staging/platform` | 2026-10-18 06:30 UTC | PR #13 merge commit `01ec52f58e9870590b50232a4fad8cdd2cce0e67`; PR #14 local exact-source head `f927073d35dc5083a6287b5ad088bfbabf892f16`; local validation comments #6250781268 and #6250781477; Bot local check `tavall-local/exact-source` passed on both heads | Merged while Draft using the owner-authorized local-check path; no Codex review or GitHub Actions run. Hosted TCI remains unrun because no active CONTROL developer environment is registered. |
| 2026-10-18 14:11 UTC | GitHub | `MERGED_TO_MAIN` | Staging root PR #26 → `main` | `staging/platform@87d35f8fee060128d82943c4d18b1a6aa06c6774` | Main merge commit `6e45442bcd89c0efdc9505d0e48975c7aefe8450`; integrated tree `f4639a7a7ee5d5c2b424e23aff004ee55d880bce`; 103 local tests; Bot check `tavall-local/exact-source` passed on the staging head | Source promotion completed with local and Bot evidence. No hosted TCI, package publication, downstream acceptance, or deployment is claimed. |
| 2026-10-18 14:11 UTC | GitHub | `MERGED_TO_MAIN` | Staging root PR #26 → `main` | `staging/platform@87d35f8fee060128d82943c4d18b1a6aa06c6774` | Main merge commit `6e45442bcd89c0efdc9505d0e48975c7aefe8450`; integrated tree `f4639a7a7ee5d5c2b424e23aff004ee55d880bce`; 103 local tests; Bot check `tavall-local/exact-source` passed on exact staging head | Source promotion completed with local and Bot evidence. No hosted TCI, package publication, downstream acceptance, or deployment is claimed. |

</details>

## 2026-10-12 — Generation-owned access required by Filesystem/NLEW acceptance

The extraction's exact-source architecture check rejected ordinary handler map fields. The canonical DI producer now supplies `GenerationOwnedDependencyAccess`, bound by metadata registration rather than consumer constructors or global lookup. Concrete/interface aliases share one metadata identity; replacement, removal and clear retire retained access. Explicit parent references do not create another owner or child mutation authority. Unbind hooks only invalidate nonblocking local state; owning runtimes perform teardown first.

Local Java 25 `check` and isolated `1.1.0` Maven publication passed; 51 tests passed, including three generation-ownership/parent-reference tests. Exact-source hosted CI, integrated staging and consumer deployment remain separate gates. The branch targets existing staging PR #12 and does not replace active DI work.

## 2026-10-13 — Generation binding main/package acceptance

Implementation PR #20 integrated through staging #12; final main `86de4c75a6b09c5922ef60a2fb06111c5244fda8` passed Tavall CI `df2e3d10-a8a1-48c2-9b62-e71af0082582` (COMPILATION, UNIT_TESTS, QUALITY, ARTIFACT, REQUIRED_ALL). The internally published DI 1.1.0 JAR SHA-256 is `0722f8baccf490d996ff7b6c0868a1d6fd45cb8182c738c32c4ede2a929986da`, matching the accepted immutable CI artifact. DI 1.0.0 remains available.

Generation-owned access binds through the existing DependencyMap metadata lifecycle. Tests cover coherent aliases, suppliers, replacement/removal/clear, failed initialization and borrowed parent metadata. Filesystem/NLEW accepted main checks and Cloud's 902 package-backed tests consume this API; Cloud runtime cutover is independently pending. Source-only publication does not claim every Tavall consumer has adopted the optional new access style.

Successor staging #21 preserves the existing independent feature children; this documentation-only promotion does not replace their branches or scope.

## 2026-10-17 Provided dependency candidate

[PR #10](https://github.com/TavallStudios/tavall-di/pull/10) now uses zero-argument factories, existing typed DependencyAccess and named getters for factory inputs and consumers, including generated expanded access and isolated-map generation ownership. The original fork branch and current platform integration parent #26 are preserved.

Implementation `5bf1ed053e491ccc0ebee5e1827b623c5654bcbb` passes Java 25 clean check/javadoc and JAR boundary validation: 95 tests, zero failures/errors/skips. Coverage includes initialized factory/product prerequisites, injected consumers, invalid declarations, alias identity and replacement, generation retirement, rollback/retry, direct/generated self-access, partial alias repair, retained generations during failed repair, and manual lambda replacement. The JDK HttpClient fixture makes no network request. [Immutable test results](evidence/2026-10-17-provided-dependencies/results.json) bind the implementation separately from the evidence commit.

Fresh native CI request `1578d22a-3792-42b0-be16-acc4ff1e7b4e` failed artifact publication because CONTROL rejected its identity path (`identityFile must be a direct file in the operation credential directory`). Current hosted source acceptance, integration fan-in, package release and deployment remain unaccepted. This is a feature-candidate checkpoint; earlier mainline audit/release entries remain historical. The Notion progression twin records this checkpoint and the same acceptance boundary.

Native phase correction: invocation `1578d22a-3792-42b0-be16-acc4ff1e7b4e` on earlier DI source `abf6397244f62bad9d2e952f7dd693fc1301b672` is verified `COMPLETED`, exit `0`. The compiled caller exports after invocation and removes its operation workspace on export failure. Detailed check results and approved artifact/evidence publication remain unavailable; this does not establish current-head native acceptance.

Review follow-up: metadata now carries its owning map for scoped factory/product injection. Factory-class repair preserves live product identity, and construction-strategy changes roll back if a later provider fails. Generation-owned products retain the existing registration/retirement path.

## 2026-10-18 Generated access provenance correction

PR #10 adds explicit generated-origin metadata to lowered dependency access views. Bootstrap uses the existing `@CompositionBoundary` annotation and the lowerer's exact class identity instead of class-name suffix matching. A regression verifies that an authored `BusinessDependencyAccess.endpoint(): String` remains a business access object and is not expanded as a generated dependency view.

Implementation source `525fef5d9f03ba81fd5218368a1d8328776b3827` passes Java 25 clean check, Javadoc, JAR packaging and boundary validation: 96 tests, zero failures/errors/skips. Javadoc reports 100 existing missing-comment warnings. [The results report](evidence/2026-10-18-provided-dependencies/results.json) binds the JUnit aggregate and local JAR identity to that code source.

The typed hosted CI request `38dd54fc-f37d-4ea2-953a-c9c6982f34af` for PR head `f5352f882625a974c9d33a7848fd3fd183fe02d4` was rejected before source execution with `UNAUTHORIZED: environment repository execution requires CONTROL environment context`. No hosted checks ran; GitHub returned no status contexts or PR workflow runs for that head. This infrastructure boundary is separate from the local test result.

PR #10 merged into `staging/platform` on 2026-10-18 at `e68919729689affef8407bde07904722a2408bb3`. At that checkpoint, staging root #26 remained Draft; it later merged to `main` at `6e45442bcd89c0efdc9505d0e48975c7aefe8450`. The PR #10 local validation did not establish composed staging acceptance, package release, or deployment.

## 2026-10-18 — DelegatesTo array annotation candidate

PR #19 corrects `DependencyAccessSourceLowerer` for array-valued `@DelegatesTo({ ... })` declarations. Its integration merge retains the current staging behavior for `@ProvidedDependency` and generated `@CompositionBoundary` metadata while avoiding annotation-array braces as type-declaration boundaries. The regression covers array-form delegation; the merged suite also retains the ProvidedDependency generated-access test.

Tested source `0a3b05993c4b4aef7a133ce4ef5f385c45927879` has PR #19 source `96b4afce1a94160db43e489a56f010bb64c9efde` and staging parent `e68919729689affef8407bde07904722a2408bb3`. Exact-source composite dependencies were Tavall Logging `e047172ab7c2be005baf0b2c4b46e230ead78417` and Tavall Custom Enum Java `2284168910f755a0d13a68fbc821cedc14d0a49b`, matching the staging `.tavallci/ci.yaml` pins.

Java 25.0.1 / Gradle 9.6.1 `clean check javadoc jar` passed: 97 tests, zero failures/errors/skips, including all eight `DependencyAccessSourceLowererTest` cases; `verifyJarContents` passed. Javadoc completed with 100 missing-comment warnings. The local-only JAR SHA-256 is `8f297988bee6c9af2bc901aa52371a9413054e354bcfeea7ebf8ce237481ca08`; it was not published. [The 97-test checkpoint report](https://github.com/TavallStudios/tavall-di/blob/36c8a30a42faef4d1235bd55a90ffffe496c7bf0/docs/progression/evidence/2026-10-18-delegates-to-array-lowerer/results.json) records that earlier source graph and acceptance boundary.

At this checkpoint, before the Codex P2 correction, PR #19 was open as a Draft targeting `staging/platform`; its tested source was mergeable into the current staging head. GitGuardian Security Checks passed on head `0a3b05993c4b4aef7a133ce4ef5f385c45927879`; no GitHub Actions validation or hosted exact-source CI ran. Composed staging validation, main promotion, package release, runtime acceptance and deployment remained unaccepted or unrun.

## 2026-10-18 — Codex P2 annotation-text boundary correction

Codex review on PR head `36c8a30a42faef4d1235bd55a90ffffe496c7bf0` found that type-boundary detection could mistake `interface Adapter` inside an annotation string, or type-like text in a comment, for a real declaration. Fix commit `1bee6d6222f19d81258274250eac8d9729c8e541` masks comments, strings, character literals and text blocks before matching annotation and type-declaration boundaries. A new regression combines array-form `@DelegatesTo`, `@CompositionBoundary("interface Adapter")`, and a comment containing `interface CommentedAdapter`.

Corrected source `1bee6d6222f19d81258274250eac8d9729c8e541` passes focused lowerer tests (9 cases) and Java 25.0.1 / Gradle 9.6.1 `clean check javadoc jar`: 98 tests, zero failures/errors/skips, `verifyJarContents` passed. Javadoc has 100 missing-comment warnings. The local-only JAR SHA-256 is `c878968c177dc02b268d2d3653a386a441eaeb6bf6e59426b69f30d13f02cbfa`; it was not published. [The corrected exact-source report](evidence/2026-10-18-delegates-to-array-lowerer/results.json) records the P2 fix and source graph.

At this checkpoint the tested P2 fix is local but not yet pushed; the remote PR head remains `36c8a30a42faef4d1235bd55a90ffffe496c7bf0`, where the finding is still open. A fresh Codex review and Owner Self-Review are required after publishing the corrected source. Hosted exact-source CI, composed staging validation, package release, runtime acceptance, deployment and main promotion remain unaccepted or unrun.

## 2026-10-18 — Tavall PR Review role scanner correction

The installed `tavall-agent-pr-review` ROLE review found that the lowerer's `CLASS_PATTERN` could match `class Adapter` inside an annotation string or comment before the actual class declaration. Commit `e2d97a59d0fef95bc320b9e9f5b26276e44dd14d` masks comments and literals once, then uses the position-preserving masked source for both declaration matching and managed-annotation boundary checks. The regression keeps the prior `interface` cases and adds `class` text to the annotation string and comment.

The exact source passes Java 25.0.1 / Gradle 9.6.1 `clean check javadoc jar`: 98 tests, zero failures/errors/skips, all 9 lowerer tests, and `verifyJarContents`; Javadoc has 100 missing-comment warnings. The local-only JAR SHA-256 is `7a0b3d0eb2687330c5e49c7a0eed76f5c050fb037f78d3008d66147aeeca55b9`. [The refreshed exact-source report](evidence/2026-10-18-delegates-to-array-lowerer/results.json) records the pinned Logging and Custom Enum composites, test receipt, and distinct source/package/runtime boundaries.

The original P2 thread is replied to and resolved. The installed PR Review Agent manifest has no configured MCPs or tools, the dedicated Tavall GitHub Bot deployment record says `NOT DEPLOYED`, and the legacy host Bot service is inactive; no bot-hosted advisory review was available. The ROLE review was used in context; no Codex review was requested on the corrected head. PR #19 was ready and mergeable at evidence head `d7ed682` against `staging/platform@e689197` and merged at `8a9b90145daad94331e1ee7a6af3a1cecc9a75a9`. GitHub returned no status contexts or Actions runs on the current evidence head. Hosted exact-source CI, composed staging validation, package release, runtime acceptance, deployment and main promotion remain unaccepted or unrun.

## 2026-10-18 — PR #19 merged into persistent staging

[DI PR #19](https://github.com/TavallStudios/tavall-di/pull/19) merged into `staging/platform` on 2026-10-18 at `8a9b90145daad94331e1ee7a6af3a1cecc9a75a9`. The merge integrates the source branch, original staging reconciliation `0a3b059`, scanner fix `1bee6d6`, reviewed class-token correction `e2d97a5`, and evidence artifact `d7ed682`. At this checkpoint, staging root #26 was Draft; it later merged to `main` at `6e45442bcd89c0efdc9505d0e48975c7aefe8450`.

The exact tested code source `e2d97a59d0fef95bc320b9e9f5b26276e44dd14d` passes Java 25.0.1 / Gradle 9.6.1 `clean check javadoc jar` with the pinned Logging and Custom Enum source composites: 98 tests, zero failures/errors/skips, all 9 lowerer cases, and `verifyJarContents`. Javadoc has 100 missing-comment warnings. The local-only JAR SHA-256 is `7a0b3d0eb2687330c5e49c7a0eed76f5c050fb037f78d3008d66147aeeca55b9`. [The verified exact-source report](evidence/2026-10-18-delegates-to-array-lowerer/results.json) separates tested source from evidence/current head.

The GitHub Bot review provider is not deployed or configured in the connected installation; the installed Tavall PR Review Agent ROLE was used and its findings were fixed. No new Codex review was requested on the corrected source. GitHub returned no status contexts or Actions runs on the evidence head; hosted exact-source CI and composed staging validation remain unrun. No package release, runtime acceptance, deployment, or main promotion was performed.

## 2026-10-18 — PR #13 CI migration merged into persistent staging

[PR #13](https://github.com/TavallStudios/tavall-di/pull/13) merged into `staging/platform` at `01ec52f58e9870590b50232a4fad8cdd2cce0e67`. Its merge parents are the previous staging head `4b7522852b5d87918e6b7faa726ec86128d43870` and exact PR head `365ead2819721bddf02aaebdad094ad112686e26`.

The current PR head passes Java 25.0.1 / Gradle 9.6.1 `clean check assemble javadoc tavallCiArtifact` with exact Logging `e047172ab7c2be005baf0b2c4b46e230ead78417` and Custom Enum Java `2284168910f755a0d13a68fbc821cedc14d0a49b` source composites: 98 tests, zero failures/errors/skips, and `verifyJarContents` passed. The standard JAR and `tavallCiArtifact` SHA-256 is `7a0b3d0eb2687330c5e49c7a0eed76f5c050fb037f78d3008d66147aeeca55b9`. Javadoc completed with 100 missing-comment warnings. The [current-head local validation comment](https://github.com/TavallStudios/tavall-di/pull/13#issuecomment-6250781268) and passing Bot check `tavall-local/exact-source` record this result.

Hosted TCI did not execute because no active CONTROL developer environment is registered. The Bot check reports the actual local exact-source result and does not claim hosted TCI. No GitHub Actions run, package publication, consumer/runtime acceptance, deployment, or main promotion is claimed.

## 2026-10-18 — PR #14 constructor policy aligned and merged into staging

[PR #14](https://github.com/TavallStudios/tavall-di/pull/14) preserves its existing branch and exact policy fix. Its current PR head `f927073d35dc5083a6287b5ad088bfbabf892f16` is integrated into the updated `staging/platform` after PR #13. The corrected rule rejects managed constructor dependencies on ordinary types and builders, removes bytecode call-site scanning, and retains stable diagnostics.

Java 25.0.1 / Gradle 9.6.1 `clean check javadoc jar` passed on the exact PR head with the pinned Logging and Custom Enum source composites: 103 tests, zero failures/errors/skips, and `verifyJarContents` passed. The local-only JAR SHA-256 is `e324f44e1a3a5b56948145185b95a7f2dad8229bafbe48057716884f8b6d8458`. The [current-head local validation comment](https://github.com/TavallStudios/tavall-di/pull/14#issuecomment-6250781477) and passing Bot check `tavall-local/exact-source` record this result.

Hosted TCI did not execute because no active CONTROL developer environment is registered. No Actions run or Codex review was requested. Package publication, consumer/runtime acceptance, deployment, and main promotion remain separate.

## 2026-10-18 — Staging root #26 merged to main

Staging root PR #26 merged to `main` at `6e45442bcd89c0efdc9505d0e48975c7aefe8450`. The main merge has the former main head `c6ecaee3027e5e42f6a0fb11a40700925968498f` and staging head `87d35f8fee060128d82943c4d18b1a6aa06c6774` as parents. `staging/platform` was fast-forwarded to the same merge commit.

The integrated tree passed Java 25.0.1 / Gradle 9.6.1 `clean check javadoc jar`: 103 tests, zero failures/errors/skips, and `verifyJarContents`; the local JAR SHA-256 is `e324f44e1a3a5b56948145185b95a7f2dad8229bafbe48057716884f8b6d8458`. The Bot's `tavall-local/exact-source` check passed on staging head `87d35f8`, whose tree matches main's merge tree. Hosted TCI was not run because no active CONTROL developer environment exists. No GitHub Actions run or Codex review was used.

This promotes source only. No package was published, no named consumer/runtime acceptance was performed, and no deployment was needed for this non-deployable library.
