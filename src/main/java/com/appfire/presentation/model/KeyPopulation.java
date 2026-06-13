package com.appfire.presentation.model;

public final class KeyPopulation {

    private KeyPopulation() {
    }

    public static boolean isPopulated(String key, String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String sanitized = value.trim();
        if (sanitized.equalsIgnoreCase(key)) {
            return false;
        }
        if (sanitized.equals("${" + key + "}")) {
            return false;
        }
        return true;
    }
}
