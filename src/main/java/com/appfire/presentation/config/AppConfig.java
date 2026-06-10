package com.appfire.presentation.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AppConfig {

    private final String geminiApiKey;
    private final Path sourcePptxPath;
    private final Path sourceDocxPath;
    private final Path outputPptxPath;
    private final String geminiModel;
    private final int geminiMaxRetries;

    private AppConfig(
            String geminiApiKey,
            Path sourcePptxPath,
            Path sourceDocxPath,
            Path outputPptxPath,
            String geminiModel,
            int geminiMaxRetries) {
        this.geminiApiKey = geminiApiKey;
        this.sourcePptxPath = sourcePptxPath;
        this.sourceDocxPath = sourceDocxPath;
        this.outputPptxPath = outputPptxPath;
        this.geminiModel = geminiModel;
        this.geminiMaxRetries = geminiMaxRetries;
    }

    public static AppConfig load() {
        Map<String, String> values = loadEnvFile(Path.of(".env"));
        mergeSystemEnv(values);

        String apiKey = requireValue(values, "GEMINI_API_KEY",
                "Set GEMINI_API_KEY in .env and re-run ./gradlew run");
        Path pptx = Path.of(values.getOrDefault("SOURCE_PPTX_PATH", "source.pptx"));
        Path docx = Path.of(values.getOrDefault("SOURCE_DOCX_PATH", "source.docx"));
        Path output = Path.of(values.getOrDefault("OUTPUT_PPTX_PATH", "final_presentation.pptx"));
        String model = values.getOrDefault("GEMINI_MODEL", "gemini-3.1-flash");
        int retries = parseInt(values.getOrDefault("GEMINI_MAX_RETRIES", "3"), 3);

        validateInputFile(pptx, "SOURCE_PPTX_PATH");
        validateInputFile(docx, "SOURCE_DOCX_PATH");

        return new AppConfig(apiKey, pptx, docx, output, model, retries);
    }

    private static Map<String, String> loadEnvFile(Path envPath) {
        Map<String, String> values = new HashMap<>();
        if (!Files.exists(envPath)) {
            return values;
        }
        try {
            List<String> lines = Files.readAllLines(envPath);
            for (String line : lines) {
                parseEnvLine(line, values);
            }
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to read .env file. Check file permissions and re-run ./gradlew run", e);
        }
        return values;
    }

    private static void parseEnvLine(String line, Map<String, String> values) {
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
            return;
        }
        int eq = trimmed.indexOf('=');
        if (eq <= 0) {
            return;
        }
        String key = trimmed.substring(0, eq).trim();
        String value = trimmed.substring(eq + 1).trim();
        values.put(key, value);
    }

    private static void mergeSystemEnv(Map<String, String> values) {
        for (String key : List.of(
                "GEMINI_API_KEY", "SOURCE_PPTX_PATH", "SOURCE_DOCX_PATH",
                "OUTPUT_PPTX_PATH", "GEMINI_MODEL", "GEMINI_MAX_RETRIES")) {
            String env = System.getenv(key);
            if (env != null && !env.isBlank()) {
                values.put(key, env);
            }
        }
    }

    private static String requireValue(Map<String, String> values, String key, String resolution) {
        String value = values.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required environment variable: " + key + ". " + resolution);
        }
        return value;
    }

    private static void validateInputFile(Path path, String envName) {
        if (!Files.exists(path) || !Files.isReadable(path)) {
            throw new IllegalStateException(
                    "Input file not found or unreadable: " + path
                            + ". Set " + envName + " in .env to a valid path and re-run ./gradlew run");
        }
    }

    private static int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public String geminiApiKey() {
        return geminiApiKey;
    }

    public Path sourcePptxPath() {
        return sourcePptxPath;
    }

    public Path sourceDocxPath() {
        return sourceDocxPath;
    }

    public Path outputPptxPath() {
        return outputPptxPath;
    }

    public String geminiModel() {
        return geminiModel;
    }

    public int geminiMaxRetries() {
        return geminiMaxRetries;
    }
}
