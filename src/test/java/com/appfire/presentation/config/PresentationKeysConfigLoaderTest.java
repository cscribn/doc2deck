package com.appfire.presentation.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.appfire.presentation.model.KeyType;
import com.appfire.presentation.model.TemplateScanResult;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PresentationKeysConfigLoaderTest {

    @Test
    void loadsExampleProperties() {
        PresentationKeysConfig config = PresentationKeysConfigLoader.load(
                Path.of("presentation-keys.example.properties"));

        assertTrue(config.header().contains("PRESENTATION KEYS"));
        assertTrue(config.instructions().containsKey("problem1"));
        assertTrue(config.formatForPrompt(Set.of("problem1")).contains("problem1 - First problem beat"));
        assertEquals(15, config.definitionFor("problem1").maxWords());
        assertEquals(KeyType.IMAGE, config.definitionFor("problemSolvingImg").type());
    }

    @Test
    void loadsTestFixture() {
        PresentationKeysConfig config = PresentationKeysConfigLoader.load(
                Path.of("src/test/resources/presentation-keys.properties"));

        assertEquals("PRESENTATION KEYS (test header)", config.header());
        assertEquals("Problem one test instruction.", config.instructions().get("problem1"));
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
    void rejectsBlankInstruction() {
        Properties properties = baseProperties();
        properties.setProperty("problem1", "   ");

        IllegalStateException error = assertThrows(
                IllegalStateException.class, () -> PresentationKeysConfigLoader.parse(properties));

        assertTrue(error.getMessage().contains("problem1"));
    }

    @Test
    void allowsOptionalNonDevCostsToBeAbsent() {
        Properties properties = baseProperties();
        properties.remove("nonDevCosts");
        properties.remove("nonDevCosts.maxWords");
        properties.remove("nonDevCosts.optional");

        PresentationKeysConfig config = PresentationKeysConfigLoader.parse(properties);

        assertTrue(config.instructions().containsKey("problem1"));
        assertTrue(!config.formatForPrompt(Set.of("nonDevCosts")).contains("nonDevCosts -"));
    }

    @Test
    void rejectsUnknownMetadataSuffix() {
        Properties properties = baseProperties();
        properties.setProperty("problem1.unknown", "value");

        IllegalStateException error = assertThrows(
                IllegalStateException.class, () -> PresentationKeysConfigLoader.parse(properties));

        assertTrue(error.getMessage().contains("problem1.unknown"));
    }

    @Test
    void validateAgainstTemplateFailsWhenTemplateKeyMissingFromConfig() {
        PresentationKeysConfig config = PresentationKeysConfigLoader.parse(baseProperties());
        TemplateScanResult scan = new TemplateScanResult(Set.of("missingKey"), java.util.List.of(), java.util.List.of());

        IllegalStateException error = assertThrows(
                IllegalStateException.class, () -> config.validateAgainstTemplate(scan));

        assertTrue(error.getMessage().contains("missingKey"));
    }

    private static Properties baseProperties() {
        PresentationKeysConfig loaded = PresentationKeysConfigLoader.load(
                Path.of("src/test/resources/presentation-keys.properties"));
        Properties properties = new Properties();
        properties.setProperty(PresentationKeysConfig.HEADER_KEY, loaded.header());
        loaded.instructions().forEach(properties::setProperty);
        loaded.keys().forEach((name, definition) -> {
            properties.setProperty(name + ".maxWords", String.valueOf(definition.maxWords()));
            if (definition.optional()) {
                properties.setProperty(name + ".optional", "true");
            }
            if (definition.isImage()) {
                properties.setProperty(name + ".type", "image");
            }
        });
        return properties;
    }
}
