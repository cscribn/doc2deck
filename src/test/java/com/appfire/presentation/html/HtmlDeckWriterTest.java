package com.appfire.presentation.html;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.appfire.presentation.config.HtmlDeckOutlineConfig;
import com.appfire.presentation.config.HtmlDeckOutlineConfigLoader;
import com.appfire.presentation.model.HtmlDeckBullet;
import com.appfire.presentation.model.HtmlDeckContentResponse;
import com.appfire.presentation.model.HtmlDeckSlide;
import com.appfire.presentation.model.ImageKeyPlan;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HtmlDeckWriterTest {

    private HtmlDeckOutlineConfig outline;

    @BeforeEach
    void loadOutline() throws IOException {
        Properties properties = new Properties();
        try (var in = Files.newInputStream(Path.of("html-deck-outline.example.properties"))) {
            properties.load(in);
        }
        outline = HtmlDeckOutlineConfigLoader.parse(properties);
    }

    @Test
    void writesSelfContainedHtml(@TempDir Path tempDir) throws IOException {
        HtmlDeckWriter writer = new HtmlDeckWriter(new HtmlSlideRenderer());
        HtmlDeckContentResponse response = new HtmlDeckContentResponse(
                "Test deck",
                List.of(
                        new HtmlDeckSlide(
                                "hero",
                                0,
                                "Open",
                                "KICK",
                                "Headline",
                                "Beat one; beat two",
                                List.of(new HtmlDeckBullet("pen", "Chip text")),
                                null,
                                List.of("Note one"),
                                null,
                                null),
                        new HtmlDeckSlide(
                                "closing",
                                4,
                                "Close",
                                null,
                                null,
                                "Thanks",
                                List.of(
                                        new HtmlDeckBullet("star", "Take one"),
                                        new HtmlDeckBullet("flag", "Take two")),
                                null,
                                List.of("Final note"),
                                null,
                                null)),
                null,
                null);

        Path out = tempDir.resolve("deck.html");
        writer.write(out, response, outline, new ImageKeyPlan(null));

        String html = Files.readString(out);
        assertTrue(html.contains("<div id=\"track\">"));
        assertTrue(html.contains("window.__DECK_MS__="));
        assertTrue(html.contains("&lt;script&gt;") || !html.contains("<script>alert"));
        assertFalse(html.contains("{{DECK_TITLE}}"));
    }
}
