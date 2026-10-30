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
                        null,
                        null),
                0,
                null);
        assertTrue(html.contains("&lt;unsafe&gt;"));
        assertTrue(html.contains("a &amp; b"));
        assertFalse(html.contains("<unsafe>"));
        assertTrue(html.contains("slide-safe"));
    }

    @Test
    void journeyLayoutRendersRoadAndNodes() {
        String html = renderer.renderSlide(
                journeySlide(),
                1,
                null);
        assertTrue(html.contains("journey-road"));
        assertTrue(html.contains("journey-node"));
        assertTrue(html.contains("layout-journey"));
    }

    @Test
    void docLayoutRendersPanel() {
        String html = renderer.renderSlide(
                new HtmlDeckSlide(
                        "doc",
                        1,
                        "Doc title",
                        "K",
                        null,
                        null,
                        List.of(
                                new HtmlDeckBullet("pen", "Row one"),
                                new HtmlDeckBullet("check", "Row two")),
                        null,
                        List.of("note"),
                        null,
                        null),
                2,
                null);
        assertTrue(html.contains("doc-panel"));
        assertTrue(html.contains("layout-doc"));
    }

    @Test
    void cardsLayoutRendersGrid() {
        String html = renderer.renderSlide(
                new HtmlDeckSlide(
                        "cards",
                        2,
                        "Cards title",
                        null,
                        null,
                        null,
                        List.of(
                                new HtmlDeckBullet("star", "Card A"),
                                new HtmlDeckBullet("flag", "Card B")),
                        null,
                        List.of("note"),
                        null,
                        null),
                3,
                null);
        assertTrue(html.contains("s17-card"));
        assertTrue(html.contains("cards-grid"));
    }

    @Test
    void listLayoutRendersRows() {
        String html = renderer.renderSlide(
                new HtmlDeckSlide(
                        "list",
                        2,
                        "List title",
                        null,
                        null,
                        null,
                        List.of(
                                new HtmlDeckBullet("check", "Item one"),
                                new HtmlDeckBullet("target", "Item two")),
                        null,
                        List.of("note"),
                        null,
                        null),
                4,
                null);
        assertTrue(html.contains("list-rows"));
        assertTrue(html.contains("s6-row"));
    }

    private static HtmlDeckSlide journeySlide() {
        return new HtmlDeckSlide(
                "journey",
                0,
                "Journey title",
                "OVERVIEW",
                null,
                null,
                List.of(
                        new HtmlDeckBullet("pen", "Step one"),
                        new HtmlDeckBullet("brief", "Step two"),
                        new HtmlDeckBullet("users", "Step three")),
                null,
                List.of("note"),
                "Coach",
                null);
    }

    @Test
    void splitLayoutUsesTwoColumnsAndImageSideLeft() {
        String html = renderer.renderSlide(
                new HtmlDeckSlide(
                        "split",
                        1,
                        "Split title",
                        "VISUAL",
                        null,
                        null,
                        List.of(
                                new HtmlDeckBullet("chart", "Metric one"),
                                new HtmlDeckBullet("trend", "Metric two")),
                        "office teamwork",
                        List.of("note"),
                        null,
                        "left"),
                0,
                "data:image/png;base64,abc");
        assertTrue(html.contains("split-cols"));
        assertTrue(html.contains("split-media"));
        assertTrue(html.contains("split-media-left"));
        assertTrue(html.contains("split-bullets"));
    }
}
