---
name: sync-requirements
description: Use only when explicitly requested to propagate uncommitted or recently committed requirement changes across code, tests, and docs, prioritizing human code readability.
---

# Sync Requirements

Propagate spec changes from `requirements.md` and linked specs across code, tests, and docs.

## Rules
* **Diff first:** Review uncommitted (`git diff HEAD`) and recent (`git diff HEAD~1`, `git log -p`) spec changes; extract added/modified/removed requirements. List affected code, test, and doc files before editing.
* **Code:** Simplify. Shallow nesting, no clever abstractions, clear naming, comments only where non-obvious.
* **Tests and docs:** Update to the new behavior. Never suppress or delete failing tests.
* **Verify:** Run affected tests; confirm they pass and readability improved.

## Watch for
* Spec drift between requirements and code; broken cross-references in specs.
* Stale or missing test assertions; docs referencing retired requirements.
