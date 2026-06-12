# Requirements

Authoritative functional specification. [README.md](README.md) is the operator guide (setup, env table, troubleshooting).

## Canonical sources

| Concern | Source |
|---------|--------|
| Operator setup, env table, troubleshooting | [README.md](README.md) |
| Env defaults (code truth) | [AppConfig.java](src/main/java/com/appfire/presentation/config/AppConfig.java) |
| Gemini rules, presentation keys, JSON schema | [prompts/](prompts/) |
| Dependency versions | [build.gradle.kts](build.gradle.kts) |
| Package layout | `src/main/java/com/appfire/presentation/` |
| Build tooling | `build.gradle.kts`, `settings.gradle.kts`, Gradle Wrapper; main class `com.appfire.presentation.Application` |

## Overview

Java CLI fills a fixed PowerPoint template from a DOCX source via the Gemini CLI. Text placeholders use `${variableName}` (docx4j); image slots use Pexels stock photos (Apache POI).

**Artifacts:** `template.pptx` (placeholders), `source.docx` (narrative), `final_presentation.pptx` (output).

**Stack:** JDK 21 (Gradle toolchain + Foojay resolver in `settings.gradle.kts`; confirm with `./gradlew -q javaToolchains`), `./gradlew` for build/run/test, docx4j + Apache POI, external `gemini` CLI (not Java SDK; CLI handles auth). Bare `java`/`javac`/`gradle` only for troubleshooting.

## Configuration

Load `.env` from working directory (key=value, `#` comments) plus system env; never hardcode secrets. Full variable list: [README.md](README.md). Defaults: [AppConfig.java](src/main/java/com/appfire/presentation/config/AppConfig.java).

**Startup:** Fail fast if Gemini CLI missing, not executable, or fails version check; fail if `TEMPLATE_PPTX_PATH` or `SOURCE_DOCX_PATH` missing/unreadable. Print step progress to stdout; console logs ERROR only (suppress INFO/WARN).

**Run:** `./gradlew run` (no trailing targets or CLI args).

## Pipeline

Strict serial order (`Application.run()`):

1. **Extract** (`DocxExtractor`): Parse `source.docx` via `XWPFDocument` into ordered `ContentBlock` records (headings 1-6, paragraphs, lists, pipe-delimited table rows). Output `DocumentContent` with blocks + flat text summary.
2. **Scan** (`TemplateScanner`): Open `template.pptx` via POI `XMLSlideShow`; detect `${variableName}` in text shapes and table cells; record image-key anchors (slide, shape ID, bounds). Warn on split-run placeholders.
3. **Prompt** (`PromptBuilder`): Load cached `prompts/prompt_*.md` via `PromptLoader`; inject DOCX context and `{{TEMPLATE_KEYS}}` by substitution; concatenate sections. Truncate flat summary at ~100k chars with truncation warning in prompt.
4. **Generate** (`GeminiClient`): Send prompt on stdin to `gemini` CLI; parse JSON response.
5. **Validate** (`ResponseValidator`): Deterministic checks before any template write (see Validation).
6. **Working copy:** Copy `template.pptx` to temp file. **Never modify `template.pptx`.**
7. **Harden layout** (`PptxLayoutNormalizer.hardenStructure`, when `LAYOUT_NORMALIZE_ENABLED`): Disable autofit (`noAutofit`), enable word wrap on all text shapes.
8. **Replace text** (`PptxTemplateReplacer`): docx4j `PresentationMLPackage` + `variableReplace()` on every slide. Map validated text keys only; exclude image keys and unpopulated optional keys. Enforce word limits via `ContentLengthEnforcer` (truncate + warn as safety net). Strip leading bullet chars (`TextSanitizer`); template provides bullets.
9. **Clean optional** (`OptionalPlaceholderCleaner`): When `nonDevCosts` absent/unpopulated, remove its bullet paragraph via POI. Treat key-name-only or empty-after-sanitize values as unpopulated.
10. **Fit text** (`PptxLayoutNormalizer.fitText`, when enabled): Bake explicit font sizes on content slides (skip static slides 1, 6, 9); min body 12pt for Google Drive/Slides compatibility.
11. **Acquire images** (`ImageAcquisitionService`): Pexels search via `curl` using 2-5 word queries; cache under `IMAGE_CACHE_DIR`. Skip when `PEXELS_API_KEY` unset; warn and continue on fetch failure.
12. **Insert and finalize** (`ImageInserter`): POI insert at pre-scanned anchors (or re-scan `${key}`); clear placeholder text. When `IMAGE_OPTIMIZATION_ENABLED`: compress all embedded JPEG (`ImageIO`, `IMAGE_JPEG_QUALITY`, same dimensions) and PNG (`pngquant-png`, preserve alpha); skip non-JPEG/PNG; replace only when smaller; warn and keep original on failure. Write `OUTPUT_PPTX_PATH`. When `FONT_CLEANUP_ENABLED`: `EmbeddedFontCleaner` removes unreferenced `.fntdata` fonts (via `ReferencedFontCollector` on slide/master/layout/notes/theme XML); keep theme-linked metadata without binaries; warn and leave file unchanged on failure.

