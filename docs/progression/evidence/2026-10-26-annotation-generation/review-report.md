# Annotation bootstrap generation binding

At source `aa5fe3c52df55a7265abce398b1687fa2790c1be`, annotation publication creates one metadata-owned instance and binds a new generation-owned consumer to that metadata's map before declared-input or lifecycle initialization. Existing publication order, alias identity and rollback remain in place. Ordinary consumers keep fieldless typed access; no map field, static fallback or second container is introduced.

The new lifecycle regression fails on preceding main d16c1de because typed access executes while its consumer is unbound. All 107 owning DI tests pass after the repair, including four lifecycle/idempotence, reload, clear and failed-start rollback regressions. The producer-declared logging/enum graph, default 1.2.1 build, production/test compile, JAR contents, Javadoc and CI artifact staging pass locally.

Actual Minecraft consumer at `5946ad26a2e6042304bdf55368d6f7ec99a5fb33` with explicit candidate DI override passes 34 proxy cases, 86 Runtime API cases and the canonical architecture gate: 121 cases without failures or skips. This provisional integration graph is recorded separately from the producer CI graph; MC final declared pinning and full/native quality remain separate.

The owning technical document and Notion twin were amended with saved-state readback. Report assets are sanitized immutable source links. No native CI, public release publication,service deployment or full original campaign completion is claimed.
