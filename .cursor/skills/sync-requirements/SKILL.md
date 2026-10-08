---
name: sync-requirements
description: Use when requested to propagate uncommitted or recently committed changes in master or linked requirement documents across code, tests, and docs, prioritizing human code readability.
---

# Sync Requirements

Propagate uncommitted or recently committed spec changes from `requirements.md` and linked spec files across code, tests, and docs, prioritizing readability.

## Rules
* **Explicit Trigger Only:** Run only when invoked by name or explicit command.
* **Audit Spec:** Check `requirements.md` and linked spec files for uncommitted (`git diff HEAD`) and recent commit (`git log -p`, `git diff HEAD~1`) changes.
* **Diff & Plan:** Extract modified/added/removed requirements before editing. List all affected code, test, and doc files first.
* **Sync Domains:**
  * **Logic:** Simplify code. Avoid deep nesting, complex inheritance, and heavy abstractions. Use clear naming and brief inline comments.
  * **Tests & Docs:** Update tests to reflect new behavior; never suppress/delete failing tests. Keep docs aligned.
* **Verify:** Run affected tests. Confirm code passes and readability is improved.

## Focus Areas
* **Spec Drift:** Discrepancies between requirements and codebase.
* **Broken Cross-Refs:** Invalid paths/references in requirement docs.
* **Test Alignment:** Stale or missing test assertions for new spec constraints.
* **Doc Staleness:** Outdated docs referencing retired requirements.
* **Code Clarity:** Superfluous patterns or clever code impacting readability.
