package com.appfire.presentation.template;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.appfire.presentation.TestFixtures;
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
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PptxLayoutNormalizerTest {

    @TempDir
    Path tempDir;

    @Test
    void hardenStructureDisablesAutofitAndEnablesWrap() throws IOException {
        Path template = TestFixtures.resolveTemplatePptx();
        assumeTrue(Files.exists(template), "template PPTX not available");

        Path workingCopy = tempDir.resolve("working.pptx");
        Files.copy(template, workingCopy);
        new PptxLayoutNormalizer(true, Set.of(), List.of()).hardenStructure(workingCopy);

        try (InputStream input = Files.newInputStream(workingCopy);
                XMLSlideShow slideShow = new XMLSlideShow(input)) {
            XSLFSlide slide = slideShow.getSlides().get(2);
            XSLFTextShape body = findFirstTextShape(slide);
            assertTrue(body.getWordWrap());
            assertFalse(hasAutofit(body));
        }
    }

    @Test
    void fitTextReducesFontSizeForLongContent() throws IOException {
        Path workingCopy = tempDir.resolve("fit.pptx");
        createLongTextFixture(workingCopy);

        PptxLayoutNormalizer normalizer = new PptxLayoutNormalizer(true, Set.of(), List.of());
        normalizer.hardenStructure(workingCopy);
        normalizer.fitText(workingCopy);

        try (InputStream input = Files.newInputStream(workingCopy);
                XMLSlideShow slideShow = new XMLSlideShow(input)) {
            XSLFTextShape body = findFirstTextShape(slideShow.getSlides().get(1));
            double fontSize = body.getTextParagraphs().get(0).getTextRuns().get(0).getFontSize();
            assertTrue(fontSize >= 12.0 && fontSize < 16.0, "expected reduced font size, got " + fontSize);
        }
    }

    private static void createLongTextFixture(Path path) throws IOException {
        try (XMLSlideShow slideShow = new XMLSlideShow()) {
            slideShow.createSlide();
            XSLFSlide contentSlide = slideShow.createSlide();
            XSLFTextShape shape = contentSlide.createTextBox();
            shape.setAnchor(new Rectangle2D.Double(50, 50, 120, 50));
            shape.setTextAutofit(TextAutofit.NORMAL);
            shape.setText("Overflow ".repeat(40).trim());
            shape.getTextParagraphs().get(0).getTextRuns().get(0).setFontSize(16.0);
            try (OutputStream output = Files.newOutputStream(path)) {
                slideShow.write(output);
            }
        }
    }

    private static XSLFTextShape findFirstTextShape(XSLFSlide slide) {
        return slide.getShapes().stream()
                .filter(XSLFTextShape.class::isInstance)
                .map(XSLFTextShape.class::cast)
                .findFirst()
                .orElseThrow();
    }

    private static boolean hasAutofit(XSLFTextShape shape) {
        TextAutofit autofit = shape.getTextAutofit();
        return autofit != null && autofit != TextAutofit.NONE;
    }
}
