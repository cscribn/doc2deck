# Requirements

## Requirements Map

- This document is master.
- Default: [requirements/default.md](requirements/default.md)
- Java: [requirements/java_spring-boot.md](requirements/java_spring-boot.md)
- Google GenAI SDK: [requirements/google-genai-sdk.md](requirements/google-genai-sdk.md)
- Cursor Agent CLI: [requirements/cursor-cli.md](requirements/cursor-cli.md)

**Project overrides (this repo):** Where [requirements/java_spring-boot.md](requirements/java_spring-boot.md) conflicts with this file, this file wins. In particular: JDK 21 Gradle toolchain in `build.gradle.kts` (not “system JVM only”); logging via `presentation-generator.log` plus stdout step progress and ERROR-only console (`logback-spring.xml`), not generic stdout/stderr-only SLF4J.

## Sources

`.env.example` (env list) | `presentation-keys.example.properties` (PPTX key instructions) | `html-deck-outline.example.properties` (HTML outline) | `AppConfig.java` (defaults) | `prompts/prompt_*.md` + `prompts/voice-styles/*.md` (LLM rules/schema) | `src/main/resources/html-deck/` (HTML shell) | `build.gradle.kts` (deps) | `src/main/java/com/appfire/presentation/` (code)

## System

Java CLI generates a live-talk deck from `.docx` files in `sources/` via a pluggable AI backend (`AI_BACKEND`). Default backend: Cursor Agent CLI (`cursor_cli`). Alternate backend: Google GenAI Java SDK (`google_genai`). Default output: filled `template.pptx` → `final_presentation.pptx`. Alternate: `DECK_FORMAT=html` → single self-contained `final_presentation.html` (Appfire-themed slide track, timeline navigation, speaker notes). Output is **live presentation** reinforcement (Fowler slide principles), not infodeck/pre-read; narrative stays in source `.docx`.

PPTX: `${variableName}` (docx4j). Images: Pexels (POI embed). HTML: structured slide JSON → Java templates (no raw HTML from the model); split-layout images embedded as data URIs. JDK 21 toolchain (`build.gradle.kts`; `./gradlew -q javaToolchains`). Build/run/test: `./gradlew bootRun` (no args). Auth: Cursor CLI (`agent login` or `CURSOR_API_KEY`) or Google (`GOOGLE_API_KEY`). Logs: `presentation-generator.log` (overwritten each run); stdout progress and ERROR-level logs only.

## Config

`.env` (cwd) + system env. `AI_BACKEND`: `cursor_cli` (default) or `google_genai`. Shared: `LLM_MAX_RETRIES` (default `3`). Google: `GOOGLE_API_KEY`, `GOOGLE_GENAI_MODEL` (default `gemini-3.1-flash-lite`). Cursor: `CURSOR_CLI_PATH` (default `agent`), optional `CURSOR_MODEL`, `CURSOR_WORKSPACE` (default project cwd). `DECK_FORMAT`: `pptx` (default) or `html`. PPTX: local `presentation-keys.properties` (copy from example; gitignored). HTML: local `html-deck-outline.properties` (copy from example; gitignored). Env: `VOICE_STYLE_PATH` (default `prompts/voice-styles/neutral.md`), `OUTPUT_HTML_PATH` (default `final_presentation.html`), `HTML_DECK_OUTLINE_PATH`, optional `LAYOUT_SKIP_TEXT_FIT_SLIDE_INDICES` (PPTX only). No hardcoded secrets.

Fail-fast: selected backend missing required auth or CLI; unreadable `SOURCES_DIR` (no `.docx`); unreadable `VOICE_STYLE_PATH`. PPTX: unreadable `TEMPLATE_PPTX_PATH`, `PRESENTATION_KEYS_PATH`; template placeholder without matching keys entry. HTML: unreadable `HTML_DECK_OUTLINE_PATH`. Stdout progress; logs ERROR only.

## Pipeline (`PipelineOrchestrator.runPipeline`)

Shared: `DocxExtractor` → `DocumentContent`.

### PPTX (`DECK_FORMAT=pptx`)

