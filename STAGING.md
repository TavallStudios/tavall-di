# Tavall DI staging

This is the persistent repository staging work surface on `staging/platform`, targeting `main`.

## Generation 1

- Baseline: Tavall DI main `d5437cc7212910142d3943e5d2a8a36c598e78a0` (2026-11-06).
- Previous staging head: `d16c1de2c205f3c72fe63cd5a2765c3b1bf55368`; it was a strict ancestor of main, so the staging branch was fast-forwarded without dropping work.
- This manifest records the refreshed root. The root PR to `main` remains Draft until exact-head validation passes.

## Intended composition

There are no focused child PRs attached to this staging root at creation. Add child work through a focused PR targeting `staging/platform` and record its exact head, status, and CI evidence here.

## Promotion and runtime boundaries

Promotion is NOT_READY until the root PR has exact-head Tavall CI evidence and all intended children are integrated and validated. This staging root does not change `main` or any runtime by itself.
