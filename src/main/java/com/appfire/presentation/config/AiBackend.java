package com.appfire.presentation.config;

public enum AiBackend {
    CURSOR_CLI("cursor_cli"),
    GOOGLE_GENAI("google_genai");

    private final String envValue;

    AiBackend(String envValue) {
        this.envValue = envValue;
    }

    public String envValue() {
        return envValue;
    }

    public static AiBackend parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return CURSOR_CLI;
        }
        String normalized = raw.trim().toLowerCase();
        for (AiBackend backend : values()) {
            if (backend.envValue.equals(normalized)) {
                return backend;
            }
        }
        throw new IllegalStateException(
                "Invalid AI_BACKEND: " + raw + ". Use cursor_cli or google_genai and re-run ./gradlew bootRun");
    }
}