1. `TemplateScanner` → `${key}` + image anchors; warn split-run placeholders; `TemplateDesignLinter` advisory (>3 `a:latin` fonts, >3 layouts on slides, >6 non-grey `srgbClr` colors)
2. `PresentationKeysConfig.validateAgainstTemplate` → fail-fast on missing config; advisory on extra keys
3. `PromptBuilder` → `prompt_*.md` + voice + `presentation-keys.properties` + DOCX + dynamic keys JSON schema; truncate summary ~100k
4. `PresentationContentGenerator` → JSON `PresentationContentResponse`
5. `ResponseValidator` → critical fail stops write
6. Copy template → temp; optional `PptxLayoutNormalizer.hardenStructure`
7. `PptxTemplateReplacer` + `ContentLengthEnforcer` + `TextSanitizer`
8. `OptionalPlaceholderCleaner`
9. Optional `PptxLayoutNormalizer.fitText` (respect `LAYOUT_SKIP_TEXT_FIT_SLIDE_INDICES`; min 12pt body)
10. `ImageAcquisitionService` → Pexels 2-5w queries; skip without `PEXELS_API_KEY`
11. `ImageInserter` → optimize/compress; optional `EmbeddedFontCleaner`; write `OUTPUT_PPTX_PATH`

### HTML (`DECK_FORMAT=html`)

1. `HtmlDeckPromptBuilder` → core/slide rules + voice + `html-deck-outline.properties` + `prompt_html_deck_{layouts,output_contract}.md` + DOCX
2. `PresentationContentGenerator.generateHtmlDeck` → JSON `HtmlDeckContentResponse` (slides array; layouts `hero|standard|split|journey|doc|cards|list|closing`)
3. `HtmlDeckResponseValidator` → slide count bounds; first/last layout rules; per-field word limits; icon allowlist; split `imageQuery` 2-5 words; critical fail stops write
4. `ImageAcquisitionService` for split slides; skip without `PEXELS_API_KEY` (placeholder panel)
5. `HtmlDeckWriter` → inline `html-deck/` CSS/JS/icons; write `OUTPUT_HTML_PATH`

Template (PPTX): `${key}` in single run; disable spell-check-as-you-type.

## Keys / outline

**PPTX:** Template-driven keys in `presentation-keys.properties`. Per key: instruction, optional `maxWords`, `optional=true`, `type=image`. Defaults: `type=text`, `optional=false`, `maxWords=15` (text) or `5` (image).

**HTML:** Outline in `html-deck-outline.properties`: `milestones`, `slides.min`/`slides.max`, per-field `*.maxWords`, `layout.*` descriptions. Model returns slide objects (title, kicker, headline, subline, bullets with icon+text, notes, optional `ai`, split `imageQuery`). Icons from prompt allowlist only; Java HTML-escapes all copy.

Prompt rules (both formats): source mapping, JSON-only output, citations in `sourceRefs`/`warnings`, sentence case slide copy, no URLs/citations/slide numbers/logos on slides. Canonical PPTX prompts: `prompt_{core_rules,slide_copy_rules,slide_principles,image_keys,output_contract,docx_content}.md`. HTML adds `prompt_html_deck_layouts.md`, `prompt_html_deck_output_contract.md`. Voice from `VOICE_STYLE_PATH`.

## Validation

**PPTX critical:** JSON/`keys` fail; required template key blank; text over per-key word limit; image query outside 2-5 words.

**HTML critical:** JSON/deckTitle/slides fail; slide count outside outline bounds; layout/field violations; invalid icon; split image query word count; first slide not `hero`; last not `closing`.

**Advisory (both):** empty `sourceRefs`; PPTX template/key/design warnings; HTML weak backing in `warnings`.

## Resilience & exit

**Google GenAI (`GoogleGenAiClient`):** in-process `generateContent`; retries per [requirements/google-genai-sdk.md](requirements/google-genai-sdk.md) up to `LLM_MAX_RETRIES`; backoff 1s–30s (429 exponential; 5xx/408/empty/malformed JSON jittered); 400/401/403 fail without retry.

**Cursor CLI (`CursorCliClient`):** one `agent -p` per prompt; prompt body in `.cache/llm/prompt-*.md`; short argv instruction; `--mode=ask`, `--trust`, `--workspace`; plain-text stdout; retries on transient failures up to `LLM_MAX_RETRIES`; backoff 1s–30s.

Non-zero exit: config, inputs, LLM, validation critical, I/O.

## Tests

Unit: extractor, scanner, design linter, replacer, inserter, validator, word count, enforcer, normalizer, prompt builder, keys/outline loaders, HTML validator/writer/renderer, image service, config, LLM clients/parsers. Integration: gated on configured AI backend availability. Accept: `./gradlew build`; `./gradlew bootRun` PPTX produces filled deck; `DECK_FORMAT=html` produces single HTML file with navigation shell.
