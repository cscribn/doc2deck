package com.appfire.presentation.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class AppConfig {

    private final String geminiCliPath;
    private final Path sourcePptxPath;
    private final Path sourceDocxPath;
    private final Path outputPptxPath;
    private final String geminiModel;
    private final int geminiMaxRetries;

    private AppConfig(
            String geminiCliPath,
            Path sourcePptxPath,
            Path sourceDocxPath,
            Path outputPptxPath,
            String geminiModel,
            int geminiMaxRetries) {
        this.geminiCliPath = geminiCliPath;
        this.sourcePptxPath = sourcePptxPath;
        this.sourceDocxPath = sourceDocxPath;
        this.outputPptxPath = outputPptxPath;
        this.geminiModel = geminiModel;
        this.geminiMaxRetries = geminiMaxRetries;
    }

    public static AppConfig load() {
        Map<String, String> values = loadEnvFile(Path.of(".env"));
        mergeSystemEnv(values);

        String cliPath = values.getOrDefault("GEMINI_CLI_PATH", "gemini");
        Path pptx = Path.of(values.getOrDefault("SOURCE_PPTX_PATH", "source.pptx"));
        Path docx = Path.of(values.getOrDefault("SOURCE_DOCX_PATH", "source.docx"));
        Path output = Path.of(values.getOrDefault("OUTPUT_PPTX_PATH", "final_presentation.pptx"));
        String model = values.getOrDefault("GEMINI_MODEL", "gemini-3.1-flash");
        int retries = parseInt(values.getOrDefault("GEMINI_MAX_RETRIES", "3"), 3);

        validateGeminiCli(cliPath);
        validateInputFile(pptx, "SOURCE_PPTX_PATH");
        validateInputFile(docx, "SOURCE_DOCX_PATH");

        return new AppConfig(cliPath, pptx, docx, output, model, retries);
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
                "GEMINI_CLI_PATH", "SOURCE_PPTX_PATH", "SOURCE_DOCX_PATH",
                "OUTPUT_PPTX_PATH", "GEMINI_MODEL", "GEMINI_MAX_RETRIES")) {
            String env = System.getenv(key);
            if (env != null && !env.isBlank()) {
                values.put(key, env);
            }
        }
    }

    private static void validateGeminiCli(String cliPath) {
        try {
            Process process = new ProcessBuilder(cliPath, "--version")
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException(
                        "Gemini CLI timed out: " + cliPath
                                + ". Verify the gemini command is installed and re-run ./gradlew run");
            }
            if (process.exitValue() != 0) {
                throw new IllegalStateException(
                        "Gemini CLI not working: " + cliPath
                                + ". Install with npm install -g @google/gemini-cli, authenticate, and re-run ./gradlew run");
            }
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Gemini CLI not found: " + cliPath
                            + ". Install with npm install -g @google/gemini-cli and ensure it is on PATH, then re-run ./gradlew run",
                    e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while validating Gemini CLI", e);
        }
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

    public String geminiCliPath() {
        return geminiCliPath;
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
