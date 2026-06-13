package com.appfire.presentation.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record PresentationContentResponse(
        Map<String, String> keys,
        Map<String, List<Integer>> sourceRefs,
        List<String> warnings) {

    public PresentationContentResponse {
        if (keys == null) {
            keys = Map.of();
        }
        if (sourceRefs == null) {
            sourceRefs = Map.of();
        }
        if (warnings == null) {
            warnings = List.of();
        }
    }

    public String valueFor(String key) {
        return keys.get(key);
    }

    public Map<String, String> textValues(java.util.Collection<String> textKeyNames) {
        Map<String, String> text = new HashMap<>();
        for (String key : textKeyNames) {
            String value = keys.get(key);
            if (value != null && !value.isBlank()) {
                text.put(key, value);
            }
        }
        return text;
    }

    public Map<String, String> imageQueries(java.util.Collection<String> imageKeyNames) {
        Map<String, String> queries = new HashMap<>();
        for (String key : imageKeyNames) {
            String value = keys.get(key);
            if (value != null && !value.isBlank()) {
                queries.put(key, value);
            }
        }
        return queries;
    }
}
