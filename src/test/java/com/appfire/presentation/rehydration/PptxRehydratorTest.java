package com.appfire.presentation.rehydration;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.appfire.presentation.TestFixtures;
import com.appfire.presentation.TestFixtureExtension;
import com.appfire.presentation.extraction.PptxExtractor;
import com.appfire.presentation.model.ContentStyle;
import com.appfire.presentation.model.ExtractedPresentation;
import com.appfire.presentation.model.GenerationResponse;
import com.appfire.presentation.model.ImagePlan;
import com.appfire.presentation.model.SlideAction;
import com.appfire.presentation.model.SlideDirective;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

@ExtendWith(TestFixtureExtension.class)
class PptxRehydratorTest {

    @Test
    void writesOutputFromValidResponse(@TempDir Path tempDir) throws Exception {
        assumeTrue(TestFixtures.hasSourcePptx(), "source.pptx not available");

        PptxExtractor extractor = new PptxExtractor();
        ExtractedPresentation extracted = extractor.extract(TestFixtures.resolveSourcePptx());
        LayoutResolver layoutResolver = new LayoutResolver(extracted.blueprint().layoutCatalog());

        SlideDirective directive = new SlideDirective(
                0, SlideAction.REPLACE, "Rehydrated Title",
                List.of("Bullet A", "  • Sub bullet"), null, null, List.of(0),
                null, null, null, ContentStyle.BULLETS, false, null,
                List.of(), List.of(), false, com.appfire.presentation.model.ImagePosition.NONE);
        GenerationResponse response = new GenerationResponse(List.of(directive), List.of());

        Path output = tempDir.resolve("out.pptx");
        new PptxRehydrator(layoutResolver).rehydrate(extracted, response, new ImagePlan(java.util.Map.of()), output);
        assertTrue(Files.exists(output));
        assertTrue(Files.size(output) > 0);
    }

    @Test
    void writesTwoColumnContent(@TempDir Path tempDir) throws Exception {
        assumeTrue(TestFixtures.hasSourcePptx(), "source.pptx not available");

        PptxExtractor extractor = new PptxExtractor();
        ExtractedPresentation extracted = extractor.extract(TestFixtures.resolveSourcePptx());
        LayoutResolver layoutResolver = new LayoutResolver(extracted.blueprint().layoutCatalog());

        SlideDirective directive = new SlideDirective(
                0, SlideAction.REPLACE, "Two Column Slide",
                List.of(), null, null, List.of(0),
                null, null, null, ContentStyle.TWO_COLUMN, false, null,
                List.of("Left A"), List.of("Right B"), false,
                com.appfire.presentation.model.ImagePosition.NONE);
        GenerationResponse response = new GenerationResponse(List.of(directive), List.of());

        Path output = tempDir.resolve("two-col.pptx");
        new PptxRehydrator(layoutResolver).rehydrate(extracted, response, new ImagePlan(java.util.Map.of()), output);
        assertTrue(Files.exists(output));
    }
}
