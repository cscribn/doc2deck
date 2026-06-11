package com.appfire.presentation.llm;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        PromptBuilder builder = new PromptBuilder(new PromptLoader(Path.of("prompts")));
        String prompt = builder.build(document, scan);

        assertTrue(prompt.contains("NON-NEGOTIABLE RULES:"));
        assertTrue(prompt.contains("PRESENTATION KEYS"));
        assertTrue(prompt.contains("IMAGE KEYS"));
        assertTrue(prompt.contains("DOCX CONTENT BLOCKS:"));
        assertTrue(prompt.contains("Template keys found: problem1, problem2"));
        assertTrue(prompt.contains("\"problemSolvingImg\": \"string\""));
        assertFalse(prompt.contains("LAYOUT CATALOG"));
        assertFalse(prompt.contains("CONTENT VARIATION"));
    }
}
