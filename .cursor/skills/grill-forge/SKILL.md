---
name: grill-forge
description: Relentlessly stress-test plans via batched questions while auto-generating ADRs and domain terms.
---

# Grill Forge

Eliminate assumptions by resolving open decisions in dependency order, documenting as answers arrive.

## Workflow

1. **Investigate:** Look up facts with tools first. Never ask what you can find.
2. **Design tree:** Decisions form a dependency tree. The **frontier** is the set of unblocked questions. Ask the whole frontier as one batch per round (structured question tool when available).
3. **Question format:** Numbered, with context, options, and a recommended default (`➡️`). Defer dependent questions to later rounds.
4. **Document immediately:** Agreed decisions → `ADR.md`; new domain terms → `GLOSSARY.md`; requirement changes → `requirements.md`. Create a file only when it has content.
5. **Finish:** Stop when the frontier is empty and the user approves the shared design.