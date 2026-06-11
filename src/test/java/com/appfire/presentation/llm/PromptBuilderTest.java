package com.appfire.presentation.llm;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.appfire.presentation.model.BlockType;
import com.appfire.presentation.model.ContentBlock;
import com.appfire.presentation.model.DocumentContent;
import com.appfire.presentation.model.LayoutBlueprint;
import com.appfire.presentation.model.LayoutCatalog;
import com.appfire.presentation.model.PresentationBlueprint;
import com.appfire.presentation.model.SlideBlueprint;
import com.appfire.presentation.model.ThemeBlueprint;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class PromptBuilderTest {

    @Test
    void buildsPromptFromMarkdownTemplates() {
        PresentationBlueprint blueprint = new PresentationBlueprint(
                List.of(new SlideBlueprint(0, "Title Slide", List.of(), "")),
                new ThemeBlueprint(List.of("Arial"), List.of("#000000")),
                new LayoutCatalog(List.of(
                        new LayoutBlueprint("Title Slide", true, false, false, false, 0))));
        DocumentContent document = new DocumentContent(
                List.of(new ContentBlock(0, BlockType.HEADING, 1, "Overview")),
                "Overview");

        PromptBuilder builder = new PromptBuilder(new PromptLoader(Path.of("prompts")));
        String prompt = builder.build(blueprint, document);

        assertTrue(prompt.contains("NON-NEGOTIABLE RULES:"));
        assertTrue(prompt.contains("CONTENT VARIATION RULES:"));
        assertTrue(prompt.contains("LAYOUT CATALOG"));
        assertTrue(prompt.contains("PRESENTATION BLUEPRINT:"));
        assertTrue(prompt.contains("DOCX CONTENT BLOCKS:"));
        assertTrue(prompt.contains("Template slide count: 1"));
        assertTrue(prompt.contains("\"imagePosition\": \"left|right|top|none\""));
    }
}
