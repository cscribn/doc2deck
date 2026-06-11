package com.appfire.presentation.template;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.appfire.presentation.images.PresentationImageOptimizer;
import com.appfire.presentation.model.ImageKeyPlan;
import com.appfire.presentation.model.ImageKeyPlan.ResolvedImageKey;
import com.appfire.presentation.model.PresentationKeys;
import com.appfire.presentation.model.TemplateScanResult;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.sl.usermodel.Placeholder;
import org.apache.poi.xslf.usermodel.SlideLayout;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFSlideLayout;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ImageInserterTest {

    @TempDir
    Path tempDir;

    @Test
    void invokesOptimizerBeforeWritingOutput() throws Exception {
        Path input = tempDir.resolve("input.pptx");
        Path output = tempDir.resolve("output.pptx");
        createTemplateWithImagePlaceholder(input);
        TemplateScanResult scan = new TemplateScanner().scan(input);

        PresentationImageOptimizer optimizer = mock(PresentationImageOptimizer.class);
        EmbeddedFontCleaner fontCleaner = mock(EmbeddedFontCleaner.class);
        when(optimizer.isEnabled()).thenReturn(true);
        when(fontCleaner.isEnabled()).thenReturn(true);

        ImageKeyPlan plan = new ImageKeyPlan(Map.of(
                PresentationKeys.PROBLEM_SOLVING_IMG,
                new ResolvedImageKey(
                        PresentationKeys.PROBLEM_SOLVING_IMG,
                        minimalJpeg(),
                        PictureData.PictureType.JPEG)));

        new ImageInserter(optimizer, fontCleaner).insert(input, plan, scan, output);

        verify(optimizer).optimize(any(XMLSlideShow.class));
        verify(fontCleaner).clean(output);
    }

    private void createTemplateWithImagePlaceholder(Path path) throws Exception {
        try (XMLSlideShow slideShow = new XMLSlideShow()) {
            XSLFSlideLayout layout = slideShow.getSlideMasters().get(0).getLayout(SlideLayout.TITLE_AND_CONTENT);
            XSLFSlide slide = slideShow.createSlide(layout);
            if (slide.getPlaceholder(Placeholder.BODY) instanceof XSLFTextShape body) {
                body.setText("${" + PresentationKeys.PROBLEM_SOLVING_IMG + "}");
            }
            try (OutputStream out = Files.newOutputStream(path)) {
                slideShow.write(out);
            }
        }
    }

    private byte[] minimalJpeg() throws Exception {
        return com.appfire.presentation.images.ImageTestSupport.createLargeJpeg();
    }
}
