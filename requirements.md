# Requirements

Authoritative functional spec. Setup/troubleshooting: README.md only.

## Sources

`.env.example` (env list) | `presentation-keys.example.properties` (key instructions) | `AppConfig.java` (defaults) | `prompts/prompt_*.md` (Gemini rules/schema) | `build.gradle.kts` (deps) | `src/main/java/com/appfire/presentation/` (code)

## System

Java CLI fills `template.pptx` from `.docx` files in `sources/` via Gemini CLI → `final_presentation.pptx`.

Text: `${variableName}` (docx4j). Images: Pexels stock (POI). JDK 21 toolchain (`settings.gradle.kts`; `./gradlew -q javaToolchains`). Build/run/test: `./gradlew run` (no args). External `gemini` CLI auth (not Java SDK).

## Config

`.env` (cwd) + system env. Local `presentation-keys.properties` (copy from example; gitignored). No hardcoded secrets. Fail-fast: gemini missing/unexecutable/version fail; unreadable `TEMPLATE_PPTX_PATH`/`SOURCES_DIR` (no `.docx` files)/`PRESENTATION_KEYS_PATH`. Stdout progress; logs ERROR only.

## Pipeline (`Application.run`)

1. `DocxExtractor` → `DocumentContent` (blocks h1-6/para/lists/pipe-tables + flat summary)
2. `TemplateScanner` → `${key}` + image anchors; warn split-run placeholders
3. `PromptBuilder` → cached `prompts/prompt_*.md` + `presentation-keys.properties` + DOCX + `{{TEMPLATE_KEYS}}`; truncate summary ~100k
4. `GeminiClient` → stdin prompt, JSON out
5. `ResponseValidator` → critical fail stops write (below)
6. Copy template → temp (never mutate template)
7. `PptxLayoutNormalizer.hardenStructure` if `LAYOUT_NORMALIZE_ENABLED`
8. `PptxTemplateReplacer` → docx4j replace; text keys only; skip image/unpopulated optional; `ContentLengthEnforcer` + `TextSanitizer` (strip leading bullets)
9. `OptionalPlaceholderCleaner` → remove `nonDevCosts` bullet when absent/unpopulated
10. `PptxLayoutNormalizer.fitText` if enabled (skip slides 1,6,9; min 12pt body)
11. `ImageAcquisitionService` → Pexels/curl 2-5w queries, `IMAGE_CACHE_DIR`; skip without `PEXELS_API_KEY`
12. `ImageInserter` → POI insert; optional JPEG/PNG compress; `EmbeddedFontCleaner` if `FONT_CLEANUP_ENABLED`; write output

Template: `${key}` in single run; disable spell-check-as-you-type.

## Keys

16 req text + 1 opt (`nonDevCosts`) + 3 req image. Limits: `KeyContentLimits`, `presentation-keys.properties`. Image queries 2-5 words. Prompt: numbered steps, JSON-only, stateless, DOCX facts only. Canonical prompts: `prompts/prompt_{core_rules,voice_style,image_keys,output_contract,docx_content}.md`.

## Validation

**Critical** (non-zero exit, no output): JSON/`keys` fail; required key blank; text over word limit; image query not 2-5 words.

**Advisory**: empty `sourceRefs`; template gap; unknown/extra Gemini key; `nonDevCosts` absent; `shortProjectDescription` >200 chars or contains "Flow API Monorepo"; split-run warnings.

## Resilience & exit

`GeminiClient`: `-p`, stdin, `--output-format json`, `--approval-mode plan`, `--skip-trust`; backoff 1s–30s × `GEMINI_MAX_RETRIES`. Non-zero: config, inputs, Gemini, validation critical, I/O.

## Tests

Unit: extractor, scanner, replacer, inserter, validator, limits, enforcer, normalizer, prompt builder, keys config loader, image service, config. Integration: gated on gemini CLI. Accept: `./gradlew build`; `./gradlew run` produces filled PPTX with images when configured; validator rejects bad keys/queries/limits.
