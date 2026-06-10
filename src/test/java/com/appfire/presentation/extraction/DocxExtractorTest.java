package com.appfire.presentation.extraction;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.appfire.presentation.TestFixtures;
import com.appfire.presentation.model.DocumentContent;
import com.appfire.presentation.TestFixtureExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TestFixtureExtension.class)
class DocxExtractorTest {

    @Test
    void extractsBlocksFromSourceDocx() throws Exception {
        assumeTrue(TestFixtures.hasSourceDocx(), "source.docx not available");
        DocxExtractor extractor = new DocxExtractor();
        DocumentContent result = extractor.extract(TestFixtures.resolveSourceDocx());
        assertFalse(result.blocks().isEmpty());
    }
}
