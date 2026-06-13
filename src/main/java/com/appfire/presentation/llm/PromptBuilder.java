package com.appfire.presentation.llm;

import com.appfire.presentation.config.PresentationKeysConfig;
import com.appfire.presentation.model.ContentBlock;
import com.appfire.presentation.model.DocumentContent;
import com.appfire.presentation.model.TemplateScanResult;
import java.util.Map;
import java.util.stream.Collectors;

public final class PromptBuilder {

    private static final int MAX_SUMMARY_CHARS = 100_000;

    private final PromptLoader promptLoader;
    private final PresentationKeysConfig keysConfig;

    public PromptBuilder(PresentationKeysConfig keysConfig) {
        this(new PromptLoader(), keysConfig);
    }

    public PromptBuilder(PromptLoader promptLoader, PresentationKeysConfig keysConfig) {
        this.promptLoader = promptLoader;
        this.keysConfig = keysConfig;
    }

    public String build(DocumentContent document, TemplateScanResult scan) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(promptLoader.load("prompt_core_rules.md"));
        prompt.append("\n\n");
        prompt.append(promptLoader.load("prompt_voice_style.md"));
        prompt.append("\n\n");
        prompt.append(keysConfig.formatForPrompt());
        prompt.append("\n\n");
        prompt.append(promptLoader.load("prompt_image_keys.md"));
        prompt.append("\n\n");
        prompt.append(buildDocument(document));
        prompt.append(buildOutputContract(scan));
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
        String summarySection = buildSummarySection(document.flatSummary());
        return promptLoader.apply(
                promptLoader.load("prompt_docx_content.md"),
                Map.of(
                        "BLOCKS", blocks.toString(),
                        "SUMMARY_SECTION", summarySection));
    }

    private String buildSummarySection(String summary) {
        if (summary.length() > MAX_SUMMARY_CHARS) {
            return "\nDOCX SUMMARY (truncated):\n"
                    + summary.substring(0, MAX_SUMMARY_CHARS)
                    + "\n[TRUNCATED]\n\n";
        }
        return "\nDOCX SUMMARY:\n" + summary + "\n\n";
    }

    private String buildOutputContract(TemplateScanResult scan) {
        String templateKeys = scan.foundKeys().stream()
                .sorted()
                .collect(Collectors.joining(", "));
        return promptLoader.apply(
                promptLoader.load("prompt_output_contract.md"),
                Map.of("TEMPLATE_KEYS", templateKeys.isBlank() ? "(none detected)" : templateKeys));
    }
}
