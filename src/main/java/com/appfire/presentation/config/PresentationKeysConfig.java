package com.appfire.presentation.config;

import com.appfire.presentation.model.KeyDefinition;
import com.appfire.presentation.model.KeyType;
import com.appfire.presentation.model.TemplateScanResult;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class PresentationKeysConfig {

    public static final String HEADER_KEY = "presentationKeys.header";
    public static final int DEFAULT_MAX_WORDS = 15;
    public static final int DEFAULT_IMAGE_MAX_WORDS = 5;
    public static final int MIN_IMAGE_QUERY_WORDS = 2;

    private final String header;
    private final Map<String, KeyDefinition> keys;

    public PresentationKeysConfig(String header, Map<String, KeyDefinition> keys) {
        this.header = header;
        this.keys = Map.copyOf(keys);
    }

    public String header() {
        return header;
    }

    public Map<String, KeyDefinition> keys() {
        return keys;
    }

    public KeyDefinition definitionFor(String keyName) {
        return keys.get(keyName);
    }

    public List<String> textKeyNames() {
        return keys.values().stream()
                .filter(KeyDefinition::isText)
                .map(KeyDefinition::name)
                .toList();
    }

    public List<String> imageKeyNames() {
        return keys.values().stream()
                .filter(KeyDefinition::isImage)
                .map(KeyDefinition::name)
                .toList();
    }

    public List<String> optionalTextKeyNames() {
        return keys.values().stream()
                .filter(KeyDefinition::isText)
                .filter(KeyDefinition::optional)
                .map(KeyDefinition::name)
                .toList();
    }

    public Set<String> configuredKeyNames() {
        return keys.keySet();
    }

    public List<String> keysForTemplate(Set<String> foundKeys) {
        List<String> ordered = new ArrayList<>();
        for (KeyDefinition definition : keys.values()) {
            if (foundKeys.contains(definition.name())) {
                ordered.add(definition.name());
            }
        }
        return ordered;
    }

    public List<String> requiredKeysForTemplate(Set<String> foundKeys) {
        return keys.values().stream()
                .filter(def -> foundKeys.contains(def.name()))
                .filter(def -> !def.optional())
                .map(KeyDefinition::name)
                .toList();
    }

    public List<String> validateAgainstTemplate(TemplateScanResult scan) {
        List<String> advisories = new ArrayList<>();
        Set<String> foundKeys = scan.foundKeys();

        for (String templateKey : foundKeys) {
            if (!keys.containsKey(templateKey)) {
                throw new IllegalStateException(
                        "Template placeholder '${" + templateKey + "}' has no entry in presentation-keys config. "
                                + "Add " + templateKey + " to presentation-keys.properties and re-run ./gradlew run");
            }
        }

        for (String configuredKey : keys.keySet()) {
            if (!foundKeys.contains(configuredKey)) {
                advisories.add("Configured key '" + configuredKey + "' not found in template scan");
            }
        }

        return advisories;
    }

    public String formatForPrompt(Set<String> templateKeys) {
        StringBuilder prompt = new StringBuilder(header.trim()).append("\n\n");
        for (KeyDefinition definition : keys.values()) {
            if (!definition.isText() || !templateKeys.contains(definition.name())) {
                continue;
            }
            prompt.append(definition.name())
                    .append(" - ")
                    .append(definition.instruction().trim())
                    .append('\n');
        }
        return prompt.toString().stripTrailing();
    }

    public String formatImageKeysForPrompt(Set<String> templateKeys) {
        StringBuilder prompt = new StringBuilder();
        for (KeyDefinition definition : keys.values()) {
            if (!definition.isImage() || !templateKeys.contains(definition.name())) {
                continue;
            }
            prompt.append(definition.name())
                    .append(" - ")
                    .append(definition.instruction().trim())
                    .append('\n');
        }
        return prompt.toString().stripTrailing();
    }

    public String buildKeysJsonSchema(Set<String> templateKeys) {
        StringBuilder schema = new StringBuilder();
        for (String keyName : keysForTemplate(templateKeys)) {
            KeyDefinition definition = keys.get(keyName);
            String typeHint = definition.isImage() ? "string" : "string";
            if (definition.optional()) {
                schema.append("    \"")
                        .append(keyName)
                        .append("\": \"")
                        .append(typeHint)
                        .append(" or omit if not applicable\",\n");
            } else {
                schema.append("    \"")
                        .append(keyName)
                        .append("\": \"")
                        .append(typeHint)
                        .append("\",\n");
            }
        }
        if (schema.length() > 0) {
            schema.setLength(schema.length() - 2);
        }
        return schema.toString();
    }

    public String buildImageKeyInstructionStep(Set<String> templateKeys) {
        List<String> imageKeys = keys.values().stream()
                .filter(KeyDefinition::isImage)
                .map(KeyDefinition::name)
                .filter(templateKeys::contains)
                .toList();
        if (imageKeys.isEmpty()) {
            return "5. Skip image keys (none in template).";
        }
        return "5. Fill " + String.join(", ", imageKeys) + " per IMAGE KEYS rules.";
    }

    public int maxImageQueryWords(String keyName) {
        KeyDefinition definition = keys.get(keyName);
        if (definition == null || !definition.isImage()) {
            return DEFAULT_IMAGE_MAX_WORDS;
        }
        return definition.maxWords();
    }

    public Map<String, String> instructions() {
        return keys.values().stream()
                .collect(Collectors.toMap(
                        KeyDefinition::name,
                        KeyDefinition::instruction,
                        (left, right) -> left,
                        LinkedHashMap::new));
    }
}
