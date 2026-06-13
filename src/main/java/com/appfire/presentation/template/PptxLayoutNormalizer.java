package com.appfire.presentation.template;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.apache.poi.sl.usermodel.TextShape.TextAutofit;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTable;
import org.apache.poi.xslf.usermodel.XSLFTableCell;
import org.apache.poi.xslf.usermodel.XSLFTableRow;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PptxLayoutNormalizer {

    private static final Logger LOG = LoggerFactory.getLogger(PptxLayoutNormalizer.class);
    private static final double MIN_FONT_PT = 12.0;
    private static final double DEFAULT_FONT_PT = 16.0;

    private final boolean enabled;
    private final Set<Integer> staticSlideIndices;
    private final List<String> imageKeyNames;

    public PptxLayoutNormalizer(boolean enabled, Set<Integer> staticSlideIndices, List<String> imageKeyNames) {
        this.enabled = enabled;
        this.staticSlideIndices = staticSlideIndices == null ? Set.of() : Set.copyOf(staticSlideIndices);
        this.imageKeyNames = imageKeyNames == null ? List.of() : List.copyOf(imageKeyNames);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void hardenStructure(Path pptxPath) throws IOException {
        if (!enabled) {
            return;
        }
        mutateSlides(pptxPath, this::hardenTextShape);
        LOG.info("Layout hardening applied to {}", pptxPath.getFileName());
    }

    public void fitText(Path pptxPath) throws IOException {
        if (!enabled) {
            return;
        }
        try (InputStream input = Files.newInputStream(pptxPath);
                XMLSlideShow slideShow = new XMLSlideShow(input)) {
            List<XSLFSlide> slides = slideShow.getSlides();
            for (int slideIndex = 0; slideIndex < slides.size(); slideIndex++) {
                if (staticSlideIndices.contains(slideIndex)) {
                    continue;
                }
                fitSlideText(slides.get(slideIndex));
            }
            writeSlideShow(slideShow, pptxPath);
        }
        LOG.info("Text fitting applied to {}", pptxPath.getFileName());
    }

    private void mutateSlides(Path pptxPath, ShapeMutator mutator) throws IOException {
        try (InputStream input = Files.newInputStream(pptxPath);
                XMLSlideShow slideShow = new XMLSlideShow(input)) {
            for (XSLFSlide slide : slideShow.getSlides()) {
                applyToSlide(slide, mutator);
            }
            writeSlideShow(slideShow, pptxPath);
        }
    }

    private void applyToSlide(XSLFSlide slide, ShapeMutator mutator) {
        for (XSLFShape shape : slide.getShapes()) {
            if (shape instanceof XSLFTextShape textShape) {
                mutator.apply(textShape);
            } else if (shape instanceof XSLFTable table) {
                for (XSLFTableRow row : table.getRows()) {
                    for (XSLFTableCell cell : row.getCells()) {
                        mutator.apply(cell);
                    }
                }
            }
        }
    }

    private void hardenTextShape(XSLFTextShape shape) {
        shape.setWordWrap(true);
        shape.setTextAutofit(TextAutofit.NONE);
    }

    private void fitSlideText(XSLFSlide slide) {
        for (XSLFShape shape : slide.getShapes()) {
            if (shape instanceof XSLFTextShape textShape) {
                fitTextShape(textShape);
            } else if (shape instanceof XSLFTable table) {
                for (XSLFTableRow row : table.getRows()) {
                    for (XSLFTableCell cell : row.getCells()) {
                        fitTextShape(cell);
                    }
                }
            }
        }
    }

    private void fitTextShape(XSLFTextShape shape) {
        if (isImagePlaceholderShape(shape)) {
            return;
        }
        String text = shape.getText();
        if (text == null || text.isBlank()) {
            return;
        }
        Rectangle2D fixed = shape.getAnchor();
        if (fixed == null) {
            return;
        }
        shape.setWordWrap(true);
        shape.setTextAutofit(TextAutofit.NONE);

        Rectangle2D needed = shape.resizeToFitText();
        double scale = computeScale(fixed, needed);
        if (scale < 1.0) {
            applyFontScale(shape, scale);
            needed = shape.resizeToFitText();
            scale = computeScale(fixed, needed);
            if (scale < 1.0) {
                applyFontScale(shape, scale);
            }
        }
        shape.setAnchor(fixed);
    }

    private static double computeScale(Rectangle2D fixed, Rectangle2D needed) {
        if (needed == null || fixed == null) {
            return 1.0;
        }
        double heightScale = needed.getHeight() > 0
                ? Math.min(1.0, fixed.getHeight() / needed.getHeight())
                : 1.0;
        double widthScale = needed.getWidth() > 0
                ? Math.min(1.0, fixed.getWidth() / needed.getWidth())
                : 1.0;
        return Math.min(heightScale, widthScale);
    }

    private void applyFontScale(XSLFTextShape shape, double scale) {
        for (XSLFTextParagraph paragraph : shape.getTextParagraphs()) {
            for (XSLFTextRun run : paragraph.getTextRuns()) {
                if (run == null) {
                    continue;
                }
                double currentSize = run.getFontSize();
                if (currentSize <= 0) {
                    currentSize = DEFAULT_FONT_PT;
                }
                run.setFontSize(Math.max(MIN_FONT_PT, currentSize * scale));
            }
        }
    }

    private boolean isImagePlaceholderShape(XSLFTextShape shape) {
        String text = shape.getText();
        if (text == null || text.isBlank()) {
            return false;
        }
        for (String key : imageKeyNames) {
            if (text.contains("${" + key + "}")) {
                return true;
            }
        }
        return false;
    }

    private void writeSlideShow(XMLSlideShow slideShow, Path pptxPath) throws IOException {
        try (OutputStream output = Files.newOutputStream(pptxPath)) {
            slideShow.write(output);
        }
    }

    @FunctionalInterface
    private interface ShapeMutator {
        void apply(XSLFTextShape shape);
    }
}
