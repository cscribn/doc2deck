package com.appfire.presentation.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.appfire.presentation.TestFixtures;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PptxZipEntryReplacerTest {

    @TempDir
    Path tempDir;

    @Test
    void replacesPresentationRelationshipsOnPoiWrittenDeck() throws Exception {
        assumeTrue(TestFixtures.hasTemplatePptx(), "template.pptx not available");
        Path output = tempDir.resolve("poi-written.pptx");
        try (InputStream input = Files.newInputStream(TestFixtures.resolveTemplatePptx());
                XMLSlideShow slideShow = new XMLSlideShow(input);
                OutputStream out = Files.newOutputStream(output)) {
            slideShow.write(out);
        }

        String relsXml = readZipEntry(output, "ppt/_rels/presentation.xml.rels");
        String updated = relsXml.replace("Font_1_Arial_Unicode_MS_Regular.fntdata", "removed-font.fntdata");
        PptxZipEntryReplacer.replaceEntry(
                output, "ppt/_rels/presentation.xml.rels", updated.getBytes(StandardCharsets.UTF_8));

        assertFalse(readZipEntry(output, "ppt/_rels/presentation.xml.rels").contains("Arial_Unicode_MS"));
    }

    @Test
    void replacesPresentationRelationshipsEntry() throws Exception {
        assumeTrue(TestFixtures.hasTemplatePptx(), "template.pptx not available");
        Path output = tempDir.resolve("updated.pptx");
        Files.copy(TestFixtures.resolveTemplatePptx(), output);

        String relsXml = readZipEntry(output, "ppt/_rels/presentation.xml.rels");
        String updated = relsXml.replace("rId13", "rId13-removed-test-marker");
        PptxZipEntryReplacer.replaceEntry(
                output, "ppt/_rels/presentation.xml.rels", updated.getBytes(StandardCharsets.UTF_8));

        assertEquals(updated, readZipEntry(output, "ppt/_rels/presentation.xml.rels"));
        assertFalse(readZipEntry(output, "ppt/presentation.xml").contains("rId13-removed-test-marker"));
    }

    private String readZipEntry(Path pptx, String entryName) throws Exception {
        try (ZipFile zip = new ZipFile(pptx.toFile());
                InputStream input = zip.getInputStream(zip.getEntry(entryName))) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
