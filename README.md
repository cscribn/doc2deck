# Presentation Generator

Java CLI that maps narrative content from a DOCX file onto a templated PPTX deck using Gemini.

See [requirements.md](requirements.md) for the full specification.

## Prerequisites

JDK 21 is provisioned automatically via Gradle toolchains. Confirm resolution:

```bash
./gradlew -q javaToolchains
```

## Setup

1. Copy `.env.example` to `.env`.
2. Set `GEMINI_API_KEY` in `.env` (from [Google AI Studio](https://aistudio.google.com/apikey)).
3. Place `source.pptx` and `source.docx` at the project root, or override paths in `.env`.

## Run

```bash
./gradlew run
```

Output is written to `final_presentation.pptx` by default.

## Build and test

```bash
./gradlew build
```

Unit tests run without an API key. The full pipeline integration test runs only when `GEMINI_API_KEY` is set in the environment.

## Environment variables

| Variable | Required | Default |
|----------|----------|---------|
| `GEMINI_API_KEY` | yes | - |
| `SOURCE_PPTX_PATH` | no | `source.pptx` |
| `SOURCE_DOCX_PATH` | no | `source.docx` |
| `OUTPUT_PPTX_PATH` | no | `final_presentation.pptx` |
| `GEMINI_MODEL` | no | `gemini-3.1-flash` |
| `GEMINI_MAX_RETRIES` | no | `3` |

## Pipeline

1. Extract slide structure and theme from `source.pptx`
2. Extract narrative blocks from `source.docx`
3. Build a Gemini prompt with slideshow constraints
4. Parse and validate the JSON slide plan
5. Rehydrate the template and write `final_presentation.pptx`

## Troubleshooting

| Problem | Resolution |
|---------|------------|
| Missing API key | Set `GEMINI_API_KEY` in `.env` |
| Input file not found | Verify `SOURCE_PPTX_PATH` and `SOURCE_DOCX_PATH` |
| Toolchain issues | Run `./gradlew -q javaToolchains` and install JDK 21 if auto-download fails |
| Validation failure | Check logs for bullet count, empty titles, or invalid slide indexes |
| Placeholder errors | Ensure template slides have TITLE and BODY placeholders |
