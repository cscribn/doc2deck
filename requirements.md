# Requirements

This document is the authoritative functional specification. [README.md](README.md) is the operator guide (build, run, and environment examples).

---

## 1. Project Overview & Architecture

### 1.1 Objective

A Java command-line application that fills a fixed PowerPoint template with content synthesized from a DOCX source document using Gemini. Text placeholders use `${variableName}` keys replaced via docx4j; image placeholders are filled with Pexels stock photos.

### 1.2 Tech Stack Requirements

- **JDK 21 (LTS)**: Java runtime for main program logic and output formatting.
- **Gradle**: Use the Gradle Wrapper (`./gradlew`) for build, run, and test. Call bare `java`, `javac`, or `gradle` only when troubleshooting (for example bootstrapping a missing wrapper or diagnosing toolchain resolution).
- **Runtime availability**: JDK 21 (Eclipse Temurin / Adoptium) is declared via Gradle Java toolchains in `build.gradle.kts`. The Foojay toolchain resolver in `settings.gradle.kts` auto-downloads a matching JDK when one is not installed locally. Confirm toolchain resolution with `./gradlew -q javaToolchains`.

### 1.3 Input & Output Artifacts

1. **Template PPTX (`template.pptx`):** Pre-designed deck with `${variableName}` placeholders for all presentation keys (text and image slots).
2. **Source DOCX (`source.docx`):** Contains the comprehensive narrative data, detailed descriptions, and foundational facts regarding the subject matter.
3. **Output PPTX (`final_presentation.pptx`):** The filled presentation with text keys replaced and images inserted.

---

## 2. Core Workflow Engine

The Java application must process files using a strict serial pipeline:

1. **DOCX Extraction:** Parse `source.docx` to extract text blocks, narrative structure, and data elements.
2. **Template Scan:** Scan `template.pptx` for `${...}` placeholders and image-key anchor locations.
3. **Payload Preparation:** Construct a Gemini prompt from DOCX content and presentation key definitions.
4. **Gemini Interaction:** Dispatch the payload via the Gemini CLI (`gemini` command). Request a structured JSON response with key values.
5. **Validation:** Verify all required keys are present and image queries meet format rules.
6. **Text Replacement:** Use docx4j `SlidePart.variableReplace()` for text keys (excluding image keys).
7. **Image Acquisition:** Fetch Pexels images using Gemini-provided search phrases for image keys.
8. **Image Insertion:** Insert images at pre-scanned anchor locations using Apache POI.
9. **Output:** Write `final_presentation.pptx`.

---

## 3. Gemini System Prompt & Constraints

The application must inject system instructions into the prompt payload. These boundaries govern content generation:

- Use only facts from the provided DOCX source text. No fabrication.
- Fill every required presentation key with grounded content.
- Return JSON only with no markdown fences and no commentary.
- Stateless: use only information in the current prompt.
- `shortProjectDescription` must be a compact fragment only; Java prepends `"Flow Monorepo API - "`.
- Image keys must be 2-5 word Pexels search phrases.

---

## 4. Environment and Configuration

Configuration is loaded from environment variables at startup. The application reads a `.env` file in the working directory when present (key=value lines, `#` comments ignored). Operators copy `.env.example` to `.env` and fill in secrets locally. Never hardcode secrets in source.

| Variable | Required | Default | Purpose |
|----------|----------|---------|---------|
| `GEMINI_CLI_PATH` | no | `gemini` | Path to the Gemini CLI executable |
| `TEMPLATE_PPTX_PATH` | no | `template.pptx` | Template deck with `${variableName}` placeholders |
| `SOURCE_DOCX_PATH` | no | `source.docx` | Narrative source document |
| `OUTPUT_PPTX_PATH` | no | `final_presentation.pptx` | Generated output path |
| `GEMINI_MODEL` | no | `gemini-3.1-flash` | Gemini model identifier passed to the CLI |
| `GEMINI_MAX_RETRIES` | no | `3` | Maximum retry attempts for transient CLI failures |
| `PEXELS_API_KEY` | no | (empty) | Pexels API key for slide image acquisition; images skipped when unset |
| `IMAGE_CACHE_DIR` | no | `.cache/images` | Local cache directory for downloaded Pexels images |

**Startup behavior:**

- Fail fast with an actionable message when the Gemini CLI is missing, not executable, or fails a version check.
- Fail fast when `TEMPLATE_PPTX_PATH` or `SOURCE_DOCX_PATH` does not exist or is unreadable.
- Log resolved paths and model at INFO level before pipeline execution.

