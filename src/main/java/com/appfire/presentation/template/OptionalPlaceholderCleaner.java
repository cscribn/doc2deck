package com.appfire.presentation.template;

import com.appfire.presentation.config.PresentationKeysConfig;
import com.appfire.presentation.model.PresentationContentResponse;
import com.appfire.presentation.model.KeyPopulation;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTable;
import org.apache.poi.xslf.usermodel.XSLFTableCell;
import org.apache.poi.xslf.usermodel.XSLFTableRow;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OptionalPlaceholderCleaner {

    private static final Logger LOG = LoggerFactory.getLogger(OptionalPlaceholderCleaner.class);

    public void clean(Path pptxPath, PresentationContentResponse response, PresentationKeysConfig keysConfig)
            throws IOException {
        List<String> emptyOptionalKeys = findEmptyOptionalKeys(response, keysConfig);
        if (emptyOptionalKeys.isEmpty()) {
            return;
        }

        try (InputStream input = Files.newInputStream(pptxPath);
                XMLSlideShow slideShow = new XMLSlideShow(input)) {
            for (XSLFSlide slide : slideShow.getSlides()) {
                for (XSLFShape shape : slide.getShapes()) {
                    cleanShape(shape, emptyOptionalKeys);
                }
            }
            writeOutput(slideShow, pptxPath);
        }

        LOG.info("Removed placeholder bullets for optional key(s): {}", emptyOptionalKeys);
    }

    private List<String> findEmptyOptionalKeys(
            PresentationContentResponse response,
            PresentationKeysConfig keysConfig) {
        List<String> empty = new ArrayList<>();
        for (String key : keysConfig.optionalTextKeyNames()) {
            if (!KeyPopulation.isPopulated(key, response.keys().get(key))) {
                empty.add(key);
            }
        }
        return empty;
    }

    private void cleanShape(XSLFShape shape, List<String> emptyOptionalKeys) {
        if (shape instanceof XSLFTextShape textShape) {
            removeOptionalParagraphs(textShape, emptyOptionalKeys);
        } else if (shape instanceof XSLFTable table) {
            for (XSLFTableRow row : table.getRows()) {
                for (XSLFTableCell cell : row.getCells()) {
                    removeOptionalParagraphs(cell, emptyOptionalKeys);
                }
            }
        }
    }

    private void removeOptionalParagraphs(XSLFTextShape textShape, List<String> emptyOptionalKeys) {
        List<XSLFTextParagraph> paragraphs = new ArrayList<>(textShape.getTextParagraphs());
        for (int i = paragraphs.size() - 1; i >= 0; i--) {
            XSLFTextParagraph paragraph = paragraphs.get(i);
            String text = paragraph.getText();
            if (text == null) {
                continue;
            }
            for (String key : emptyOptionalKeys) {
                if (matchesOptionalPlaceholder(text, key)) {
                    textShape.removeTextParagraph(paragraph);
                    break;
                }
            }
        }
    }

    private boolean matchesOptionalPlaceholder(String text, String key) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        String token = "${" + key + "}";
        if (trimmed.contains(token)) {
            return true;
        }
        String sanitized = TextSanitizer.sanitize(trimmed);
        return sanitized.equalsIgnoreCase(key) || sanitized.equalsIgnoreCase(token);
    }

    private void writeOutput(XMLSlideShow slideShow, Path outputPath) throws IOException {
        try (OutputStream output = Files.newOutputStream(outputPath)) {
            slideShow.write(output);
        }
    }
}
