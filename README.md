# Presentation Generator

Java CLI that fills a PowerPoint template with content synthesized from a DOCX file using the Gemini CLI.

See [requirements.md](requirements.md) for the full specification.

## Prerequisites

- JDK 21 (provisioned automatically via Gradle toolchains)
- [Gemini CLI](https://github.com/google-gemini/gemini-cli) installed and authenticated
- Optional: [Pexels API key](https://www.pexels.com/api/) for template image slots

Confirm JDK resolution:

```bash
./gradlew -q javaToolchains
```

Install and authenticate the Gemini CLI:

```bash
npm install -g @google/gemini-cli
gemini  # follow prompts to sign in
```

## Setup

1. Copy `.env.example` to `.env`.
2. Place `template.pptx` and `source.docx` at the project root, or override paths in `.env`.
3. Optionally set `GEMINI_CLI_PATH` if `gemini` is not on your PATH.
4. Optionally set `PEXELS_API_KEY` for image slots (presentations generate without images when unset).

Your `template.pptx` must contain `${variableName}` placeholders matching the presentation keys defined in [requirements.md](requirements.md). Each placeholder should be a single uninterrupted text run in PowerPoint.

## Run

```bash
./gradlew run
```

Output is written to `final_presentation.pptx` by default.

## Build and test

```bash
./gradlew build
```

Unit tests run without the Gemini CLI. The full pipeline integration test runs only when the `gemini` command is available and authenticated.

## Environment variables

| Variable | Required | Default |
|----------|----------|---------|
| `GEMINI_CLI_PATH` | no | `gemini` |
| `TEMPLATE_PPTX_PATH` | no | `template.pptx` |
| `SOURCE_DOCX_PATH` | no | `source.docx` |
| `OUTPUT_PPTX_PATH` | no | `final_presentation.pptx` |
| `GEMINI_MODEL` | no | `gemini-3.1-flash` |
| `GEMINI_MAX_RETRIES` | no | `3` |
| `PEXELS_API_KEY` | no | (empty) |
| `IMAGE_CACHE_DIR` | no | `.cache/images` |

## Pipeline

1. Extract narrative blocks from `source.docx`
2. Scan `template.pptx` for `${variableName}` placeholders and image anchors
3. Build a Gemini prompt from DOCX content and presentation key definitions
4. Invoke the Gemini CLI and parse the JSON key map
5. Validate required keys and image query format
6. Replace text placeholders via docx4j (`PptxTemplateReplacer`)
7. Acquire Pexels images for image keys (`ImageAcquisitionService`)
8. Insert images at anchor locations (`ImageInserter`)
9. Write `final_presentation.pptx`

## Troubleshooting

| Problem | Resolution |
|---------|------------|
| Gemini CLI not found | Install with `npm install -g @google/gemini-cli` or set `GEMINI_CLI_PATH` |
| Gemini CLI auth failure | Run `gemini` interactively once to sign in |
| Input file not found | Verify `TEMPLATE_PPTX_PATH` and `SOURCE_DOCX_PATH` |
| Toolchain issues | Run `./gradlew -q javaToolchains` and install JDK 21 if auto-download fails |
| Validation failure | Check logs for missing keys or invalid image queries (must be 2-5 words) |
| Prompt file not found | Run from the project root so `./prompts/` is visible |
| Placeholders not replaced | Ensure each `${key}` is a single text run; disable spell-check-as-you-type in PowerPoint |
| No slide images | Set `PEXELS_API_KEY` in `.env`; check logs for Pexels fetch warnings |
| SSL/certificate errors from Java HTTP | Corporate SSL inspection may block Java HTTPS; the app falls back to `curl` automatically |
