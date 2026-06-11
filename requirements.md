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
9. **Image Optimization:** Compress all embedded JPEG and PNG pictures in the deck before final write (same pixel dimensions; PNG alpha preserved).
10. **Output:** Write `final_presentation.pptx`.

---

## 3. Gemini System Prompt & Constraints

The application must inject system instructions into the prompt payload. These boundaries govern content generation:

- Use only facts from the provided DOCX source text. No fabrication.
- Fill every required presentation key with grounded content.
- Apply TEDx-inspired voice: professional, engaging, lightly humorous when appropriate, slightly sensational within source-backed truth.
- All text keys must use slide copy: short fragments or phrase clusters, not complete sentences.
- Return JSON only with no markdown fences and no commentary.
- Stateless: use only information in the current prompt.
- `shortProjectDescription` must be a compact fragment only and must not contain "Flow API Monorepo" (already in the template title).
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
| `IMAGE_JPEG_QUALITY` | no | `0.8` | JPEG re-encode quality (0.0-1.0) for embedded picture optimization |
| `IMAGE_OPTIMIZATION_ENABLED` | no | `true` | When `false`, skip embedded picture compression before final write |
| `FONT_CLEANUP_ENABLED` | no | `true` | When `false`, skip removal of unused embedded `.fntdata` font binaries after final write |

**Startup behavior:**

- Fail fast with an actionable message when the Gemini CLI is missing, not executable, or fails a version check.
- Fail fast when `TEMPLATE_PPTX_PATH` or `SOURCE_DOCX_PATH` does not exist or is unreadable.
- Print user-friendly step progress to standard output during pipeline execution.
- Emit only ERROR-level log messages to the console; suppress INFO and WARN from console output.

**Run command:** `./gradlew run` (no trailing targets or CLI arguments).

---

## 5. Project Layout and Dependencies

### 5.1 Package structure

Root package: `com.appfire.presentation`

```
prompts/
  prompt_core_rules.md
  prompt_voice_style.md
  prompt_presentation_keys.md
  prompt_image_keys.md
  prompt_docx_content.md
  prompt_output_contract.md
src/main/java/com/appfire/presentation/
  Application.java
  ConsoleProgress.java
  config/AppConfig.java
  extraction/DocxExtractor.java
  llm/PromptBuilder.java
  llm/PromptLoader.java
  llm/GeminiClient.java
  llm/ResponseValidator.java
  template/TemplateScanner.java
  template/PptxTemplateReplacer.java
  template/ImageInserter.java
  template/EmbeddedFontCleaner.java
  template/ReferencedFontCollector.java
  images/ImageAcquisitionService.java
  images/PresentationImageOptimizer.java
  images/JpegImageCompressor.java
  images/PngImageCompressor.java
  model/
src/test/java/com/appfire/presentation/
  (mirrors main package)
```

### 5.2 Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| Apache POI `poi-ooxml` | 5.4.1 | DOCX parsing and PPTX image insertion |
| `log4j-to-slf4j` | 2.24.3 | Routes Apache POI Log4j API calls to SLF4J |
| docx4j `docx4j-JAXB-ReferenceImpl` | 11.5.7 | PPTX `${variableName}` text replacement |
| Jackson `jackson-databind` | 2.19.0 | JSON serialization for Gemini CLI responses |
| JUnit 5 + Mockito | 5.12.2 / 5.17.0 | Unit and integration tests |
| `junit-platform-launcher` | (BOM) | Required test runtime for Gradle JUnit Platform |
| SLF4J + `slf4j-simple` | 2.0.17 | Structured logging |
| `pngquant-png` (`com.xqlee.image`) | 1.0.0 | PNG quantization for embedded picture optimization |

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
3. Apply TEDx-inspired voice via `prompt_voice_style.md`: hooks, narrative momentum, light humor, and vivid but accurate language.
4. Require slide-fragment copy for all text keys (no complete sentences or essay-style prose).
5. Require a JSON-only response with no markdown fences and no commentary (`prompt_output_contract.md`).
6. Instruct the model to be stateless: use only information in the current prompt.

### 7.2 Token limit strategy

When DOCX content exceeds approximately 100,000 characters, truncate the flat summary to the first 100,000 characters and append a truncation warning in the prompt.

### 7.3 Presentation keys

**Text keys (17 required + 1 optional):**

| Key | Guidance |
|-----|----------|
| `shortProjectDescription` | Hook fragment; 4-10 words; never include "Flow API Monorepo" |
| `problem1`, `problem2` | Problem fragments; 8-15 words each |
| `persona` | Persona fragment; 6-12 words |
| `currentSolution1`, `currentSolution2` | Workaround fragments; 8-15 words each |
| `scale`, `whyNow`, `ifUnsolved` | 1-2 fragments; 8-18 words total each |
| `architectureApproach`, `approachReasons` | 2-3 semicolon-joined fragments; 15-25 words total each |
| `personaBetterment`, `appfireBetterment` | 1-2 fragments; 8-18 words total each |
| `estimatedImpact` | 2-3 semicolon-joined fragments; 15-25 words total |
| `currentState` | 1-2 fragments; 8-18 words total |
| `sprintsToDeliver` | Single fragment; 4-10 words (2-week sprints) |
| `nonDevCosts` | Optional; 1-2 fragments; 8-18 words total |

