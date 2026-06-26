# Requirements

## Requirements Map

- This document is master.
- Default: [requirements/default.md](requirements/default.md)
- Java: [requirements/java_spring-boot.md](requirements/java_spring-boot.md)

## Sources

`.env.example` (env list) | `presentation-keys.example.properties` (key instructions) | `AppConfig.java` (defaults) | `prompts/prompt_*.md` + `prompts/voice-styles/*.md` (Gemini rules/schema) | `build.gradle.kts` (deps) | `src/main/java/com/appfire/presentation/` (code)

## System

Java CLI fills any `template.pptx` from `.docx` files in `sources/` via Gemini CLI → `final_presentation.pptx`.

Text: `${variableName}` (docx4j). Images: Pexels stock (POI). JDK 21 toolchain (`settings.gradle.kts`; `./gradlew -q javaToolchains`). Build/run/test: `./gradlew run` (no args). External `gemini` CLI auth (not Java SDK).

## Config

`.env` (cwd) + system env. Local `presentation-keys.properties` (copy from example; gitignored). Env: `VOICE_STYLE_PATH` (default `prompts/voice-styles/neutral.md`), optional `LAYOUT_SKIP_TEXT_FIT_SLIDE_INDICES` (0-based comma list; empty = fit all slides). No hardcoded secrets. Fail-fast: gemini missing/unexecutable/version fail; unreadable `TEMPLATE_PPTX_PATH`/`SOURCES_DIR` (no `.docx` files)/`PRESENTATION_KEYS_PATH`/`VOICE_STYLE_PATH`; template placeholder without matching `presentation-keys.properties` entry. Stdout progress; logs ERROR only.

## Pipeline (`Application.run`)

1. `DocxExtractor` → `DocumentContent` (blocks h1-6/para/lists/pipe-tables + flat summary)
2. `TemplateScanner` → `${key}` + image anchors from configured image keys; warn split-run placeholders
3. `PresentationKeysConfig.validateAgainstTemplate` → fail-fast on missing config; advisory on extra config keys
4. `PromptBuilder` → cached `prompts/prompt_*.md` + voice style file + `presentation-keys.properties` + DOCX + dynamic JSON schema from template keys; truncate summary ~100k
5. `GeminiClient` → stdin prompt, JSON out
6. `ResponseValidator` → critical fail stops write (below)
7. Copy template → temp (never mutate template)
8. `PptxLayoutNormalizer.hardenStructure` if `LAYOUT_NORMALIZE_ENABLED`
9. `PptxTemplateReplacer` → docx4j replace; configured text keys only; skip unpopulated optional; `ContentLengthEnforcer` + `TextSanitizer` (strip leading bullets)
10. `OptionalPlaceholderCleaner` → remove optional key bullets when absent/unpopulated
11. `PptxLayoutNormalizer.fitText` if enabled (skip slides in `LAYOUT_SKIP_TEXT_FIT_SLIDE_INDICES`; min 12pt body)
12. `ImageAcquisitionService` → Pexels/curl 2-5w queries, `IMAGE_CACHE_DIR`; skip without `PEXELS_API_KEY`
13. `ImageInserter` → POI insert; optional JPEG/PNG compress; `EmbeddedFontCleaner` if `FONT_CLEANUP_ENABLED`; write output

Template: `${key}` in single run; disable spell-check-as-you-type.

## Keys

Template-driven: every `${key}` in the PPTX must have a `presentation-keys.properties` entry. Per key: instruction, optional `maxWords`, `optional=true`, `type=image`. Defaults: `type=text`, `optional=false`, `maxWords=15` (text) or `5` (image). Image queries 2-5 words (max from `maxWords`). Prompt: External AI Prompting rules (source mapping, numbered steps, JSON-only output, citations in sourceRefs/warnings, stateless). Canonical prompts: `prompts/prompt_{core_rules,slide_copy_rules,image_keys,output_contract,docx_content}.md`; voice from `VOICE_STYLE_PATH`.

## Validation

**Critical** (non-zero exit, no output): JSON/`keys` fail; required template key blank; text over per-key word limit; image query outside 2-5 words (or key `maxWords`).

**Advisory**: empty `sourceRefs`; template gap; unknown/extra Gemini key; optional key absent; `shortProjectDescription` >200 chars; configured key not in template; split-run warnings.

## Resilience & exit

`GeminiClient`: `-p`, stdin, `--output-format json`, `--approval-mode plan`, `--skip-trust`; backoff 1s–30s × `GEMINI_MAX_RETRIES`. Non-zero: config, inputs, Gemini, validation critical, I/O.

## Tests

Unit: extractor, scanner, replacer, inserter, validator, word count, enforcer, normalizer, prompt builder, keys config loader, image service, config. Integration: gated on gemini CLI. Accept: `./gradlew build`; `./gradlew run` produces filled PPTX with images when configured; validator rejects bad keys/queries/limits.
