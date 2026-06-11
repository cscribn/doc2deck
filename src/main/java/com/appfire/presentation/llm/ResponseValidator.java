package com.appfire.presentation.llm;

import com.appfire.presentation.model.PresentationContentResponse;
import com.appfire.presentation.model.PresentationKeys;
import com.appfire.presentation.model.TemplateScanResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ResponseValidator {

    private static final Logger LOG = LoggerFactory.getLogger(ResponseValidator.class);
    private static final int MAX_SHORT_DESCRIPTION_CHARS = 200;
    private static final int MIN_IMAGE_QUERY_WORDS = 2;
    private static final int MAX_IMAGE_QUERY_WORDS = 5;

    public ValidationResult validate(PresentationContentResponse response, TemplateScanResult scan) {
        List<String> critical = new ArrayList<>();
        List<String> advisory = new ArrayList<>();

        if (response.keys() == null || response.keys().isEmpty()) {
            critical.add("Missing or empty keys object");
            return new ValidationResult(false, critical, advisory);
        }

        for (String key : PresentationKeys.requiredKeys()) {
            String value = response.keys().get(key);
            if (value == null || value.isBlank()) {
                critical.add("Required key '" + key + "' is missing or blank");
            }
        }

        validateShortDescription(response, advisory);
        validateImageQueries(response, critical);
        validateSourceRefs(response, advisory);
        validateTemplateAlignment(response, scan, advisory);

        boolean passed = critical.isEmpty();
        if (!passed) {
            critical.forEach(msg -> LOG.error("Validation critical: {}. Resolution: fix Gemini output or re-run.",
                    msg));
        }
        advisory.forEach(msg -> LOG.warn("Validation advisory: {}", msg));

        return new ValidationResult(passed, critical, advisory);
    }

    private void validateShortDescription(PresentationContentResponse response, List<String> advisory) {
        String value = response.keys().get(PresentationKeys.SHORT_PROJECT_DESCRIPTION);
        if (value != null && value.length() > MAX_SHORT_DESCRIPTION_CHARS) {
            advisory.add("shortProjectDescription exceeds " + MAX_SHORT_DESCRIPTION_CHARS + " characters");
        }
    }

    private void validateImageQueries(PresentationContentResponse response, List<String> critical) {
        for (String key : PresentationKeys.imageKeys()) {
            String query = response.keys().get(key);
            if (query == null || query.isBlank()) {
                continue;
            }
            int wordCount = query.trim().split("\\s+").length;
            if (wordCount < MIN_IMAGE_QUERY_WORDS || wordCount > MAX_IMAGE_QUERY_WORDS) {
                critical.add("Image key '" + key + "' query must be 2-5 words, got " + wordCount);
            }
        }
    }

    private void validateSourceRefs(PresentationContentResponse response, List<String> advisory) {
        for (String key : PresentationKeys.requiredKeys()) {
            List<Integer> refs = response.sourceRefs().get(key);
            if (refs == null || refs.isEmpty()) {
                advisory.add("Key '" + key + "' has empty sourceRefs");
            }
        }
    }

    private void validateTemplateAlignment(
            PresentationContentResponse response,
            TemplateScanResult scan,
            List<String> advisory) {
        Set<String> found = scan.foundKeys();
        for (String key : found) {
            if (!response.keys().containsKey(key) || response.keys().get(key).isBlank()) {
                advisory.add("Template placeholder '${" + key + "}' has no Gemini value");
            }
        }
        for (String key : response.keys().keySet()) {
            if (!PresentationKeys.allKeyNames().contains(key)) {
                advisory.add("Gemini returned unknown key '" + key + "'");
            } else if (!found.contains(key) && !PresentationKeys.isImageKey(key)) {
                advisory.add("Gemini key '" + key + "' not found in template scan");
            }
        }
        if (!response.keys().containsKey(PresentationKeys.NON_DEV_COSTS)) {
            advisory.add("nonDevCosts absent (optional key)");
        }
        scan.splitRunWarnings().forEach(advisory::add);
    }

    public record ValidationResult(boolean passed, List<String> criticalFailures, List<String> advisories) {
    }
}
