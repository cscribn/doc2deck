package com.appfire.presentation.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Rectangle;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.apache.poi.xslf.usermodel.SlideLayout;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlideMaster;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.junit.jupiter.api.Test;

class TemplateDesignLinterTest {

    @Test
    void noWarningsForMinimalDeck() throws Exception {
        try (XMLSlideShow slideShow = new XMLSlideShow()) {
            addTextSlide(slideShow, "Inter", Color.BLACK);
            assertEquals(List.of(), lintSaved(slideShow));
        }
    }

    @Test
    void warnsOnTooManyFontFamilies() throws Exception {
        try (XMLSlideShow slideShow = new XMLSlideShow()) {
            for (String font : List.of("Inter", "Open Sans", "Work Sans", "Bitter")) {
                addTextSlide(slideShow, font, Color.BLACK);
            }
            assertTrue(hasWarning(lintSaved(slideShow), "font families"));
        }
    }

    @Test
    void ignoresGreysWhenCountingColors() throws Exception {
        try (XMLSlideShow slideShow = new XMLSlideShow()) {
            for (Color grey : List.of(Color.BLACK, Color.WHITE, Color.GRAY, Color.DARK_GRAY, Color.LIGHT_GRAY,
                    new Color(17, 17, 17), new Color(238, 238, 238))) {
                addTextSlide(slideShow, "Inter", grey);
            }
            assertEquals(List.of(), lintSaved(slideShow));
        }
    }

    @Test
    void warnsOnTooManyNonGreyColors() throws Exception {
        try (XMLSlideShow slideShow = new XMLSlideShow()) {
            for (int i = 0; i < 7; i++) {
                addTextSlide(slideShow, "Inter", new Color(200, 10 * i, 50));
            }
            assertTrue(hasWarning(lintSaved(slideShow), "non-grey RGB colors"));
        }
    }

    @Test
    void warnsOnTooManyUsedLayouts() throws Exception {
        try (XMLSlideShow slideShow = new XMLSlideShow()) {
            XSLFSlideMaster master = slideShow.getSlideMasters().getFirst();
            for (SlideLayout type : List.of(
                    SlideLayout.TITLE, SlideLayout.BLANK, SlideLayout.TITLE_ONLY, SlideLayout.TWO_OBJ)) {
                slideShow.createSlide(master.getLayout(type));
            }
            assertTrue(hasWarning(lintSaved(slideShow), "layouts"));
        }
    }

    private static void addTextSlide(XMLSlideShow slideShow, String font, Color color) {
        XSLFTextBox box = slideShow.createSlide().createTextBox();
        box.setAnchor(new Rectangle(50, 50, 400, 100));
        XSLFTextRun run = box.addNewTextParagraph().addNewTextRun();
        run.setText("Sample");
        run.setFontFamily(font);
        run.setFontColor(color);
    }

    private static List<String> lintSaved(XMLSlideShow slideShow) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        slideShow.write(out);
        try (XMLSlideShow saved = new XMLSlideShow(new ByteArrayInputStream(out.toByteArray()))) {
            return TemplateDesignLinter.lint(saved);
        }
    }

    private static boolean hasWarning(List<String> warnings, String fragment) {
        return warnings.stream().anyMatch(msg -> msg.contains(fragment));
    }
}
