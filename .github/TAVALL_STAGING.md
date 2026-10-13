# Tavall DI Staging Root

```text
<!-- tavall-staging:v1 -->
Type: REPOSITORY_INTEGRATION
State: ACTIVE
Branch: staging/platform
Parent: main
Promotion: MANUAL
ChildMergeTarget: staging/platform
```

This branch is the combined integration tree for Tavall DI APIs, provided-dependency factories, typed module identities/profiles, annotation processing, lifecycle behavior, and downstream compatibility. Child merges are integration for combined validation, not production promotion.

## Validated generation binding promotion

DI feature #20 and staging #12 reached main at `86de4c75a6b09c5922ef60a2fb06111c5244fda8`. Integrated exact-source CI `a46b149a-217c-4835-b8b9-c5d8b9d71394` passed all required checks. The same `staging/platform` branch continues as a persistent Draft integration root for active children #10, #11, #13, #14 and #19; their source and identities are preserved. This successor metadata is not another main promotion or deployment authorization.
