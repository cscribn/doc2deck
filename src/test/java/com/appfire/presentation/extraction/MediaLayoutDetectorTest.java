package com.appfire.presentation.extraction;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.appfire.presentation.TestFixtures;
import com.appfire.presentation.model.ImagePosition;
import com.appfire.presentation.model.MediaLayoutSpec;
import org.junit.jupiter.api.Test;

class MediaLayoutDetectorTest {

    @Test
    void detectsSplitMediaLayoutsFromSourcePptx() throws Exception {
        assumeTrue(TestFixtures.hasSourcePptx());
        var extracted = new PptxExtractor().extract(TestFixtures.resolveSourcePptx());
        assertFalse(extracted.blueprint().layoutCatalog().mediaLayouts().isEmpty());
        assertTrue(extracted.blueprint().layoutCatalog().mediaLayouts().stream()
                .anyMatch(m -> m.imagePosition() == ImagePosition.RIGHT));
        assertTrue(extracted.blueprint().layoutCatalog().mediaLayouts().stream()
                .anyMatch(m -> m.imagePosition() == ImagePosition.LEFT));
    }

    @Test
    void mainPointLayoutHasNonOverlappingRegions() throws Exception {
        assumeTrue(TestFixtures.hasSourcePptx());
        var extracted = new PptxExtractor().extract(TestFixtures.resolveSourcePptx());
        MediaLayoutSpec spec = extracted.blueprint().layoutCatalog().mediaLayouts().stream()
                .filter(m -> m.layoutName().equals("MAIN_POINT_1")
                        && m.imagePosition() == ImagePosition.RIGHT)
                .findFirst()
                .orElse(null);
        assumeTrue(spec != null);
        assertTrue(spec.textX() + spec.textWidth() <= spec.mediaX() + 1);
    }
}
