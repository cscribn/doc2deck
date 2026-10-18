---
name: code-reconcile
description: Use strictly when explicitly requested to strip dead code, slop, and defensive bloat while keeping requirements.md and linked requirement documents completely aligned with codebase behavior.
---

# Code Reconcile

Purge bloat while keeping `requirements.md`, linked requirement docs, and code in sync.

## Rules
* **Zero behavior changes:** Keep interfaces, outputs, and feature logic intact.
* **Delete, don't abstract:** No new patterns or abstractions.
* **Sync requirements:** Add behavior implemented in code but missing from specs; drop references to purged features.
* **Verify:** Run tests; revert on failure.

## Targets
* **Slop:** Self-explanatory comments, defensive guards/exception handling on internal paths, type-checker overrides.
* **Dead code:** Unreferenced variables, imports, internal functions, exports, unreachable branches.
* **Duplication:** Merge identical helpers or loops into one canonical version.
* **Doc drift:** Code/spec discrepancies.
