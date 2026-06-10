# Presentation Generator

Java CLI that maps narrative content from a DOCX file onto a templated PPTX deck using the Gemini CLI.

See [requirements.md](requirements.md) for the full specification.

## Prerequisites

- JDK 21 (provisioned automatically via Gradle toolchains)
- [Gemini CLI](https://github.com/google-gemini/gemini-cli) installed and authenticated

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

## Pipeline

1. Extract slide structure and theme from `source.pptx`
2. Extract narrative blocks from `source.docx`
3. Build a Gemini prompt with slideshow constraints
4. Invoke the Gemini CLI and parse the JSON slide plan
5. Rehydrate the template and write `final_presentation.pptx`

## Troubleshooting

| Problem | Resolution |
|---------|------------|
| Gemini CLI not found | Install with `npm install -g @google/gemini-cli` or set `GEMINI_CLI_PATH` |
| Gemini CLI auth failure | Run `gemini` interactively once to sign in |
| Input file not found | Verify `SOURCE_PPTX_PATH` and `SOURCE_DOCX_PATH` |
| Toolchain issues | Run `./gradlew -q javaToolchains` and install JDK 21 if auto-download fails |
| Validation failure | Check logs for bullet count, empty titles, or invalid slide indexes |
| Placeholder errors | Ensure template slides have TITLE and BODY placeholders |
