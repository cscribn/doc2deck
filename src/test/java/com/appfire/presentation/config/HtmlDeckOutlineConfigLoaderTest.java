package com.appfire.presentation.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class HtmlDeckOutlineConfigLoaderTest {

    @Test
    void parsesExampleProperties() throws IOException {
        Properties properties = new Properties();
        try (var in = Files.newInputStream(Path.of("html-deck-outline.example.properties"))) {
            properties.load(in);
        }
        HtmlDeckOutlineConfig config = HtmlDeckOutlineConfigLoader.parse(properties);

        assertEquals(5, config.milestoneLabels().size());
        assertEquals("Intro", config.milestoneLabels().get(0));
        assertEquals(8, config.slidesMin());
        assertEquals(14, config.slidesMax());
        assertEquals(12, config.maxWordsFor("deckTitle"));
        assertFalse(config.layoutDescriptions().isEmpty());
        assertTrue(config.milestonesJsonForRuntime().contains("Intro"));
    }

    @Test
    void rejectsMissingHeader() {
        Properties properties = new Properties();
        properties.setProperty("milestones", "0:Start");
        properties.setProperty("slides.min", "1");
        properties.setProperty("slides.max", "5");
        properties.setProperty("layout.hero", "desc");
        properties.setProperty("layout.standard", "desc");
        properties.setProperty("layout.split", "desc");
        properties.setProperty("layout.closing", "desc");

        assertThrows(IllegalStateException.class, () -> HtmlDeckOutlineConfigLoader.parse(properties));
    }
}
