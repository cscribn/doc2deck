package com.appfire.presentation.llm;

import com.appfire.presentation.config.HtmlDeckOutlineConfig;
import com.appfire.presentation.model.ContentBlock;
import com.appfire.presentation.model.DocumentContent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class HtmlDeckPromptBuilder {

    private static final int MAX_SUMMARY_CHARS = 100_000;

    private final PromptLoader promptLoader;
    private final HtmlDeckOutlineConfig outlineConfig;
    private final String voiceStyleContent;

    public HtmlDeckPromptBuilder(
            PromptLoader promptLoader, HtmlDeckOutlineConfig outlineConfig, Path voiceStylePath) {
        this.promptLoader = promptLoader;
        this.outlineConfig = outlineConfig;
        this.voiceStyleContent = loadVoiceStyle(voiceStylePath);
    }

    public String build(DocumentContent document) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(promptLoader.load("prompt_core_rules.md"));
        prompt.append("\n\n");
        prompt.append("VOICE AND STYLE (rationale; not part of JSON deliverable):\n");
        prompt.append(voiceStyleContent.trim());
        prompt.append("\n\n");
        prompt.append(promptLoader.load("prompt_slide_copy_rules.md"));
        prompt.append("\n\n");
        prompt.append(promptLoader.load("prompt_slide_principles.md"));
        prompt.append("\n\n");
        prompt.append(outlineConfig.formatForPrompt());
        prompt.append("\n\n");
        prompt.append(buildDocument(document));
        prompt.append(buildOutputContract());
        return prompt.toString();
    }

    private String buildDocument(DocumentContent document) {
        StringBuilder blocks = new StringBuilder();
        for (ContentBlock block : document.blocks()) {
            blocks.append("Block ").append(block.index())
                    .append(" type=").append(block.type());
            if (block.headingLevel() > 0) {
                blocks.append(" level=").append(block.headingLevel());
            }
            if (!block.text().isBlank()) {
                blocks.append(" text=").append(block.text());
            }
            if (!block.items().isEmpty()) {
                blocks.append(" items=").append(block.items());
            }
            blocks.append("\n");
        }
        String summary = document.flatSummary();
        String summarySection = summary.length() > MAX_SUMMARY_CHARS
                ? "\nDOCX SUMMARY (truncated):\n" + summary.substring(0, MAX_SUMMARY_CHARS) + "\n[TRUNCATED]\n\n"
                : "\nDOCX SUMMARY:\n" + summary + "\n\n";
        return promptLoader.apply(
                promptLoader.load("prompt_docx_content.md"),
                Map.of("BLOCKS", blocks.toString(), "SUMMARY_SECTION", summarySection));
    }

    private String buildOutputContract() {
        return promptLoader.apply(
                promptLoader.load("prompt_html_deck_output_contract.md"),
                Map.of(
                        "SLIDES_MIN", String.valueOf(outlineConfig.slidesMin()),
                        "SLIDES_MAX", String.valueOf(outlineConfig.slidesMax()),
                        "HTML_DECK_OUTLINE", outlineConfig.formatForPrompt(),
                        "HTML_DECK_LAYOUTS", promptLoader.load("prompt_html_deck_layouts.md")));
    }

    private static String loadVoiceStyle(Path voiceStylePath) {
        try {
            return Files.readString(voiceStylePath, StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to read voice style file: " + voiceStylePath.toAbsolutePath()
                            + ". Check VOICE_STYLE_PATH in .env and re-run ./gradlew bootRun",
                    e);
        }
    }
}
