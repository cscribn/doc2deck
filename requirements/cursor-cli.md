# Cursor Agent CLI

## Cursor Agent CLI Prompts

- Bullets > paragraphs; fragments OK.
- One instruction block; correctness > creativity; no speculation.
- Dashes OK; avoid em/en dashes.
- One-shot prompt/response; no unsolicited content.

## Cursor Agent CLI Usage

- One CLI invocation per prompt; no chaining/streaming.
- CLI output treated as plain text, not structured JSON.
- Large inputs: workspace/`--add-dir` + file tools, not argv (many builds cap prompt argv ~1 KB).
