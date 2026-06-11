package com.appfire.presentation.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.appfire.presentation.TestFixtures;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EmbeddedFontCleanerTest {

    @TempDir
    Path tempDir;

    @Test
    void removesUnusedEmbeddedFontFromDirectTemplateCopy() throws Exception {
        assumeTrue(TestFixtures.hasTemplatePptx(), "template.pptx not available");
        Path output = tempDir.resolve("direct-copy.pptx");
        Files.copy(TestFixtures.resolveTemplatePptx(), output);
        new EmbeddedFontCleaner(true).clean(output);

        String presentationXml = readZipEntry(output, "ppt/presentation.xml");
        assertFalse(
                presentationXml.contains("Arial Unicode MS"),
                "presentation.xml should drop unused embedded font metadata");
        assertDeckIntegrity(output);
        assertFalse(containsPart(output, "ppt/fonts/"), "unused fntdata parts should be removed from direct copy");
        assertFalse(readZipEntry(output, "ppt/presentation.xml").contains("Arial Unicode MS"));
    }

    @Test
    void removesUnusedEmbeddedFontFromTemplate() throws Exception {
        assumeTrue(TestFixtures.hasTemplatePptx(), "template.pptx not available");
        Path output = tempDir.resolve("cleaned.pptx");

        try (InputStream input = Files.newInputStream(TestFixtures.resolveTemplatePptx());
                XMLSlideShow slideShow = new XMLSlideShow(input);
                OutputStream out = Files.newOutputStream(output)) {
            slideShow.write(out);
        }
        new EmbeddedFontCleaner(true).clean(output);

        assertDeckIntegrity(output);
        assertFalse(containsPart(output, "ppt/fonts/"), "unused fntdata parts should be removed");
        String presentationXml = readZipEntry(output, "ppt/presentation.xml");
        assertFalse(
                presentationXml.contains("Arial Unicode MS"),
                "unused Arial Unicode MS embed should be removed from presentation.xml");
        try (ZipFile zip = new ZipFile(output.toFile())) {
            long relsCount = zip.stream()
                    .map(ZipEntry::getName)
                    .filter(name -> name.equals("ppt/_rels/presentation.xml.rels"))
                    .count();
            assertEquals(1, relsCount, "presentation.xml.rels should appear once in the zip");
        }
        String relsXml = readZipEntry(output, "ppt/_rels/presentation.xml.rels");
        assertFalse(relsXml.contains("Arial_Unicode_MS"), "font relationship should be removed from presentation.xml.rels");
        assertTrue(Files.size(output) < 15_000_000L, "removing 23MB font should materially reduce file size");
    }

    @Test
    void removeRelationshipPatternMatchesPoiGeneratedRels() {
        String relsXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Relationships"
                        + " xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                        + "<Relationship Id=\"rId13\" Target=\"fonts/Font_1_Arial_Unicode_MS_Regular.fntdata\""
                        + " Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/font\"/>"
                        + "</Relationships>";
        Pattern relationship = Pattern.compile("<Relationship\\s+[^>]*\\bId=\"" + Pattern.quote("rId13") + "\"[^>]*/>");
        String updated = relationship.matcher(relsXml).replaceAll("");
        assertFalse(updated.contains("Arial_Unicode_MS"));
    }

    @Test
    void collectsFontsUsedInTemplateSlides() throws Exception {
        assumeTrue(TestFixtures.hasTemplatePptx(), "template.pptx not available");

        try (InputStream input = Files.newInputStream(TestFixtures.resolveTemplatePptx());
                XMLSlideShow slideShow = new XMLSlideShow(input)) {
            var referenced = ReferencedFontCollector.collect(slideShow.getPackage());
            assertTrue(referenced.contains("Roboto Light"));
            assertTrue(referenced.contains("Open Sans Light"));
            assertFalse(referenced.contains("Arial Unicode MS"));
        }
    }

    private void assertDeckIntegrity(Path pptx) throws Exception {
        assertNotNull(
                readZipEntry(pptx, "ppt/_rels/presentation.xml.rels"),
                "presentation.xml.rels must be preserved for slide relationships");
        try (InputStream input = Files.newInputStream(pptx);
                XMLSlideShow slideShow = new XMLSlideShow(input)) {
            assertFalse(slideShow.getSlides().isEmpty(), "cleaned deck must still contain slides");
            String slideText = slideShow.getSlides().getFirst().getShapes().stream()
                    .filter(shape -> shape instanceof org.apache.poi.xslf.usermodel.XSLFTextShape)
                    .map(shape -> ((org.apache.poi.xslf.usermodel.XSLFTextShape) shape).getText())
                    .filter(text -> text != null && !text.isBlank())
                    .findFirst()
                    .orElse("");
            assertFalse(slideText.isBlank(), "first slide should retain visible text after font cleanup");
        }
    }

    private boolean containsPart(Path pptx, String prefix) throws Exception {
        try (ZipFile zip = new ZipFile(pptx.toFile())) {
            return zip.stream().map(ZipEntry::getName).anyMatch(name -> name.startsWith(prefix));
        }
    }

    private String readZipEntry(Path pptx, String entryName) throws Exception {
        try (ZipFile zip = new ZipFile(pptx.toFile());
                InputStream input = zip.getInputStream(zip.getEntry(entryName))) {
            return new String(input.readAllBytes());
        }
    }
}
