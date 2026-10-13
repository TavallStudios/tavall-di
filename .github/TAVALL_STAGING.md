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
