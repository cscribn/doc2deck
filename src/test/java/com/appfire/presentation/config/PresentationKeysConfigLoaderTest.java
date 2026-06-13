package com.appfire.presentation.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.appfire.presentation.model.PresentationKeys;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class PresentationKeysConfigLoaderTest {

    @Test
    void loadsExampleProperties() {
        PresentationKeysConfig config = PresentationKeysConfigLoader.load(
                Path.of("presentation-keys.example.properties"));

        assertTrue(config.header().contains("PRESENTATION KEYS"));
        assertTrue(config.instructions().containsKey(PresentationKeys.PROBLEM_1));
        assertTrue(config.formatForPrompt().contains("problem1 - First problem beat"));
    }

    @Test
    void loadsTestFixture() {
        PresentationKeysConfig config = PresentationKeysConfigLoader.load(
                Path.of("src/test/resources/presentation-keys.properties"));

        assertEquals("PRESENTATION KEYS (test header)", config.header());
        assertEquals("Problem one test instruction.", config.instructions().get(PresentationKeys.PROBLEM_1));
    }

    @Test
    void rejectsMissingHeader() {
        Properties properties = baseProperties();
        properties.remove(PresentationKeysConfig.HEADER_KEY);

        IllegalStateException error = assertThrows(
                IllegalStateException.class, () -> PresentationKeysConfigLoader.parse(properties));

        assertTrue(error.getMessage().contains("presentationKeys.header"));
    }

    @Test
    void rejectsMissingRequiredKey() {
        Properties properties = baseProperties();
        properties.remove(PresentationKeys.PROBLEM_1);

        IllegalStateException error = assertThrows(
                IllegalStateException.class, () -> PresentationKeysConfigLoader.parse(properties));

        assertTrue(error.getMessage().contains("problem1"));
    }

    @Test
    void rejectsUnknownProperty() {
        Properties properties = baseProperties();
        properties.setProperty("unknownKey", "bad");

        IllegalStateException error = assertThrows(
                IllegalStateException.class, () -> PresentationKeysConfigLoader.parse(properties));

        assertTrue(error.getMessage().contains("unknownKey"));
    }

    @Test
    void rejectsBlankInstruction() {
        Properties properties = baseProperties();
        properties.setProperty(PresentationKeys.PROBLEM_1, "   ");

        IllegalStateException error = assertThrows(
                IllegalStateException.class, () -> PresentationKeysConfigLoader.parse(properties));

        assertTrue(error.getMessage().contains("problem1"));
    }

    @Test
    void allowsOptionalNonDevCostsToBeAbsent() {
        Properties properties = baseProperties();
        properties.remove(PresentationKeys.NON_DEV_COSTS);

        PresentationKeysConfig config = PresentationKeysConfigLoader.parse(properties);

        assertTrue(config.instructions().containsKey(PresentationKeys.PROBLEM_1));
        assertTrue(!config.formatForPrompt().contains("nonDevCosts -"));
    }

    private static Properties baseProperties() {
        PresentationKeysConfig loaded = PresentationKeysConfigLoader.load(
                Path.of("src/test/resources/presentation-keys.properties"));
        Properties properties = new Properties();
        properties.setProperty(PresentationKeysConfig.HEADER_KEY, loaded.header());
        loaded.instructions().forEach(properties::setProperty);
        return properties;
    }
}