**Template authoring:** Each `${variableName}` in a single text run; disable spell-check-as-you-type to avoid split runs.

## Prompt and keys

Prompt templates: `prompts/prompt_<two_word_description>.md`. Rules, voice, keys, and JSON schema are canonical in:

- [prompt_core_rules.md](prompts/prompt_core_rules.md), [prompt_voice_style.md](prompts/prompt_voice_style.md)
- [prompt_presentation_keys.md](prompts/prompt_presentation_keys.md), [prompt_image_keys.md](prompts/prompt_image_keys.md)
- [prompt_output_contract.md](prompts/prompt_output_contract.md), [prompt_docx_content.md](prompts/prompt_docx_content.md)

**Keys:** 17 required text + 1 optional (`nonDevCosts`) + 3 image keys. Per-key word limits enforced at validation and truncated at replace time. Image queries: 2-5 word Pexels phrases. Prompt must number instruction steps; JSON-only response; stateless; DOCX-grounded facts only.

## Validation

`ResponseValidator` runs before template fill.

Critical (exit non-zero; do not write `OUTPUT_PPTX_PATH`; log each failure with concrete example):

- JSON parse failure or missing `keys` object
- Required text or image key missing or blank
- Text key exceeds per-key word limit (`KeyContentLimits`)
- Image key query not 2-5 words

Advisory (log; continue when no critical failures):

- Empty `sourceRefs` per key
- Template placeholders without Gemini values
- Gemini keys not in template scan
- `nonDevCosts` absent
- Split-run placeholder warnings from template scan

## Resilience

**GeminiClient:** `gemini -p` headless, prompt on stdin, `--output-format json`, `--approval-mode plan`, `--skip-trust`. Exponential backoff 1s-30s, up to `GEMINI_MAX_RETRIES`; retry rate limits, timeouts, transient CLI failures.

**Exit codes:** Non-zero on missing config, unreadable inputs, Gemini failure, validation critical failure, or output I/O errors.

## Testing

**Unit tests:** `DocxExtractor` (root `source.docx` or `src/test/resources/` fixtures); `TemplateScanner`, `PptxTemplateReplacer`, `ImageInserter` against `template.pptx`; `ResponseValidator` with JSON fixtures (including word-limit rejection); `KeyContentLimits`, `ContentLengthEnforcer`, `PptxLayoutNormalizer`; `PromptBuilder`, `ImageAcquisitionService`, `AppConfig`.

**Integration:** Full pipeline gated on `gemini` CLI availability.

**Acceptance:**

1. `./gradlew build` passes non-gated tests
2. `./gradlew run` with valid `.env` produces `final_presentation.pptx`
3. All text placeholders replaced; images inserted where configured
4. JPEG/PNG compressed when optimization enabled and smaller size achievable
5. Validator rejects missing keys, invalid image queries, over-limit text
