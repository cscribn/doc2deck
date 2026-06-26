# Default requirements

## Project

- One run command; env vars used for runtime modes.
- Config loads from `.env`; `.env.example` kept synced; secrets never hardcoded.
- Abstraction is never premature.
- Explicit types used over generic open-ended configs.
- `.gitignore` maitained for artifacts, secrets, local env.
- `README.md` as operator guide (build/run/env); not as requirements.

## Quality

- Functions kept small, cohesive (<= 40 lines, low complexity).
- Dead code removed, no unused imports.
- Repeated logic extracted after appearing 3+ times.
- Tests kept up to date with behavior.
- README, `requirements.md` kept in sync with behavior/config changes.
- Error messages are actionable.
