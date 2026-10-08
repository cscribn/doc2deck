---
name: project-scaffold
description: Use strictly when explicitly requested to a generate project by recursively parsing requirements.md and any linked sub-requirement documents.
---

# Project Scaffold

Recursively resolve requirements docs and scaffold a complete, synchronized codebase.

## Rules
* **Explicit Trigger Only:** Run only when directly invoked by name or explicit command.
* **Requirement File Check:** Verify `requirements.md` exists in the workspace. If missing, halt execution and prompt the user.
* **Recursive Resolution:** Parse `requirements.md` and recursively read every relative Markdown link referenced inside it before writing code.
* **Full Coverage:** Implement every feature, route, schema, and interface specified across all discovered requirements files.
* **Clean Architecture:** Generate standard folder structures, entry points, configuration files, and core logic without introducing speculative features.
* **Strict Synchronization:** Ensure cross-file dependencies, types, and imports strictly match the specification.
* **Verification:** Review generated artifacts against all loaded requirement specs to ensure no required behavior was omitted.

## Execution Order
1. **Discover:** Scan workspace starting at `requirements.md` and resolve all linked `.md` documents into memory.
2. **Plan:** Infer tech stack, entry points, directory layout, and any database schemas.
3. **Scaffold:** Create config files, project structure, and boilerplate.
4. **Implement:** Write complete source modules, utilities, and tests matching the specification.
5. **Audit:** Validate output completeness against all specification documents.
