# Presentation Generator

Operator guide. Behavioral spec: [requirements.md](requirements.md).

## Quick start

1. Copy [.env.example](.env.example) to `.env`.
2. Copy [presentation-keys.example.properties](presentation-keys.example.properties) to `presentation-keys.properties`.
3. Configure an AI backend (default `AI_BACKEND=cursor_cli`):
   - **Cursor CLI:** Install from [Cursor CLI docs](https://cursor.com/docs/cli/overview), then `agent login` or set `CURSOR_API_KEY`.
   - **Google GenAI SDK:** Set `AI_BACKEND=google_genai` and `GOOGLE_API_KEY` (see [Google AI Studio](https://aistudio.google.com/app/apikey)).
4. Place `template.pptx` at project root and one or more `.docx` files in `sources/` (or override paths in `.env`).
5. Run from project root: `./gradlew bootRun` → `final_presentation.pptx` (default) or `final_presentation.html` when `DECK_FORMAT=html` (step progress on stdout; errors only in logs; full log in `presentation-generator.log`).

**HTML deck:** Set `DECK_FORMAT=html` in `.env`. Copy [html-deck-outline.example.properties](html-deck-outline.example.properties) to `html-deck-outline.properties`. PPTX template and `presentation-keys.properties` are not required in HTML mode. Reference UX: [sample_deck.html](sample_deck.html).

**Prerequisites:** JDK 21 (Gradle toolchain auto-provision; `./gradlew -q javaToolchains`), configured AI backend, `curl` on PATH. Optional: `PEXELS_API_KEY` for slide images (skipped when unset).

**Template:** `${variableName}` placeholders in single uninterrupted text runs. Key instructions and per-key limits: local `presentation-keys.properties` (see [presentation-keys.example.properties](presentation-keys.example.properties)). Voice/tone: `VOICE_STYLE_PATH` in `.env` (see `prompts/voice-styles/`). Generated decks target live presentation (short on-slide reinforcement), not infodeck pre-reads; use source DOCX for full detail. See [requirements.md](requirements.md).

**Template design** (advisory warnings at scan; never fail the run):
- 1-3 fonts; sans-serif for body (e.g. Inter, Open Sans); serif only for titles
- Max 6 colors plus black, white, greys; reuse one palette across decks
- 2-3 layouts (title + blank); no floodmarks (logos, slide numbers, copyright on every slide)
- Sentence case; check legibility at 50% zoom
- Share links and citations via a closing QR code, not on individual slides

## Commands

- `./gradlew bootRun` — generate presentation (no CLI args; `./gradlew run` aliases to `bootRun`)
- `./gradlew build` — unit tests always; full pipeline integration test when the configured AI backend is available

## Environment

Variables and commented defaults: [.env.example](.env.example). Code defaults: [AppConfig.java](src/main/java/com/appfire/presentation/config/AppConfig.java).

## Troubleshooting

- **Cursor CLI not found:** install Cursor CLI or set `CURSOR_CLI_PATH`
- **Cursor auth failure:** run `agent login` or set `CURSOR_API_KEY`
- **Google GenAI auth failure:** set valid `GOOGLE_API_KEY` when `AI_BACKEND=google_genai`
- **Input not found:** check `TEMPLATE_PPTX_PATH` (PPTX mode), `SOURCES_DIR` (must contain at least one `.docx`)
- **Presentation keys config not found (PPTX):** copy `presentation-keys.example.properties` to `presentation-keys.properties`, or set `PRESENTATION_KEYS_PATH`
- **HTML outline not found:** copy `html-deck-outline.example.properties` to `html-deck-outline.properties`, or set `HTML_DECK_OUTLINE_PATH`
- **Gradle fails with `What went wrong: 27`:** Gradle 8.x cannot run on JDK 27; this project uses Gradle 9.8+ (wrapper). Re-run `./gradlew bootRun`. If you must stay on Gradle 8, set `JAVA_HOME` to JDK 21–24 for the Gradle process.
- **Toolchain:** `./gradlew -q javaToolchains`; install JDK 21 if auto-download fails
- **Validation failure:** fix missing keys, word-limit errors, or image queries (2-5 words)
- **Google Drive/Slides overflow:** keep `LAYOUT_NORMALIZE_ENABLED=true`; fix validation errors first
- **Prompt file not found:** run from project root (`./prompts/` must be visible)
- **Placeholders not replaced:** single text run per `${key}`; disable spell-check-as-you-type
- **No images:** set `PEXELS_API_KEY`; ensure `curl` is on PATH
- **`Template design:` warnings:** advisory only (fonts >3, layouts used >3, non-grey colors >6); simplify the template to clear them

Pipeline and validation rules: [requirements.md](requirements.md).
