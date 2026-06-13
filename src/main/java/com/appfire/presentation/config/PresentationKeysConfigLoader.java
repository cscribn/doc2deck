package com.appfire.presentation.config;

import com.appfire.presentation.model.PresentationKeys;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

public final class PresentationKeysConfigLoader {

    private PresentationKeysConfigLoader() {
    }

    public static PresentationKeysConfig load(Path path) {
        if (!Files.exists(path) || !Files.isReadable(path)) {
            throw new IllegalStateException(
                    "Presentation keys config not found or unreadable: " + path.toAbsolutePath()
                            + ". Copy presentation-keys.example.properties to presentation-keys.properties and re-run ./gradlew run");
        }

        Properties properties = new Properties();
        try (Reader reader = new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to read presentation keys config: " + path.toAbsolutePath()
                            + ". Check file permissions and re-run ./gradlew run",
                    e);
        }

        return parse(properties);
    }

    static PresentationKeysConfig parse(Properties properties) {
        String header = properties.getProperty(PresentationKeysConfig.HEADER_KEY, "").trim();
        if (header.isBlank()) {
            throw new IllegalStateException(
                    "Presentation keys config missing required property: "
                            + PresentationKeysConfig.HEADER_KEY
                            + ". Set it in presentation-keys.properties and re-run ./gradlew run");
        }

        Set<String> allowedKeys = PresentationKeys.textKeys().stream()
                .collect(Collectors.toSet());
        Map<String, String> instructions = new LinkedHashMap<>();

        for (String propertyName : properties.stringPropertyNames()) {
            if (PresentationKeysConfig.HEADER_KEY.equals(propertyName)) {
                continue;
            }
            if (!allowedKeys.contains(propertyName)) {
                throw new IllegalStateException(
                        "Unknown presentation key in config: " + propertyName
                                + ". Use known key names from presentation-keys.example.properties and re-run ./gradlew run");
            }
            String instruction = properties.getProperty(propertyName, "").trim();
            if (instruction.isBlank()) {
                throw new IllegalStateException(
                        "Presentation key instruction is blank: " + propertyName
                                + ". Provide instruction text in presentation-keys.properties and re-run ./gradlew run");
            }
            instructions.put(propertyName, instruction);
        }

        for (String requiredKey : PresentationKeys.requiredTextKeys()) {
            if (!instructions.containsKey(requiredKey)) {
                throw new IllegalStateException(
                        "Presentation keys config missing required key: " + requiredKey
                                + ". Copy presentation-keys.example.properties to presentation-keys.properties and re-run ./gradlew run");
            }
        }

        return new PresentationKeysConfig(header, instructions);
    }
}
