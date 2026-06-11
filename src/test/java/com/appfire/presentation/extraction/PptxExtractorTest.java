package com.appfire.presentation.extraction;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.appfire.presentation.TestFixtures;
import com.appfire.presentation.TestFixtureExtension;
import com.appfire.presentation.model.ExtractedPresentation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TestFixtureExtension.class)
class PptxExtractorTest {

    @Test
    void extractsSlidesFromSourcePptx() throws Exception {
        assumeTrue(TestFixtures.hasSourcePptx(), "source.pptx not available");
        PptxExtractor extractor = new PptxExtractor();
        ExtractedPresentation result = extractor.extract(TestFixtures.resolveSourcePptx());
        assertFalse(result.blueprint().slides().isEmpty());
    }

    @Test
    void extractsLayoutCatalog() throws Exception {
        assumeTrue(TestFixtures.hasSourcePptx(), "source.pptx not available");
        PptxExtractor extractor = new PptxExtractor();
        ExtractedPresentation result = extractor.extract(TestFixtures.resolveSourcePptx());
        assertFalse(result.blueprint().layoutCatalog().layouts().isEmpty());
    }
}
