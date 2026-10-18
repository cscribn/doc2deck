---
name: project-scaffold
description: Use strictly when explicitly requested to generate a project by recursively parsing requirements.md and any linked sub-requirement documents.
---

# Project Scaffold

Scaffold a complete codebase from `requirements.md` and every recursively linked sub-requirement doc.

## Rules
* **Gate:** If `requirements.md` is missing, halt and ask the user.
* **Resolve fully:** Recursively read every relative Markdown link before writing code.
* **Full coverage:** Implement every feature, route, schema, and interface in the discovered specs. No speculative features.
* **Consistency:** Standard layout, entry points, configs; types and imports must match the specs exactly.
* **Audit:** Check all output against the specs for omissions.

## Order
Discover specs → infer stack, layout, schemas → scaffold configs and structure → implement modules and tests → audit completeness.
