package com.appfire.presentation.llm;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.appfire.presentation.config.PresentationKeysConfig;
import com.appfire.presentation.config.PresentationKeysConfigLoader;
import com.appfire.presentation.model.BlockType;
import com.appfire.presentation.model.ContentBlock;
import com.appfire.presentation.model.DocumentContent;
import com.appfire.presentation.model.TemplateScanResult;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PromptBuilderTest {

    @Test
    void buildsPromptFromMarkdownTemplates() {
        DocumentContent document = new DocumentContent(
                List.of(new ContentBlock(0, BlockType.HEADING, 1, "Overview")),
                "Overview");
        TemplateScanResult scan = new TemplateScanResult(Set.of("problem1", "problem2"), List.of(), List.of());
        PresentationKeysConfig keysConfig = PresentationKeysConfigLoader.load(
                Path.of("presentation-keys.example.properties"));
        PromptBuilder builder = new PromptBuilder(
                new PromptLoader(Path.of("prompts")),
                keysConfig,
                Path.of("prompts/voice-styles/neutral.md"),
                "");
        String prompt = builder.build(document, scan);

        assertTrue(prompt.contains("EXTERNAL AI PROMPTING (NON-NEGOTIABLE):"));
        assertTrue(prompt.contains("INSTRUCTIONS (execute in order):"));
        assertTrue(prompt.contains("OUTPUT FORMAT AND CONSTRAINTS:"));
        assertTrue(prompt.contains("SOURCE MATERIAL (cite block indices"));
        assertTrue(prompt.contains("VOICE AND STYLE (rationale; not part of JSON deliverable):"));
        assertTrue(prompt.contains("PRESENTATION KEYS"));
        assertTrue(prompt.contains("IMAGE KEYS"));
        assertTrue(prompt.contains("DOCX CONTENT BLOCKS:"));
        assertTrue(prompt.contains("Template keys found: problem1, problem2"));
        assertTrue(prompt.contains("\"problem1\": \"string\""));
        assertFalse(prompt.contains("LAYOUT CATALOG"));
        assertFalse(prompt.contains("CONTENT VARIATION"));
    }
}
