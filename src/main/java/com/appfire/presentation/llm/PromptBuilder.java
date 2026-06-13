package com.appfire.presentation.llm;

import com.appfire.presentation.config.AppConfig;
import com.appfire.presentation.config.PresentationKeysConfig;
import com.appfire.presentation.model.ContentBlock;
import com.appfire.presentation.model.DocumentContent;
import com.appfire.presentation.model.TemplateScanResult;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class PromptBuilder {

    private static final int MAX_SUMMARY_CHARS = 100_000;

    private final PromptLoader promptLoader;
    private final PresentationKeysConfig keysConfig;
    private final String voiceStyleContent;

    public PromptBuilder(AppConfig appConfig, PresentationKeysConfig keysConfig) {
        this(new PromptLoader(), keysConfig, appConfig.voiceStylePath());
    }

    public PromptBuilder(PromptLoader promptLoader, PresentationKeysConfig keysConfig, Path voiceStylePath) {
        this.promptLoader = promptLoader;
        this.keysConfig = keysConfig;
        this.voiceStyleContent = loadVoiceStyle(voiceStylePath);
    }

    public String build(DocumentContent document, TemplateScanResult scan) {
        Set<String> templateKeys = scan.foundKeys();
        StringBuilder prompt = new StringBuilder();
        prompt.append(promptLoader.load("prompt_core_rules.md"));
        prompt.append("\n\n");
        prompt.append("VOICE AND STYLE (rationale; not part of JSON deliverable):\n");
        prompt.append(voiceStyleContent.trim());
        prompt.append("\n\n");
        prompt.append(promptLoader.load("prompt_slide_copy_rules.md"));
        prompt.append("\n\n");
        prompt.append(keysConfig.formatForPrompt(templateKeys));
        prompt.append("\n\n");
        prompt.append(buildImageKeysSection(templateKeys));
        prompt.append("\n\n");
        prompt.append(buildDocument(document));
        prompt.append(buildOutputContract(scan, templateKeys));
        return prompt.toString();
    }

    private String buildImageKeysSection(Set<String> templateKeys) {
        String definitions = keysConfig.formatImageKeysForPrompt(templateKeys);
        if (definitions.isBlank()) {
            definitions = "(no image keys in template)";
        }
        return promptLoader.apply(
                promptLoader.load("prompt_image_keys.md"),
                Map.of("IMAGE_KEY_DEFINITIONS", definitions));
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

    private String buildOutputContract(TemplateScanResult scan, Set<String> templateKeys) {
        String templateKeysList = scan.foundKeys().stream()
                .sorted()
                .collect(Collectors.joining(", "));
        return promptLoader.apply(
                promptLoader.load("prompt_output_contract.md"),
                Map.of(
                        "TEMPLATE_KEYS", templateKeysList.isBlank() ? "(none detected)" : templateKeysList,
                        "KEYS_JSON_SCHEMA", keysConfig.buildKeysJsonSchema(templateKeys),
                        "IMAGE_KEY_STEP", keysConfig.buildImageKeyInstructionStep(templateKeys)));
    }

    private static String loadVoiceStyle(Path voiceStylePath) {
        try {
            return Files.readString(voiceStylePath, StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to read voice style file: " + voiceStylePath.toAbsolutePath()
                            + ". Check VOICE_STYLE_PATH in .env and re-run ./gradlew run",
                    e);
        }
    }
}
