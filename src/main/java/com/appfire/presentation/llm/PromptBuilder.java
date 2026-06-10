package com.appfire.presentation.llm;

import com.appfire.presentation.model.ContentBlock;
import com.appfire.presentation.model.DocumentContent;
import com.appfire.presentation.model.PresentationBlueprint;
import com.appfire.presentation.model.ShapeBlueprint;
import com.appfire.presentation.model.SlideBlueprint;
import java.util.List;

public final class PromptBuilder {

    private static final int MAX_SUMMARY_CHARS = 100_000;

    public String build(PresentationBlueprint blueprint, DocumentContent document) {
        StringBuilder prompt = new StringBuilder();
        appendRules(prompt);
        appendBlueprint(prompt, blueprint);
        appendDocument(prompt, document);
        appendOutputContract(prompt, blueprint.slides().size());
        return prompt.toString();
    }

    private void appendRules(StringBuilder prompt) {
        prompt.append("NON-NEGOTIABLE RULES:\n");
        prompt.append("1. Use only facts from the provided DOCX and PPTX source text. No fabrication.\n");
        prompt.append("2. One core concept per slide. Maximum 4 bullets per slide.\n");
        prompt.append("3. Two fonts maximum. Title at least 32pt, body at least 18pt.\n");
        prompt.append("4. High contrast. Maximum three theme colors.\n");
        prompt.append("5. Narrative flow: Title/Hook, Context, Deliverables, Next Steps.\n");
        prompt.append("6. Round numbers to nearest whole or single decimal unless precision required.\n");
        prompt.append("7. Stateless: use only this prompt. No prior conversation.\n\n");
    }

    private void appendBlueprint(StringBuilder prompt, PresentationBlueprint blueprint) {
        prompt.append("PRESENTATION BLUEPRINT:\n");
        for (SlideBlueprint slide : blueprint.slides()) {
            prompt.append("Slide ").append(slide.slideIndex())
                    .append(" layout=").append(slide.layoutName()).append("\n");
            if (!slide.notes().isBlank()) {
                prompt.append("  notes: ").append(slide.notes()).append("\n");
            }
            for (ShapeBlueprint shape : slide.shapes()) {
                prompt.append("  shape ").append(shape.shapeId())
                        .append(" type=").append(shape.placeholderType())
                        .append(" text=").append(shape.currentText()).append("\n");
            }
        }
        prompt.append("Theme fonts: ").append(blueprint.theme().fontFamilies()).append("\n");
        prompt.append("Theme colors: ").append(blueprint.theme().colors()).append("\n\n");
    }

    private void appendDocument(StringBuilder prompt, DocumentContent document) {
        prompt.append("DOCX CONTENT BLOCKS:\n");
        for (ContentBlock block : document.blocks()) {
            prompt.append("Block ").append(block.index())
                    .append(" type=").append(block.type());
            if (block.headingLevel() > 0) {
                prompt.append(" level=").append(block.headingLevel());
            }
            if (!block.text().isBlank()) {
                prompt.append(" text=").append(block.text());
            }
            if (!block.items().isEmpty()) {
                prompt.append(" items=").append(block.items());
            }
            prompt.append("\n");
        }
        String summary = document.flatSummary();
        if (summary.length() > MAX_SUMMARY_CHARS) {
            prompt.append("\nDOCX SUMMARY (truncated):\n")
                    .append(summary, 0, MAX_SUMMARY_CHARS)
                    .append("\n[TRUNCATED]\n\n");
        } else {
            prompt.append("\nDOCX SUMMARY:\n").append(summary).append("\n\n");
        }
    }

    private void appendOutputContract(StringBuilder prompt, int slideCount) {
        prompt.append("INSTRUCTIONS:\n");
        prompt.append("1. Map DOCX content onto the template slides.\n");
        prompt.append("2. Use action replace for existing slides, append for new slides, skip to keep unchanged.\n");
        prompt.append("3. Cite source block indices in sourceRefs for each slide.\n");
        prompt.append("4. Return JSON only. No markdown fences. No commentary.\n\n");
        prompt.append("Template slide count: ").append(slideCount).append("\n");
        prompt.append("JSON schema:\n");
        prompt.append("""
                {
                  "slides": [
                    {
                      "slideIndex": 0,
                      "action": "replace",
                      "title": "string",
                      "bullets": ["string"],
                      "bodyText": "string or null",
                      "notes": "string or null",
                      "sourceRefs": [0]
                    }
                  ],
                  "warnings": ["string"]
                }
                """);
    }
}
