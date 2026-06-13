# Presentation Generator

Operator guide. Behavioral spec: [requirements.md](requirements.md).

## Quick start

1. Copy [.env.example](.env.example) to `.env`.
2. Copy [presentation-keys.example.properties](presentation-keys.example.properties) to `presentation-keys.properties`.
3. Install and authenticate Gemini CLI: `npm install -g @google/gemini-cli`, then run `gemini` once to sign in.
4. Place `template.pptx` and `source.docx` at project root (or override paths in `.env`).
5. Run from project root: `./gradlew run` → `final_presentation.pptx` (step progress on stdout; errors only in logs).

**Prerequisites:** JDK 21 (Gradle toolchain auto-provision; `./gradlew -q javaToolchains`), Gemini CLI, `curl` on PATH. Optional: `PEXELS_API_KEY` for slide images (skipped when unset).

**Template:** `${variableName}` placeholders in single uninterrupted text runs. Key instructions: local `presentation-keys.properties` (see [presentation-keys.example.properties](presentation-keys.example.properties)).

## Commands

- `./gradlew run` — generate presentation (no CLI args)
- `./gradlew build` — unit tests always; full pipeline integration test only when `gemini` is available

## Environment

Variables and commented defaults: [.env.example](.env.example). Code defaults: [AppConfig.java](src/main/java/com/appfire/presentation/config/AppConfig.java).

## Troubleshooting

- **Gemini CLI not found:** `npm install -g @google/gemini-cli` or set `GEMINI_CLI_PATH`
- **Auth failure:** run `gemini` interactively once
- **Input not found:** check `TEMPLATE_PPTX_PATH`, `SOURCE_DOCX_PATH`
- **Presentation keys config not found:** copy `presentation-keys.example.properties` to `presentation-keys.properties`, or set `PRESENTATION_KEYS_PATH`
- **Toolchain:** `./gradlew -q javaToolchains`; install JDK 21 if auto-download fails
- **Validation failure:** fix missing keys, word-limit errors, or image queries (2-5 words)
- **Google Drive/Slides overflow:** keep `LAYOUT_NORMALIZE_ENABLED=true`; fix validation errors first
- **Prompt file not found:** run from project root (`./prompts/` must be visible)
- **Placeholders not replaced:** single text run per `${key}`; disable spell-check-as-you-type
- **No images:** set `PEXELS_API_KEY`; ensure `curl` is on PATH

Pipeline and validation rules: [requirements.md](requirements.md).
