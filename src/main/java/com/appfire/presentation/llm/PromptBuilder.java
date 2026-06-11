package com.appfire.presentation.llm;

import com.appfire.presentation.model.ContentBlock;
import com.appfire.presentation.model.DocumentContent;
import com.appfire.presentation.model.LayoutBlueprint;
import com.appfire.presentation.model.PresentationBlueprint;
import com.appfire.presentation.model.ShapeBlueprint;
import com.appfire.presentation.model.SlideBlueprint;
import java.util.Map;

public final class PromptBuilder {

    private static final int MAX_SUMMARY_CHARS = 100_000;

    private final PromptLoader promptLoader;

    public PromptBuilder() {
        this(new PromptLoader());
    }

    public PromptBuilder(PromptLoader promptLoader) {
        this.promptLoader = promptLoader;
    }

    public String build(PresentationBlueprint blueprint, DocumentContent document) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(promptLoader.load("prompt_core_rules.md"));
        prompt.append("\n\n");
        prompt.append(promptLoader.load("prompt_content_variation.md"));
        prompt.append("\n\n");
        prompt.append(buildLayoutCatalog(blueprint));
        prompt.append(buildBlueprint(blueprint));
        prompt.append(buildDocument(document));
        prompt.append(buildOutputContract(blueprint.slides().size()));
        return prompt.toString();
    }

    private String buildLayoutCatalog(PresentationBlueprint blueprint) {
        StringBuilder entries = new StringBuilder();
        for (LayoutBlueprint layout : blueprint.layoutCatalog().layouts()) {
            entries.append("- name=").append(layout.name())
                    .append(" hasTitle=").append(layout.hasTitle())
                    .append(" hasBody=").append(layout.hasBody())
                    .append(" hasPicture=").append(layout.hasPicture())
                    .append(" hasTwoColumns=").append(layout.hasTwoColumns())
                    .append(" bodyPlaceholders=").append(layout.bodyPlaceholderCount())
                    .append("\n");
        }
        return promptLoader.apply(
                promptLoader.load("prompt_layout_catalog.md"),
                Map.of("ENTRIES", entries.toString()));
    }

    private String buildBlueprint(PresentationBlueprint blueprint) {
        StringBuilder slides = new StringBuilder();
        for (SlideBlueprint slide : blueprint.slides()) {
            slides.append("Slide ").append(slide.slideIndex())
                    .append(" layout=").append(slide.layoutName()).append("\n");
            if (!slide.notes().isBlank()) {
                slides.append("  notes: ").append(slide.notes()).append("\n");
            }
            for (ShapeBlueprint shape : slide.shapes()) {
                slides.append("  shape ").append(shape.shapeId())
                        .append(" type=").append(shape.placeholderType())
                        .append(" text=").append(shape.currentText()).append("\n");
            }
        }
        return promptLoader.apply(
                promptLoader.load("prompt_presentation_blueprint.md"),
                Map.of(
                        "SLIDES", slides.toString(),
                        "THEME_FONTS", blueprint.theme().fontFamilies().toString(),
                        "THEME_COLORS", blueprint.theme().colors().toString()));
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

    private String buildOutputContract(int slideCount) {
        return promptLoader.apply(
                promptLoader.load("prompt_output_contract.md"),
                Map.of("SLIDE_COUNT", Integer.toString(slideCount)));
    }
}
