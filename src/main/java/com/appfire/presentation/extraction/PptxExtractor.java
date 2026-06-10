package com.appfire.presentation.extraction;

import com.appfire.presentation.model.ExtractedPresentation;
import com.appfire.presentation.model.PresentationBlueprint;
import com.appfire.presentation.model.ShapeBlueprint;
import com.appfire.presentation.model.SlideBlueprint;
import com.appfire.presentation.model.ThemeBlueprint;
import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.poi.sl.usermodel.PaintStyle;
import org.apache.poi.sl.usermodel.Placeholder;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFNotes;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PptxExtractor {

    private static final Logger LOG = LoggerFactory.getLogger(PptxExtractor.class);

    public ExtractedPresentation extract(Path pptxPath) throws IOException {
        try (InputStream input = Files.newInputStream(pptxPath)) {
            XMLSlideShow slideShow = new XMLSlideShow(input);
            List<SlideBlueprint> slides = extractSlides(slideShow);
            ThemeBlueprint theme = extractTheme(slides);
            PresentationBlueprint blueprint = new PresentationBlueprint(slides, theme);
            LOG.info("Extracted {} slides from {}", slides.size(), pptxPath);
            return new ExtractedPresentation(blueprint, slideShow);
        }
    }

    private List<SlideBlueprint> extractSlides(XMLSlideShow slideShow) {
        List<SlideBlueprint> slides = new ArrayList<>();
        List<XSLFSlide> slideList = slideShow.getSlides();
        for (int i = 0; i < slideList.size(); i++) {
            slides.add(extractSlide(slideList.get(i), i));
        }
        return slides;
    }

    private SlideBlueprint extractSlide(XSLFSlide slide, int index) {
        String layoutName = slide.getSlideLayout() != null
                ? slide.getSlideLayout().getName()
                : "unknown";
        List<ShapeBlueprint> shapes = new ArrayList<>();
        int shapeCounter = 0;
        for (XSLFShape shape : slide.getShapes()) {
            if (shape instanceof XSLFTextShape textShape) {
                shapes.add(toShapeBlueprint(textShape, shapeCounter++));
            }
        }
        return new SlideBlueprint(index, layoutName, shapes, extractNotes(slide));
    }

    private ShapeBlueprint toShapeBlueprint(XSLFTextShape textShape, int shapeCounter) {
        String placeholderType = resolvePlaceholderType(textShape);
        String shapeId = placeholderType + "-" + shapeCounter;
        String text = textShape.getText();
        FontStyle style = extractFontStyle(textShape);
        return new ShapeBlueprint(
                shapeId, placeholderType, text, style.family(), style.sizePt(), style.colorHex());
    }

    private String resolvePlaceholderType(XSLFTextShape textShape) {
        Placeholder placeholder = textShape.getPlaceholder();
        if (placeholder == null) {
            return "CUSTOM";
        }
        return placeholder.name();
    }

    private FontStyle extractFontStyle(XSLFTextShape textShape) {
        for (XSLFTextParagraph paragraph : textShape.getTextParagraphs()) {
            for (XSLFTextRun run : paragraph.getTextRuns()) {
                String family = run.getFontFamily();
                Double size = run.getFontSize();
                String color = colorToHex(run.getFontColor());
                if (family != null || size != null || color != null) {
                    return new FontStyle(family, size, color);
                }
            }
        }
        return new FontStyle(null, null, null);
    }

    private String extractNotes(XSLFSlide slide) {
        XSLFNotes notes = slide.getNotes();
        if (notes == null) {
            return "";
        }
        for (XSLFShape shape : notes.getShapes()) {
            if (shape instanceof XSLFTextShape textShape) {
                String text = textShape.getText();
                if (text != null && !text.isBlank()) {
                    return text.trim();
                }
            }
        }
        return "";
    }

    private ThemeBlueprint extractTheme(List<SlideBlueprint> slides) {
        Set<String> fonts = new LinkedHashSet<>();
        Set<String> colors = new LinkedHashSet<>();
        for (SlideBlueprint slide : slides) {
            for (ShapeBlueprint shape : slide.shapes()) {
                if (shape.fontFamily() != null) {
                    fonts.add(shape.fontFamily());
                }
                if (shape.colorHex() != null) {
                    colors.add(shape.colorHex());
                }
            }
        }
        return new ThemeBlueprint(
                fonts.stream().limit(2).toList(),
                colors.stream().limit(3).toList());
    }

    private String colorToHex(PaintStyle paintStyle) {
        if (paintStyle instanceof PaintStyle.SolidPaint solid) {
            if (solid.getSolidColor() != null) {
                Color color = solid.getSolidColor().getColor();
                if (color != null) {
                    return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
                }
            }
        }
        return null;
    }

    private record FontStyle(String family, Double sizePt, String colorHex) {
    }
}
