package com.appfire.presentation.config;

import com.appfire.presentation.model.KeyDefinition;
import com.appfire.presentation.model.KeyType;
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

public final class PresentationKeysConfigLoader {

    private static final Set<String> METADATA_SUFFIXES = Set.of("maxWords", "optional", "type");

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

        Map<String, String> instructions = new LinkedHashMap<>();
        Map<String, Integer> maxWordsByKey = new LinkedHashMap<>();
        Map<String, Boolean> optionalByKey = new LinkedHashMap<>();
        Map<String, KeyType> typeByKey = new LinkedHashMap<>();

        for (String propertyName : properties.stringPropertyNames()) {
            if (PresentationKeysConfig.HEADER_KEY.equals(propertyName)) {
                continue;
            }
            if (propertyName.contains(".")) {
                parseMetadata(propertyName, properties.getProperty(propertyName), maxWordsByKey, optionalByKey, typeByKey);
                continue;
            }
            String instruction = properties.getProperty(propertyName, "").trim();
            if (instruction.isBlank()) {
                throw new IllegalStateException(
                        "Presentation key instruction is blank: " + propertyName
                                + ". Provide instruction text in presentation-keys.properties and re-run ./gradlew run");
            }
            instructions.put(propertyName, instruction);
        }

        if (instructions.isEmpty()) {
            throw new IllegalStateException(
                    "Presentation keys config has no key definitions. "
                            + "Add key instructions to presentation-keys.properties and re-run ./gradlew run");
        }

        Map<String, KeyDefinition> keys = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : instructions.entrySet()) {
            String keyName = entry.getKey();
            KeyType type = typeByKey.getOrDefault(keyName, KeyType.TEXT);
            boolean optional = optionalByKey.getOrDefault(keyName, false);
            int maxWords = maxWordsByKey.getOrDefault(
                    keyName,
                    type == KeyType.IMAGE ? PresentationKeysConfig.DEFAULT_IMAGE_MAX_WORDS
                            : PresentationKeysConfig.DEFAULT_MAX_WORDS);
            keys.put(keyName, new KeyDefinition(keyName, entry.getValue(), maxWords, optional, type));
        }

        return new PresentationKeysConfig(header, keys);
    }

    private static void parseMetadata(
            String propertyName,
            String rawValue,
            Map<String, Integer> maxWordsByKey,
            Map<String, Boolean> optionalByKey,
            Map<String, KeyType> typeByKey) {
        int dot = propertyName.indexOf('.');
        String keyName = propertyName.substring(0, dot);
        String suffix = propertyName.substring(dot + 1);
        if (!METADATA_SUFFIXES.contains(suffix)) {
            throw new IllegalStateException(
                    "Unknown presentation key metadata property: " + propertyName
                            + ". Use maxWords, optional, or type suffixes and re-run ./gradlew run");
        }
        String value = rawValue == null ? "" : rawValue.trim();
        switch (suffix) {
            case "maxWords" -> maxWordsByKey.put(keyName, parsePositiveInt(keyName, suffix, value));
            case "optional" -> optionalByKey.put(keyName, parseBoolean(keyName, value));
            case "type" -> typeByKey.put(keyName, parseKeyType(keyName, value));
            default -> throw new IllegalStateException("Unsupported metadata suffix: " + suffix);
        }
    }

    private static int parsePositiveInt(String keyName, String suffix, String value) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed <= 0) {
                throw new NumberFormatException("non-positive");
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalStateException(
                    "Invalid " + keyName + "." + suffix + " value: " + value
                            + ". Set a positive integer in presentation-keys.properties and re-run ./gradlew run");
        }
    }

    private static boolean parseBoolean(String keyName, String value) {
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        throw new IllegalStateException(
                "Invalid " + keyName + ".optional value: " + value
                        + ". Use true or false in presentation-keys.properties and re-run ./gradlew run");
    }

    private static KeyType parseKeyType(String keyName, String value) {
        if ("text".equalsIgnoreCase(value)) {
            return KeyType.TEXT;
        }
        if ("image".equalsIgnoreCase(value)) {
            return KeyType.IMAGE;
        }
        throw new IllegalStateException(
                "Invalid " + keyName + ".type value: " + value
                        + ". Use text or image in presentation-keys.properties and re-run ./gradlew run");
    }
}
