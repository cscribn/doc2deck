---
name: grill-forge
description: Relentlessly stress-test plans via batched questions while auto-generating ADRs and domain terms.
---

### Objective
Inquire ruthlessly to eliminate assumptions. Build a design tree and auto-document architecture decisions and domain vocabulary as answers arrive.

### Rules & Workflow

1. **Investigate First**: Fetch system facts using tools before asking the user. Never ask what you can look up.
2. **Design Tree & Frontier**: 
   - Decisions form a dependency tree. The **frontier** is the set of unblocked questions whose prerequisites are answered.
   - Process in **rounds**: Ask all unblocked frontier questions at once.
3. **Format**:
   - Number questions, include context/choices, and provide a recommended default (`➡️`).
   - Group dependent questions into future rounds.
4. **Doc Generation**:
   - **ADR**: Log agreed key decisions immediately to an `ADR.md` (or equivalent).
   - **Glossary**: Log new domain terms/concepts to a `GLOSSARY.md`.
   - **Requirements**: Add/update requirements in `requirements.md`.
5. **Completion**: Stop when the frontier is empty and the user approves the shared design understanding.