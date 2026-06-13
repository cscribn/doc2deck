package com.appfire.presentation;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.poi.sl.usermodel.Placeholder;
import org.apache.poi.xslf.usermodel.SlideLayout;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFSlideLayout;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

public final class TestFixtureGenerator {

    private TestFixtureGenerator() {
    }

    public static void ensureFixtures() throws IOException {
        Path pptx = Path.of("src/test/resources/template.pptx");
        Path docx = Path.of("src/test/resources/source.docx");
        if (!Files.exists(pptx)) {
            createMinimalTemplatePptx(pptx);
        }
        if (!Files.exists(docx)) {
            createMinimalDocx(docx);
        }
    }

    private static void createMinimalTemplatePptx(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        try (XMLSlideShow slideShow = new XMLSlideShow()) {
            XSLFSlideLayout layout = slideShow.getSlideMasters().get(0).getLayout(SlideLayout.TITLE_AND_CONTENT);
            XSLFSlide slide = slideShow.createSlide(layout);
            if (slide.getPlaceholder(Placeholder.TITLE) instanceof XSLFTextShape title) {
                title.setText("${shortProjectDescription}");
            }
            if (slide.getPlaceholder(Placeholder.BODY) instanceof XSLFTextShape body) {
                body.setText("${problem1}\n${problem2}");
            }
            XSLFSlide imageSlide = slideShow.createSlide(layout);
            if (imageSlide.getPlaceholder(Placeholder.BODY) instanceof XSLFTextShape imageBody) {
                imageBody.setText("${problemSolvingImg}");
            }
            try (OutputStream out = Files.newOutputStream(path)) {
                slideShow.write(out);
            }
        }
    }

    private static void createMinimalDocx(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFParagraph heading = document.createParagraph();
            heading.setStyle("Heading1");
            XWPFRun headingRun = heading.createRun();
            headingRun.setText("Sample Heading");

            XWPFParagraph paragraph = document.createParagraph();
            XWPFRun paragraphRun = paragraph.createRun();
            paragraphRun.setText("Sample narrative paragraph for presentation generation.");

            try (OutputStream out = Files.newOutputStream(path)) {
                document.write(out);
            }
        }
    }
}