**Image keys (3 required):**

| Key | Description |
|-----|-------------|
| `problemSolvingImg` | Pexels query: dramatic engineering breakthrough or impossible problem cracked |
| `architectureApproachImg` | Pexels query: bold architecture planning or complex system taking shape |
| `valueImpactImg` | Pexels query: engineers delivering visible, high-impact results |

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
- Build replacement map from validated text keys.
- Strip leading bullet characters from all text values (`TextSanitizer`); template bullets provide formatting.
- Exclude image keys and unpopulated optional keys from text replacement.
- Apply `variableReplace()` on every slide.
- Save to a temporary file.

### 9.2 Optional placeholder cleanup (`OptionalPlaceholderCleaner`)

- When `nonDevCosts` is absent or not populated, remove the bullet paragraph containing `${nonDevCosts}` from the deck via Apache POI.
- Treat placeholder-like values (key name only, empty after sanitization) as unpopulated.

### 9.3 Image acquisition (`ImageAcquisitionService`)

- Search Pexels using image-key query values via `curl`, cache results under `IMAGE_CACHE_DIR`.
- When `PEXELS_API_KEY` is unset or fetch fails, log a warning and continue without the image.

### 9.4 Image insertion (`ImageInserter`)

- Open the text-replaced PPTX with Apache POI.
- For each image key, locate the pre-scanned anchor (or re-scan for `${key}` token).
- Insert picture at anchor bounds; clear placeholder text.
- Run embedded picture optimization (section 9.5) when enabled.
- Write final output to `OUTPUT_PPTX_PATH`.
- Run embedded font cleanup (section 9.6) on the saved file when enabled.

### 9.5 Image optimization (`PresentationImageOptimizer`)

- After image insertion, iterate all `XMLSlideShow.getPictureData()` entries (template images and acquired photos).
- JPEG: re-encode via `ImageIO` at `IMAGE_JPEG_QUALITY` (default 0.8) without changing pixel dimensions.
- PNG: compress via `pngquant-png` default settings; preserve alpha channel and pixel dimensions.
- Skip non-JPEG/PNG picture types (EMF, WMF, GIF, etc.).
- Replace picture bytes only when compressed data is smaller than the original.
- On per-picture failure, log a warning and keep the original bytes (non-blocking).

### 9.6 Embedded font cleanup (`EmbeddedFontCleaner`)

- After `ImageInserter` writes the final PPTX, reopen the file with Apache POI `OPCPackage` in read-write mode.
- Collect typefaces referenced in slide, master, layout, notes, and theme XML via `ReferencedFontCollector`.
- Parse `presentation.xml` and `presentation.xml.rels` for embedded fonts with `.fntdata` binaries.
- Remove embedded font entries whose typeface is not referenced anywhere in the deck.
- Delete the `.fntdata` part, relationship, and `[Content_Types].xml` override for each removed font.
- Keep embedded font metadata entries that have no binary (for example, theme-linked Roboto Light) when still listed in `presentation.xml`.
- On failure, log a warning and leave the output file unchanged (non-blocking).

### 9.7 Template authoring

- Each `${variableName}` must be in a single PowerPoint text run.
- Disable "Check spelling as you type" when authoring templates to avoid split runs.

---

## 10. Error Handling and Resilience

- **GeminiClient:** Invokes `gemini -p` in headless mode with prompt on stdin, `--output-format json`, `--approval-mode plan`, and `--skip-trust`. Exponential backoff starting at 1 second, capped at 30 seconds, up to `GEMINI_MAX_RETRIES` attempts. Retry on rate limits, timeouts, and transient CLI failures.
- **Console output:** Print one user-friendly message per pipeline step to standard output. Errors are logged to the console at ERROR level with actionable resolution steps (for example, "Install gemini CLI with npm install -g @google/gemini-cli and re-run ./gradlew run"). INFO and WARN logs are suppressed from console output.
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
4. Embedded JPEG and PNG pictures are compressed when optimization is enabled and a smaller byte size is achievable.
5. Validator rejects JSON with missing required keys or invalid image queries.

---

## 12. Operator Documentation

[README.md](README.md) is the operator guide and must document:

- Prerequisites (JDK 21 via Gradle toolchain auto-provision; Gemini CLI installed and authenticated)
- Setup: copy `.env.example` to `.env`, install and authenticate the Gemini CLI
- Place `template.pptx` and `source.docx` at project root (or override paths via env)
- Run: `./gradlew run`
- Troubleshooting: missing Gemini CLI, CLI auth failures, input path errors, validation failures, split-run placeholders

Supporting files: `.gitignore` (build artifacts, `.env`, OS clutter), `.env.example` (no secrets).
