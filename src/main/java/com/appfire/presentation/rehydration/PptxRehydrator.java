package com.appfire.presentation.rehydration;

import com.appfire.presentation.model.ExtractedPresentation;
import com.appfire.presentation.model.GenerationResponse;
import com.appfire.presentation.model.ShapeBlueprint;
import com.appfire.presentation.model.SlideAction;
import com.appfire.presentation.model.SlideBlueprint;
import com.appfire.presentation.model.SlideDirective;
import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.apache.poi.sl.usermodel.Placeholder;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFSlideLayout;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PptxRehydrator {

    private static final Logger LOG = LoggerFactory.getLogger(PptxRehydrator.class);

    public void rehydrate(ExtractedPresentation extracted, GenerationResponse response, Path outputPath)
            throws IOException {
        XMLSlideShow slideShow = extracted.slideShow();
        for (SlideDirective directive : response.slides()) {
            applyDirective(slideShow, extracted, directive);
        }
        writeOutput(slideShow, outputPath);
    }

    private void applyDirective(XMLSlideShow slideShow, ExtractedPresentation extracted, SlideDirective directive) {
        if (directive.action() == SlideAction.SKIP) {
            return;
        }
        XSLFSlide slide = resolveSlide(slideShow, directive);
        if (slide == null) {
            LOG.error("Could not resolve slide for index {}. Resolution: check template layouts.",
                    directive.slideIndex());
            return;
        }
        SlideBlueprint blueprint = findBlueprint(extracted, directive.slideIndex());
        applyContent(slide, directive, blueprint);
    }

    private XSLFSlide resolveSlide(XMLSlideShow slideShow, SlideDirective directive) {
        if (directive.action() == SlideAction.APPEND) {
            return createAppendSlide(slideShow);
        }
        List<XSLFSlide> slides = slideShow.getSlides();
        if (directive.slideIndex() >= 0 && directive.slideIndex() < slides.size()) {
            return slides.get(directive.slideIndex());
        }
        return null;
    }

    private XSLFSlide createAppendSlide(XMLSlideShow slideShow) {
        XSLFSlideLayout layout = findContentLayout(slideShow);
        if (layout == null) {
            LOG.error("No content layout found for append. Resolution: add a TITLE_AND_CONTENT layout to template.");
            return null;
        }
        return slideShow.createSlide(layout);
    }

    private XSLFSlideLayout findContentLayout(XMLSlideShow slideShow) {
        for (XSLFSlideLayout layout : slideShow.getSlideMasters().get(0).getSlideLayouts()) {
            String name = layout.getName() != null ? layout.getName().toUpperCase() : "";
            if (name.contains("TITLE") && name.contains("CONTENT")) {
                return layout;
            }
        }
        XSLFSlideLayout[] layouts = slideShow.getSlideMasters().get(0).getSlideLayouts();
        return layouts.length > 1 ? layouts[1] : layouts[0];
    }

    private SlideBlueprint findBlueprint(ExtractedPresentation extracted, int slideIndex) {
        return extracted.blueprint().slides().stream()
                .filter(s -> s.slideIndex() == slideIndex)
                .findFirst()
                .orElse(null);
    }

    private void applyContent(XSLFSlide slide, SlideDirective directive, SlideBlueprint blueprint) {
        setPlaceholderText(slide, Placeholder.TITLE, directive.title(), blueprint, true);
        String body = buildBodyText(directive);
        if (!body.isBlank()) {
            setPlaceholderText(slide, Placeholder.BODY, body, blueprint, false);
        }
    }

    private String buildBodyText(SlideDirective directive) {
        if (directive.bodyText() != null && !directive.bodyText().isBlank()) {
            return directive.bodyText();
        }
        if (directive.bullets() == null || directive.bullets().isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String bullet : directive.bullets()) {
            builder.append("\u2022 ").append(bullet).append("\n");
        }
        return builder.toString().trim();
    }

    private void setPlaceholderText(
            XSLFSlide slide,
            Placeholder placeholder,
            String text,
            SlideBlueprint blueprint,
            boolean isTitle) {
        Optional<XSLFTextShape> shape = findTextShape(slide, placeholder, blueprint, isTitle);
        if (shape.isEmpty()) {
            LOG.error("Slide {} has no {} placeholder. Resolution: check template layout.",
                    slide.getSlideNumber() - 1, placeholder.name());
            return;
        }
        XSLFTextShape textShape = shape.get();
        textShape.clearText();
        XSLFTextParagraph paragraph = textShape.addNewTextParagraph();
        XSLFTextRun run = paragraph.addNewTextRun();
        run.setText(text);
        applyStyle(run, blueprint, placeholder.name(), isTitle);
    }

    private Optional<XSLFTextShape> findTextShape(
            XSLFSlide slide,
            Placeholder placeholder,
            SlideBlueprint blueprint,
            boolean isTitle) {
        Optional<XSLFTextShape> direct = findPlaceholder(slide, placeholder);
        if (direct.isPresent()) {
            return direct;
        }
        Optional<XSLFTextShape> fromBlueprint = findFromBlueprint(slide, blueprint, placeholder.name());
        if (fromBlueprint.isPresent()) {
            return fromBlueprint;
        }
        return findByPosition(slide, isTitle);
    }

    private Optional<XSLFTextShape> findPlaceholder(XSLFSlide slide, Placeholder placeholder) {
        if (slide.getPlaceholder(placeholder) instanceof XSLFTextShape direct) {
            return Optional.of(direct);
        }
        for (var shape : slide.getShapes()) {
            if (shape instanceof XSLFTextShape textShape
                    && textShape.getPlaceholder() == placeholder) {
                return Optional.of(textShape);
            }
        }
        return Optional.empty();
    }

    private Optional<XSLFTextShape> findFromBlueprint(
            XSLFSlide slide, SlideBlueprint blueprint, String placeholderType) {
        if (blueprint == null) {
            return Optional.empty();
        }
        boolean hasMatch = blueprint.shapes().stream()
                .anyMatch(s -> s.placeholderType().equals(placeholderType));
        if (!hasMatch) {
            return Optional.empty();
        }
        for (var shape : slide.getShapes()) {
            if (shape instanceof XSLFTextShape textShape) {
                Placeholder ph = textShape.getPlaceholder();
                if (ph != null && ph.name().equals(placeholderType)) {
                    return Optional.of(textShape);
                }
            }
        }
        return Optional.empty();
    }

    private Optional<XSLFTextShape> findByPosition(XSLFSlide slide, boolean isTitle) {
        List<XSLFTextShape> textShapes = slide.getShapes().stream()
                .filter(XSLFTextShape.class::isInstance)
                .map(XSLFTextShape.class::cast)
                .toList();
        if (textShapes.isEmpty()) {
            return Optional.empty();
        }
        int index = isTitle ? 0 : Math.min(1, textShapes.size() - 1);
        return Optional.of(textShapes.get(index));
    }

    private void applyStyle(XSLFTextRun run, SlideBlueprint blueprint, String placeholderType, boolean isTitle) {
        ShapeBlueprint shapeStyle = findShapeStyle(blueprint, placeholderType);
        if (shapeStyle == null) {
            return;
        }
        if (shapeStyle.fontFamily() != null) {
            run.setFontFamily(shapeStyle.fontFamily());
        }
        if (shapeStyle.fontSizePt() != null) {
            run.setFontSize(shapeStyle.fontSizePt());
        }
        if (shapeStyle.colorHex() != null) {
            run.setFontColor(parseColor(shapeStyle.colorHex()));
        }
        if (isTitle && shapeStyle.fontSizePt() == null) {
            run.setFontSize(32.0);
        }
        if (!isTitle && shapeStyle.fontSizePt() == null) {
            run.setFontSize(18.0);
        }
    }

    private ShapeBlueprint findShapeStyle(SlideBlueprint blueprint, String placeholderType) {
        if (blueprint == null) {
            return null;
        }
        return blueprint.shapes().stream()
                .filter(s -> s.placeholderType().equals(placeholderType))
                .findFirst()
                .orElse(blueprint.shapes().isEmpty() ? null : blueprint.shapes().get(0));
    }

    private Color parseColor(String hex) {
        String normalized = hex.startsWith("#") ? hex.substring(1) : hex;
        int rgb = Integer.parseInt(normalized, 16);
        return new Color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
    }

    private void writeOutput(XMLSlideShow slideShow, Path outputPath) throws IOException {
        Path parent = outputPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (OutputStream out = Files.newOutputStream(outputPath)) {
            slideShow.write(out);
        }
        LOG.info("Wrote presentation to {}", outputPath);
    }
}
