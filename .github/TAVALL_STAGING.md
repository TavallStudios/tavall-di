# Tavall DI Staging Root

```text
<!-- tavall-staging:v1 -->
Type: STAGING
State: ACTIVE
Branch: staging/platform
Parent: main
Promotion: MANUAL
ChildMergeTarget: staging/platform
```

This branch is the combined integration tree for Tavall DI APIs, provided-dependency factories, typed module identities/profiles, annotation processing, lifecycle behavior, and downstream compatibility. Child merges are integration for combined validation, not production promotion.

## Validated generation binding promotion

DI feature #20 and staging #12 reached main at `86de4c75a6b09c5922ef60a2fb06111c5244fda8`. Integrated exact-source CI `a46b149a-217c-4835-b8b9-c5d8b9d71394` passed all required checks. The same `staging/platform` branch continues as a persistent Draft integration root for active children #10, #11, #13, #14 and #19; their source and identities are preserved. This successor metadata is not another main promotion or deployment authorization.

## Validated 1.2.0 promotion (2026-10-15)

Children #11 (typed module identities and effective profiles) and #23 (retired-generation resurrection fence) were integrated and promoted through staging #21 to main at `fb985397d75bcc6c4490b972746ce10b6b405c68` as **Tavall DI 1.2.0** (RELEASE). Exact-source CI passed on #11 `a8505d9d`, #23 `f6e1b8dc`, the staging head `f85b42f3` and main; all produced the same 1.2.0 artifact digest `6ef820e5eae5db057143388bc3d5f33fad65a0ebfc20e9c40e82d1a66018e440`. Unreleased branches use the canonical `0.<iteration>.<sub>-SNAPSHOT` versions. The same `staging/platform` branch continues for active children #10, #13, #14 and #19; their sources are preserved and not declared accepted.
