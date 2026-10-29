# Google Gen AI SDK

## Prompts

- Bullets > paragraphs; fragments OK.
- One instruction block; correctness > creativity; no speculation.
- Dashes OK; no em/en dashes.
- One-shot prompt/response; no unsolicited content.

## Client

- Auth: `GOOGLE_API_KEY`.
- `models.generate_content(model=..., contents=prompt_text)` retries up to max attempts with exponential backoff (retry base to max delay).
- Errors: 429 → exp backoff; 503/5xx/408/Timeout/Malformed JSON/200+empty candidates → jittered retry; 400 → log prompt + model ID, fail; 401/403 → fail with re-auth guidance.
- Output: Return `response.text` if non-empty, else joined candidate text parts; else raise empty-text error (retried).
- Logging: Stderr retry notices if `JERBS_VERBOSE=true`. Exhausted retries raise `RuntimeError` naming last error.
- Model: Fixed; no fallback across attempts.