**Run command:** `./gradlew run` (no trailing targets or CLI arguments).

---

## 5. Project Layout and Dependencies

### 5.1 Package structure

Root package: `com.appfire.presentation`

```
prompts/
  prompt_core_rules.md
  prompt_presentation_keys.md
  prompt_image_keys.md
  prompt_docx_content.md
  prompt_output_contract.md
src/main/java/com/appfire/presentation/
  Application.java
  config/AppConfig.java
  extraction/DocxExtractor.java
  llm/PromptBuilder.java
  llm/PromptLoader.java
  llm/GeminiClient.java
  llm/ResponseValidator.java
  template/TemplateScanner.java
  template/PptxTemplateReplacer.java
  template/ImageInserter.java
  images/ImageAcquisitionService.java
  model/
src/test/java/com/appfire/presentation/
  (mirrors main package)
```

### 5.2 Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| Apache POI `poi-ooxml` | 5.4.1 | DOCX parsing and PPTX image insertion |
| docx4j `docx4j-JAXB-ReferenceImpl` | 11.5.7 | PPTX `${variableName}` text replacement |
| Jackson `jackson-databind` | 2.19.0 | JSON serialization for Gemini CLI responses |
| JUnit 5 + Mockito | 5.12.2 / 5.17.0 | Unit and integration tests |
| `junit-platform-launcher` | (BOM) | Required test runtime for Gradle JUnit Platform |
| SLF4J + `slf4j-simple` | 2.0.17 | Structured logging |

Gemini is invoked via the external `gemini` CLI (not the Java SDK). Authentication is handled by the CLI itself; no API key is required in `.env`.

### 5.3 Build tooling

