package com.appfire.presentation.llm;

import com.appfire.presentation.config.PresentationKeysConfig;
import com.appfire.presentation.model.KeyDefinition;
import com.appfire.presentation.model.KeyPopulation;
import com.appfire.presentation.model.PresentationContentResponse;
import com.appfire.presentation.model.TemplateScanResult;
import com.appfire.presentation.template.WordCount;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ResponseValidator {

    private static final Logger LOG = LoggerFactory.getLogger(ResponseValidator.class);
    private static final int MAX_SHORT_DESCRIPTION_CHARS = 200;

    public ValidationResult validate(
            PresentationContentResponse response,
            TemplateScanResult scan,
            PresentationKeysConfig keysConfig) {
        List<String> critical = new ArrayList<>();
        List<String> advisory = new ArrayList<>();

        if (response.keys() == null || response.keys().isEmpty()) {
            critical.add("Missing or empty keys object");
            return new ValidationResult(false, critical, advisory);
        }

        Set<String> foundKeys = scan.foundKeys();
        List<String> requiredKeys = keysConfig.requiredKeysForTemplate(foundKeys);

        for (String key : requiredKeys) {
            String value = response.keys().get(key);
            if (!KeyPopulation.isPopulated(key, value)) {
                critical.add("Required key '" + key + "' is missing or blank");
            }
        }

        validateShortDescription(response, keysConfig, advisory);
        validateTextKeyWordLimits(response, keysConfig, foundKeys, critical);
        validateImageQueries(response, keysConfig, foundKeys, critical);
        validateSourceRefs(response, requiredKeys, advisory);
        validateTemplateAlignment(response, scan, keysConfig, advisory);

        boolean passed = critical.isEmpty();
        if (!passed) {
            critical.forEach(msg -> LOG.error("Validation critical: {}. Resolution: fix Gemini output or re-run.",
                    msg));
        }
        advisory.forEach(msg -> LOG.warn("Validation advisory: {}", msg));

        return new ValidationResult(passed, critical, advisory);
    }

    private void validateShortDescription(
            PresentationContentResponse response,
            PresentationKeysConfig keysConfig,
            List<String> advisory) {
        String keyName = "shortProjectDescription";
        if (!keysConfig.configuredKeyNames().contains(keyName)) {
            return;
        }
        String value = response.keys().get(keyName);
        if (value == null || value.isBlank()) {
            return;
        }
        if (value.length() > MAX_SHORT_DESCRIPTION_CHARS) {
            advisory.add("shortProjectDescription exceeds " + MAX_SHORT_DESCRIPTION_CHARS + " characters");
        }
    }

    private void validateTextKeyWordLimits(
            PresentationContentResponse response,
            PresentationKeysConfig keysConfig,
            Set<String> foundKeys,
            List<String> critical) {
        for (KeyDefinition definition : keysConfig.keys().values()) {
            if (!definition.isText() || !foundKeys.contains(definition.name())) {
                continue;
            }
            String value = response.keys().get(definition.name());
            if (!KeyPopulation.isPopulated(definition.name(), value)) {
                continue;
            }
            int wordCount = WordCount.count(value);
            int maxWords = definition.maxWords();
            if (wordCount > maxWords) {
                critical.add("Key '" + definition.name() + "' exceeds " + maxWords + "-word limit, got " + wordCount);
            }
        }
    }

    private void validateImageQueries(
            PresentationContentResponse response,
            PresentationKeysConfig keysConfig,
            Set<String> foundKeys,
            List<String> critical) {
        for (KeyDefinition definition : keysConfig.keys().values()) {
            if (!definition.isImage() || !foundKeys.contains(definition.name())) {
                continue;
            }
            String query = response.keys().get(definition.name());
            if (query == null || query.isBlank()) {
                continue;
            }
            int wordCount = query.trim().split("\\s+").length;
            int minWords = PresentationKeysConfig.MIN_IMAGE_QUERY_WORDS;
            int maxWords = keysConfig.maxImageQueryWords(definition.name());
            if (wordCount < minWords || wordCount > maxWords) {
                critical.add("Image key '" + definition.name() + "' query must be " + minWords + "-" + maxWords
                        + " words, got " + wordCount);
            }
        }
    }

    private void validateSourceRefs(
            PresentationContentResponse response,
            List<String> requiredKeys,
            List<String> advisory) {
        for (String key : requiredKeys) {
            List<Integer> refs = response.sourceRefs().get(key);
            if (refs == null || refs.isEmpty()) {
                advisory.add("Key '" + key + "' has empty sourceRefs");
            }
        }
    }

    private void validateTemplateAlignment(
            PresentationContentResponse response,
            TemplateScanResult scan,
            PresentationKeysConfig keysConfig,
            List<String> advisory) {
        Set<String> found = scan.foundKeys();
        Set<String> configured = keysConfig.configuredKeyNames();

        for (String key : found) {
            if (!KeyPopulation.isPopulated(key, response.keys().get(key))) {
                KeyDefinition definition = keysConfig.definitionFor(key);
                if (definition != null && definition.optional()) {
                    continue;
                }
                advisory.add("Template placeholder '${" + key + "}' has no Gemini value");
            }
        }

        for (String key : response.keys().keySet()) {
            if (!configured.contains(key)) {
                advisory.add("Gemini returned unknown key '" + key + "'");
            } else if (!found.contains(key) && keysConfig.definitionFor(key).isText()) {
                advisory.add("Gemini key '" + key + "' not found in template scan");
            }
        }

        for (String optionalKey : keysConfig.optionalTextKeyNames()) {
            if (found.contains(optionalKey)
                    && !KeyPopulation.isPopulated(optionalKey, response.keys().get(optionalKey))) {
                advisory.add(optionalKey + " absent (optional key)");
            }
        }

        scan.splitRunWarnings().forEach(advisory::add);
    }

    public record ValidationResult(boolean passed, List<String> criticalFailures, List<String> advisories) {
    }
}
