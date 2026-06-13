package com.appfire.presentation.template;

import static org.junit.jupiter.api.Assertions.assertFalse;

import com.appfire.presentation.config.PresentationKeysConfig;
import com.appfire.presentation.config.PresentationKeysConfigLoader;
import com.appfire.presentation.model.PresentationContentResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.apache.poi.sl.usermodel.Placeholder;
import org.apache.poi.xslf.usermodel.SlideLayout;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFSlideLayout;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OptionalPlaceholderCleanerTest {

    @TempDir
    Path tempDir;

    @Test
    void removesOptionalPlaceholderBulletWhenKeyAbsent() throws Exception {
        Path template = tempDir.resolve("template.pptx");
        createTemplateWithOptionalBullet(template);
        PresentationKeysConfig keysConfig = PresentationKeysConfigLoader.load(
                Path.of("presentation-keys.example.properties"));

        PresentationContentResponse response = new PresentationContentResponse(
                Map.of("sprintsToDeliver", "4 two-week sprints"),
                Map.of(),
                java.util.List.of());

        new PptxTemplateReplacer().replace(template, response, keysConfig, template);
        new OptionalPlaceholderCleaner().clean(template, response, keysConfig);

        try (InputStream in = Files.newInputStream(template);
                XMLSlideShow slideShow = new XMLSlideShow(in)) {
            String text = slideShow.getSlides().get(0).getShapes().stream()
                    .filter(XSLFTextShape.class::isInstance)
                    .map(s -> ((XSLFTextShape) s).getText())
                    .reduce("", String::concat);
            assertFalse(text.contains("${nonDevCosts}"));
            assertFalse(text.toLowerCase().contains("nondevcosts"));
        }
    }

    private void createTemplateWithOptionalBullet(Path path) throws Exception {
        try (XMLSlideShow slideShow = new XMLSlideShow()) {
            XSLFSlideLayout layout = slideShow.getSlideMasters().get(0).getLayout(SlideLayout.TITLE_AND_CONTENT);
            XSLFSlide slide = slideShow.createSlide(layout);
            if (slide.getPlaceholder(Placeholder.BODY) instanceof XSLFTextShape body) {
                body.setText("${sprintsToDeliver}\n${nonDevCosts}");
            }
            try (OutputStream out = Files.newOutputStream(path)) {
                slideShow.write(out);
            }
        }
    }
}
