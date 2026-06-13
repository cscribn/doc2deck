package com.appfire.presentation.template;

import com.appfire.presentation.config.PresentationKeysConfig;
import com.appfire.presentation.model.TemplateScanResult;
import com.appfire.presentation.model.TemplateScanResult.ImageKeyAnchor;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

public final class TemplateScanner {

    private static final Logger LOG = LoggerFactory.getLogger(TemplateScanner.class);
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([a-zA-Z][a-zA-Z0-9_]*)\\}");

    public TemplateScanResult scan(Path templatePath, PresentationKeysConfig keysConfig) throws IOException {
        try (InputStream input = Files.newInputStream(templatePath);
                XMLSlideShow slideShow = new XMLSlideShow(input)) {
            Set<String> foundKeys = new HashSet<>();
            List<String> splitRunWarnings = new ArrayList<>();
            List<ImageKeyAnchor> imageAnchors = new ArrayList<>();

            List<XSLFSlide> slides = slideShow.getSlides();
            for (int slideIndex = 0; slideIndex < slides.size(); slideIndex++) {
                scanSlide(
                        slides.get(slideIndex),
                        slideIndex,
                        foundKeys,
                        splitRunWarnings,
                        imageAnchors,
                        keysConfig.imageKeyNames());
            }

            splitRunWarnings.forEach(msg -> LOG.warn("Template scan: {}", msg));
            LOG.info("Template scan found {} placeholder key(s)", foundKeys.size());
            return new TemplateScanResult(foundKeys, splitRunWarnings, imageAnchors);
        }
    }

    private void scanSlide(
            XSLFSlide slide,
            int slideIndex,
            Set<String> foundKeys,
            List<String> splitRunWarnings,
            List<ImageKeyAnchor> imageAnchors,
            List<String> imageKeys) {
        for (XSLFShape shape : slide.getShapes()) {
            if (shape instanceof XSLFTextShape textShape) {
                scanTextShape(textShape, slideIndex, foundKeys, splitRunWarnings, imageAnchors, imageKeys);
            } else if (shape instanceof XSLFTable table) {
                scanTable(table, slideIndex, foundKeys, splitRunWarnings, imageAnchors, imageKeys);
            }
        }
    }

    private void scanTable(
            XSLFTable table,
            int slideIndex,
            Set<String> foundKeys,
            List<String> splitRunWarnings,
            List<ImageKeyAnchor> imageAnchors,
            List<String> imageKeys) {
        for (XSLFTableRow row : table.getRows()) {
            for (XSLFTableCell cell : row.getCells()) {
                scanTextShape(cell, slideIndex, foundKeys, splitRunWarnings, imageAnchors, imageKeys);
            }
        }
    }

    private void scanTextShape(
            XSLFTextShape textShape,
            int slideIndex,
            Set<String> foundKeys,
            List<String> splitRunWarnings,
            List<ImageKeyAnchor> imageAnchors,
            List<String> imageKeys) {
        String fullText = textShape.getText();
        collectKeys(fullText, foundKeys);

        if (fullText != null && fullText.contains("${")) {
            checkSplitRuns(textShape, slideIndex, splitRunWarnings);
        }

        for (String key : imageKeys) {
            if (fullText != null && fullText.contains("${" + key + "}")) {
                Rectangle2D anchor = textShape.getAnchor();
                imageAnchors.add(new ImageKeyAnchor(key, slideIndex, textShape.getShapeId(), anchor));
            }
        }
    }

    private void collectKeys(String text, Set<String> foundKeys) {
        if (text == null || text.isBlank()) {
            return;
        }
        Matcher matcher = PLACEHOLDER.matcher(text);
        while (matcher.find()) {
            foundKeys.add(matcher.group(1));
        }
    }

    private void checkSplitRuns(XSLFTextShape textShape, int slideIndex, List<String> splitRunWarnings) {
        for (XSLFTextParagraph paragraph : textShape.getTextParagraphs()) {
            StringBuilder runsText = new StringBuilder();
            for (XSLFTextRun run : paragraph.getTextRuns()) {
                String raw = run.getRawText();
                if (raw != null) {
                    runsText.append(raw);
                }
            }
            String combined = runsText.toString();
            if (combined.contains("$") && !combined.contains("${")) {
                splitRunWarnings.add("Slide " + slideIndex + " shape " + textShape.getShapeId()
                        + " may have split placeholder runs. Ensure each ${key} is a single text run.");
            }
        }
    }
}
