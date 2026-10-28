package com.appfire.presentation.html;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.appfire.presentation.model.HtmlDeckBullet;
import com.appfire.presentation.model.HtmlDeckSlide;
import java.util.List;
import org.junit.jupiter.api.Test;

class HtmlSlideRendererTest {

    private final HtmlSlideRenderer renderer = new HtmlSlideRenderer();

    @Test
    void escapesUserContent() {
        String html = renderer.renderSlide(
                new HtmlDeckSlide(
                        "standard",
                        1,
                        "<unsafe>",
                        "K",
                        null,
                        null,
                        List.of(new HtmlDeckBullet("pen", "a & b")),
                        null,
                        List.of("note"),
                        null),
                0,
                null);
        assertTrue(html.contains("&lt;unsafe&gt;"));
        assertTrue(html.contains("a &amp; b"));
        assertFalse(html.contains("<unsafe>"));
    }
}
