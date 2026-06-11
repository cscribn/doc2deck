# Presentation Generator

Java CLI that maps narrative content from a DOCX file onto a templated PPTX deck using the Gemini CLI.

See [requirements.md](requirements.md) for the full specification.

## Prerequisites

- JDK 21 (provisioned automatically via Gradle toolchains)
- [Gemini CLI](https://github.com/google-gemini/gemini-cli) installed and authenticated
- Optional: [Pexels API key](https://www.pexels.com/api/) for slide images

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
2. Place `source.pptx` and `source.docx` at the project root, or override paths in `.env`.
3. Optionally set `GEMINI_CLI_PATH` if `gemini` is not on your PATH.
4. Optionally set `PEXELS_API_KEY` for slide images (presentations generate without images when unset).

Use a template PPTX whose slide master includes varied layouts (title+content, two-column, picture) for best results.

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
| `SOURCE_PPTX_PATH` | no | `source.pptx` |
| `SOURCE_DOCX_PATH` | no | `source.docx` |
| `OUTPUT_PPTX_PATH` | no | `final_presentation.pptx` |
| `GEMINI_MODEL` | no | `gemini-3.1-flash` |
| `GEMINI_MAX_RETRIES` | no | `3` |
| `PEXELS_API_KEY` | no | (empty) |
| `IMAGE_CACHE_DIR` | no | `.cache/images` |

## Pipeline

1. Extract slide structure, layout catalog, and theme from `source.pptx`
2. Extract narrative blocks from `source.docx`
3. Build a Gemini prompt from `prompts/*.md` templates with slideshow constraints, layout catalog, and content variation rules
4. Invoke the Gemini CLI and parse the JSON slide plan
5. Filter meta-instruction slides (`MetaSlideFilter`)
6. Validate the response (`ResponseValidator`)
7. Acquire Pexels images for slides with `includeImage` (`ImageAcquisitionService`)
8. Rehydrate the template with layout selection, native bullets, and images (`PptxRehydrator`)
9. Write `final_presentation.pptx`

## Troubleshooting

| Problem | Resolution |
|---------|------------|
| Gemini CLI not found | Install with `npm install -g @google/gemini-cli` or set `GEMINI_CLI_PATH` |
| Gemini CLI auth failure | Run `gemini` interactively once to sign in |
| Input file not found | Verify `SOURCE_PPTX_PATH` and `SOURCE_DOCX_PATH` |
| Toolchain issues | Run `./gradlew -q javaToolchains` and install JDK 21 if auto-download fails |
| Validation failure | Check logs for bullet count, empty titles, or invalid slide indexes |
| Prompt file not found | Run from the project root so `./prompts/` is visible, or verify all `prompt_*.md` files exist |
| Placeholder errors | Ensure template slides have TITLE and BODY placeholders |
| No slide images | Set `PEXELS_API_KEY` in `.env`; check logs for Pexels fetch warnings |
| Images overlap text | Template must include split media layouts (e.g. `MAIN_POINT_1`); the app detects media regions from layout geometry |
| SSL/certificate errors from Java HTTP | Corporate SSL inspection (e.g. Netskope) may block Java HTTPS; the app falls back to `curl` automatically |
| `Acquired 0 of N requested image(s)` | Check logs for Pexels API errors; verify `curl` is installed for SSL fallback |
| Layout fallback warnings | Ensure `source.pptx` master includes picture and two-column layouts |
