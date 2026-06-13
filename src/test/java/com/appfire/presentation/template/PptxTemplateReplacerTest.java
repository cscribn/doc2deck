package com.appfire.presentation.template;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.appfire.presentation.TestFixtureGenerator;
import com.appfire.presentation.TestFixtures;
import com.appfire.presentation.config.PresentationKeysConfigLoader;
import com.appfire.presentation.model.PresentationContentResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PptxTemplateReplacerTest {

    @TempDir
    Path tempDir;

    @BeforeAll
    static void ensureFixtures() throws Exception {
        TestFixtureGenerator.ensureFixtures();
    }

    @Test
    void replacesTextPlaceholders() throws Exception {
        assumeTrue(TestFixtures.hasTemplatePptx(), "template.pptx not available");

        ObjectMapper mapper = new ObjectMapper();
        PresentationContentResponse response;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("valid-response.json")) {
            response = mapper.readValue(in, PresentationContentResponse.class);
        }

        Path output = tempDir.resolve("replaced.pptx");
        var keysConfig = PresentationKeysConfigLoader.load(Path.of("presentation-keys.example.properties"));
        new PptxTemplateReplacer().replace(TestFixtures.resolveTemplatePptx(), response, keysConfig, output);

        try (InputStream in = Files.newInputStream(output);
                XMLSlideShow slideShow = new XMLSlideShow(in)) {
            XSLFSlide slide = slideShow.getSlides().get(0);
            String text = collectText(slide);
            assertFalse(text.contains("${problem1}"));
            assertFalse(text.contains("${shortProjectDescription}"));
        }
    }

    private String collectText(XSLFSlide slide) {
        StringBuilder builder = new StringBuilder();
        slide.getShapes().forEach(shape -> {
            if (shape instanceof XSLFTextShape textShape) {
                builder.append(textShape.getText());
            }
        });
        return builder.toString();
    }
}