- `build.gradle.kts` with Java 21 toolchain and `application` plugin
- `settings.gradle.kts` with Foojay toolchain resolver plugin
- Committed Gradle Wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/`)
- Main class: `com.appfire.presentation.Application`

---

## 6. Extraction Specifications

### 6.1 DOCX extraction (`DocxExtractor`)

- Open `source.docx` via `XWPFDocument`.
- Emit an ordered list of `ContentBlock` records preserving document order:
  - Heading blocks: level (1-6) and text
  - Paragraph blocks: plain text
  - List blocks: bullet or numbered items
  - Table blocks: rows as pipe-delimited text
- Output: `DocumentContent` record with blocks and a flat text summary for prompt inclusion.

### 6.2 Template scan (`TemplateScanner`)

- Open `template.pptx` via Apache POI `XMLSlideShow`.
- Detect all `${variableName}` tokens across text shapes and table cells.
- Record image-key anchor locations (slide index, shape ID, bounds) for image keys.
- Warn when placeholders may be split across multiple XML text runs.

---

## 7. Gemini Prompt and JSON Contract

### 7.1 Prompt construction rules

Gemini prompt templates live in the project-root `prompts/` directory as Markdown files named `prompt_<two_word_description>.md`. `PromptLoader` reads and caches these files at runtime (relative to the working directory). `PromptBuilder` loads static rule and contract templates, injects dynamic DOCX context and template key list via `{{PLACEHOLDER}}` substitution, and concatenates sections in order.

1. Number all instruction steps in the prompt.
2. Anchor every content claim to text extracted from the DOCX (no fabrication).
3. Require a JSON-only response with no markdown fences and no commentary (`prompt_output_contract.md`).
4. Instruct the model to be stateless: use only information in the current prompt.

### 7.2 Token limit strategy

When DOCX content exceeds approximately 100,000 characters, truncate the flat summary to the first 100,000 characters and append a truncation warning in the prompt.

### 7.3 Presentation keys

**Text keys (17 required + 1 optional):**

| Key | Guidance |
|-----|----------|
| `shortProjectDescription` | Compact fragment; Java prepends `"Flow Monorepo API - "` |
| `problem1`, `problem2` | Problem bullets |
| `persona` | Who has this problem |
| `currentSolution1`, `currentSolution2` | Current workaround bullets |
| `scale`, `whyNow`, `ifUnsolved` | 1-2 sentences each |
| `architectureApproach`, `approachReasons` | 2-3 sentences each |
| `personaBetterment`, `appfireBetterment` | 1-2 sentences each |
| `estimatedImpact` | 2-3 sentences with rationale |
| `currentState` | 1-2 sentences |
| `sprintsToDeliver` | Single bullet (2-week sprints) |
| `nonDevCosts` | Optional; 1-2 sentences |

**Image keys (3 required):**

| Key | Description |
|-----|-------------|
| `problemSolvingImg` | Pexels query: difficult problem solved by engineers |
| `architectureApproachImg` | Pexels query: complex architecture being planned |
| `valueImpactImg` | Pexels query: engineers delivering significant impact |

### 7.4 Response JSON schema

```json
{
  "keys": {
    "shortProjectDescription": "string",
    "problem1": "string",
    "problemSolvingImg": "string"
  },
  "sourceRefs": { "problem1": [0, 2] },
  "warnings": ["string"]
}
```

---

## 8. Response Validation

`ResponseValidator` performs deterministic checks before template filling.

**Critical failures (block output write, exit non-zero):**

- JSON parse failure or missing `keys` object
- Any required text or image key missing or blank
- Image key query not 2-5 words

**Advisory warnings (log, continue when no critical failures):**

- Empty `sourceRefs` per key
- Template placeholders without Gemini values
- Gemini keys not found in template scan
- `nonDevCosts` absent
- Split-run placeholder warnings from template scan

On critical failure, log each failed check with a concrete example. Do not write `OUTPUT_PPTX_PATH`.

---

## 9. Template Fill Specifications

Pipeline after validation: text replace, then image acquire, then image insert.

### 9.1 Text replacement (`PptxTemplateReplacer`)

- Load `template.pptx` with docx4j `PresentationMLPackage`.
- Build replacement map from validated text keys; prepend project title prefix for `shortProjectDescription`.
- Exclude image keys from text replacement.
- Apply `variableReplace()` on every slide.
- Save to a temporary file.

### 9.2 Image acquisition (`ImageAcquisitionService`)

- Search Pexels using image-key query values, cache results under `IMAGE_CACHE_DIR`.
- When `PEXELS_API_KEY` is unset or fetch fails, log a warning and continue without the image.

### 9.3 Image insertion (`ImageInserter`)

- Open the text-replaced PPTX with Apache POI.
- For each image key, locate the pre-scanned anchor (or re-scan for `${key}` token).
- Insert picture at anchor bounds; clear placeholder text.
- Write final output to `OUTPUT_PPTX_PATH`.

### 9.4 Template authoring

- Each `${variableName}` must be in a single PowerPoint text run.
- Disable "Check spelling as you type" when authoring templates to avoid split runs.

---

## 10. Error Handling and Resilience

- **GeminiClient:** Invokes `gemini -p` in headless mode with prompt on stdin, `--output-format json`, `--approval-mode plan`, and `--skip-trust`. Exponential backoff starting at 1 second, capped at 30 seconds, up to `GEMINI_MAX_RETRIES` attempts. Retry on rate limits, timeouts, and transient CLI failures.
- **Logging:** Pair every ERROR log with actionable resolution steps (for example, "Install gemini CLI with npm install -g @google/gemini-cli and re-run ./gradlew run").
- **Exit codes:** Non-zero exit on missing configuration, unreadable inputs, Gemini CLI failure, validation critical failure, or I/O errors during output write.

---

## 11. Testing and Acceptance Criteria

### 11.1 Unit tests

- `DocxExtractor` against `source.docx` at project root, falling back to `src/test/resources/` fixtures
- `TemplateScanner`, `PptxTemplateReplacer`, and `ImageInserter` against `template.pptx`
- `ResponseValidator` with valid and invalid JSON fixtures under `src/test/resources/`
- `PromptBuilder`, `ImageAcquisitionService`, and `AppConfig` unit tests
- Full pipeline integration test gated on the `gemini` CLI being available

### 11.2 Acceptance criteria

1. `./gradlew build` passes all non-gated tests.
2. `./gradlew run` with a valid `.env` produces `final_presentation.pptx`.
3. Output has all text placeholders replaced and images inserted where configured.
4. Validator rejects JSON with missing required keys or invalid image queries.

---

## 12. Operator Documentation

[README.md](README.md) is the operator guide and must document:

- Prerequisites (JDK 21 via Gradle toolchain auto-provision; Gemini CLI installed and authenticated)
- Setup: copy `.env.example` to `.env`, install and authenticate the Gemini CLI
- Place `template.pptx` and `source.docx` at project root (or override paths via env)
- Run: `./gradlew run`
- Troubleshooting: missing Gemini CLI, CLI auth failures, input path errors, validation failures, split-run placeholders

Supporting files: `.gitignore` (build artifacts, `.env`, OS clutter), `.env.example` (no secrets).
