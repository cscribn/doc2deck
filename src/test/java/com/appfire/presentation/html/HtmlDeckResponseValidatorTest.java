package com.appfire.presentation.html;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.appfire.presentation.config.HtmlDeckOutlineConfig;
import com.appfire.presentation.config.HtmlDeckOutlineConfigLoader;
import com.appfire.presentation.llm.HtmlDeckResponseValidator;
import com.appfire.presentation.model.HtmlDeckBullet;
import com.appfire.presentation.model.HtmlDeckContentResponse;
import com.appfire.presentation.model.HtmlDeckSlide;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HtmlDeckResponseValidatorTest {

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
    void acceptsValidMinimalDeck() {
        HtmlDeckResponseValidator validator = new HtmlDeckResponseValidator(outline);
        HtmlDeckContentResponse response = sampleDeck();
        assertTrue(validator.validate(response).passed());
    }

    @Test
    void rejectsTooFewSlides() {
        HtmlDeckResponseValidator validator = new HtmlDeckResponseValidator(outline);
        HtmlDeckContentResponse response = new HtmlDeckContentResponse(
                "Hook title here",
                List.of(
                        heroSlide(),
                        closingSlide()),
                null,
                null);
        assertFalse(validator.validate(response).passed());
    }

    @Test
    void rejectsNonHeroFirstSlide() {
        HtmlDeckResponseValidator validator = new HtmlDeckResponseValidator(outline);
        List<HtmlDeckSlide> slides = List.of(
                standardSlide(0),
                standardSlide(1),
                standardSlide(1),
                standardSlide(2),
                standardSlide(2),
                standardSlide(3),
                standardSlide(3),
                closingSlide());
        HtmlDeckContentResponse response = new HtmlDeckContentResponse("Hook title here", slides, null, null);
        assertFalse(validator.validate(response).passed());
    }

    private HtmlDeckContentResponse sampleDeck() {
        return new HtmlDeckContentResponse(
                "Hook title here",
                List.of(
                        heroSlide(),
                        standardSlide(1),
                        standardSlide(2),
                        splitSlide(3),
                        standardSlide(2),
                        standardSlide(3),
                        standardSlide(3),
                        closingSlide()),
                null,
                null);
    }

    private HtmlDeckSlide heroSlide() {
        return new HtmlDeckSlide(
                "hero",
                0,
                "Opening beat",
                "INTRO",
                "Main headline",
                "First beat; second beat",
                List.of(new HtmlDeckBullet("pen", "One fragment here")),
                null,
                List.of("Speaker context for the opening."),
                null);
    }

    private HtmlDeckSlide standardSlide(int milestone) {
        return new HtmlDeckSlide(
                "standard",
                milestone,
                "Slide title fragment",
                "SECTION",
                null,
                null,
                List.of(
                        new HtmlDeckBullet("check", "First point here"),
                        new HtmlDeckBullet("target", "Second point here")),
                null,
                List.of("Expand on this beat verbally."),
                null);
    }

    private HtmlDeckSlide splitSlide(int milestone) {
        return new HtmlDeckSlide(
                "split",
                milestone,
                "Split slide title",
                "PROOF",
                null,
                null,
                List.of(
                        new HtmlDeckBullet("chart", "Metric beat one"),
                        new HtmlDeckBullet("trend", "Metric beat two")),
                "team collaboration office",
                List.of("Walk through the visual."),
                null);
    }

    private HtmlDeckSlide closingSlide() {
        return new HtmlDeckSlide(
                "closing",
                4,
                "Thank you",
                null,
                null,
                "Questions welcome",
                List.of(
                        new HtmlDeckBullet("star", "Takeaway one here"),
                        new HtmlDeckBullet("flag", "Takeaway two here")),
                null,
                List.of("Close with next steps."),
                null);
    }
}
