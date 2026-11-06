---
name: code-reconcile
description: Use strictly when explicitly requested to strip dead code, slop, and defensive bloat, keep requirements.md and linked requirement documents completely aligned with codebase behavior, and synchronize construct identifiers (files, classes, functions) with their underlying data and functionality.
---

# Code Reconcile

Purge bloat while keeping `requirements.md`, linked requirement docs, construct names, and code behavior in sync.

## Rules
- **Zero behavior changes:** Keep public runtime behavior, outputs, and feature logic intact.
- **Delete, don't abstract:** No new patterns, wrappers, or unnecessary abstractions.
- **Semantic alignment:** Rename files, classes, functions, variables, and data structures to accurately reflect their true data and runtime responsibilities across the entire codebase.
- **Sync requirements:** Add behavior implemented in code but missing from specs; drop references to purged features; update any renamed interfaces or references in requirement docs.
- **Verify:** Run tests and build checks; revert immediately on failure.

## Targets
- **Slop:** Self-explanatory comments, defensive guards/exception handling on internal paths, type-checker overrides.
- **Dead code:** Unreferenced variables, imports, internal functions, exports, unreachable branches.
- **Duplication:** Merge identical helpers or loops into one canonical version.
- **Doc drift:** Code/spec discrepancies.
- **Construct misnomers & drift:**
  - **Files & modules:** File names that contradict or obscure the primary service, data entity, or logic they host.
  - **Classes & types:** Class, interface, or struct names that no longer map to the underlying data shape or role.
  - **Functions & methods:** Verb-noun pairs that misstate what the function returns, modifies, or coordinates.
  - **Data constructs:** Misleading variable, parameter, table, schema, and field names whose names have diverged from the actual values or states they represent.
