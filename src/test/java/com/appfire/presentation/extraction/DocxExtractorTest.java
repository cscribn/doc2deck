package com.appfire.presentation.extraction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.appfire.presentation.TestFixtures;
import com.appfire.presentation.model.BlockType;
import com.appfire.presentation.model.ContentBlock;
import com.appfire.presentation.model.DocumentContent;
import com.appfire.presentation.TestFixtureExtension;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

@ExtendWith(TestFixtureExtension.class)
class DocxExtractorTest {

    @Test
    void extractsBlocksFromSourceDocx() throws Exception {
        assumeTrue(TestFixtures.hasSourceDocx(), "source.docx not available");
        DocxExtractor extractor = new DocxExtractor();
        DocumentContent result = extractor.extract(TestFixtures.resolveSourceDocx());
        assertFalse(result.blocks().isEmpty());
    }

    @Test
    void extractAllMergesMultipleDocx(@TempDir Path tempDir) throws Exception {
        Path first = tempDir.resolve("alpha.docx");
        Path second = tempDir.resolve("beta.docx");
        createMinimalDocx(first, "Alpha heading", "Alpha paragraph");
        createMinimalDocx(second, "Beta heading", "Beta paragraph");

        DocxExtractor extractor = new DocxExtractor();
        DocumentContent result = extractor.extractAll(List.of(first, second));

        assertFalse(result.blocks().isEmpty());
        assertFalse(result.flatSummary().isBlank());
        assertEquals(6, result.blocks().size());

        ContentBlock firstDelimiter = result.blocks().get(0);
        assertEquals(BlockType.HEADING, firstDelimiter.type());
        assertEquals("Source: alpha.docx", firstDelimiter.text());

        ContentBlock secondDelimiter = result.blocks().get(3);
        assertEquals(BlockType.HEADING, secondDelimiter.type());
        assertEquals("Source: beta.docx", secondDelimiter.text());

        assertTrue(result.flatSummary().contains("--- alpha.docx ---"));
        assertTrue(result.flatSummary().contains("--- beta.docx ---"));

        for (int i = 0; i < result.blocks().size(); i++) {
            assertEquals(i, result.blocks().get(i).index());
        }
    }

    private static void createMinimalDocx(Path path, String heading, String paragraph) throws IOException {
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFParagraph headingParagraph = document.createParagraph();
            headingParagraph.setStyle("Heading1");
            XWPFRun headingRun = headingParagraph.createRun();
            headingRun.setText(heading);

            XWPFParagraph bodyParagraph = document.createParagraph();
            XWPFRun bodyRun = bodyParagraph.createRun();
            bodyRun.setText(paragraph);

            Files.createDirectories(path.getParent());
            try (OutputStream out = Files.newOutputStream(path)) {
                document.write(out);
            }
        }
    }
}
