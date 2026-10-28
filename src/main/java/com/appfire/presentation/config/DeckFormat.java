package com.appfire.presentation.config;

public enum DeckFormat {
    PPTX,
    HTML;

    public static DeckFormat parse(String raw) {
        if (raw == null || raw.isBlank() || "pptx".equalsIgnoreCase(raw.trim())) {
            return PPTX;
        }
        if ("html".equalsIgnoreCase(raw.trim())) {
            return HTML;
        }
        throw new IllegalStateException(
                "Invalid DECK_FORMAT value: " + raw + ". Use pptx or html and re-run ./gradlew bootRun");
    }
}
