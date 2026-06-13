package com.appfire.presentation.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public final class AppConfig {

    private final String geminiCliPath;
    private final Path templatePptxPath;
    private final List<Path> sourceDocxPaths;
    private final Path outputPptxPath;
    private final String geminiModel;
    private final int geminiMaxRetries;
    private final String pexelsApiKey;
    private final Path imageCacheDir;
    private final float imageJpegQuality;
    private final boolean imageOptimizationEnabled;
    private final boolean fontCleanupEnabled;
    private final boolean layoutNormalizeEnabled;
    private final Path presentationKeysPath;
    private final Path voiceStylePath;
    private final Set<Integer> layoutSkipTextFitSlideIndices;

    private AppConfig(
            String geminiCliPath,
            Path templatePptxPath,
            List<Path> sourceDocxPaths,
            Path outputPptxPath,
            String geminiModel,
            int geminiMaxRetries,
            String pexelsApiKey,
            Path imageCacheDir,
            float imageJpegQuality,
            boolean imageOptimizationEnabled,
            boolean fontCleanupEnabled,
            boolean layoutNormalizeEnabled,
            Path presentationKeysPath,
            Path voiceStylePath,
            Set<Integer> layoutSkipTextFitSlideIndices) {
        this.geminiCliPath = geminiCliPath;
        this.templatePptxPath = templatePptxPath;
        this.sourceDocxPaths = List.copyOf(sourceDocxPaths);
        this.outputPptxPath = outputPptxPath;
        this.geminiModel = geminiModel;
        this.geminiMaxRetries = geminiMaxRetries;
        this.pexelsApiKey = pexelsApiKey;
        this.imageCacheDir = imageCacheDir;
        this.imageJpegQuality = imageJpegQuality;
        this.imageOptimizationEnabled = imageOptimizationEnabled;
        this.fontCleanupEnabled = fontCleanupEnabled;
        this.layoutNormalizeEnabled = layoutNormalizeEnabled;
        this.presentationKeysPath = presentationKeysPath;
        this.voiceStylePath = voiceStylePath;
        this.layoutSkipTextFitSlideIndices = Set.copyOf(layoutSkipTextFitSlideIndices);
    }

    public static AppConfig load() {
        Map<String, String> values = loadEnvFile(Path.of(".env"));
        mergeSystemEnv(values);

        String cliPath = values.getOrDefault("GEMINI_CLI_PATH", "gemini");
        Path pptx = Path.of(values.getOrDefault("TEMPLATE_PPTX_PATH", "template.pptx"));
        Path sourcesDir = Path.of(values.getOrDefault("SOURCES_DIR", "sources"));
        Path output = Path.of(values.getOrDefault("OUTPUT_PPTX_PATH", "final_presentation.pptx"));
        String model = values.getOrDefault("GEMINI_MODEL", "gemini-3.1-flash-lite");
        int retries = parseInt(values.getOrDefault("GEMINI_MAX_RETRIES", "3"), 3);
        String pexelsKey = values.getOrDefault("PEXELS_API_KEY", "");
        Path imageCache = Path.of(values.getOrDefault("IMAGE_CACHE_DIR", ".cache/images"));
        float jpegQuality = parseFloat(values.getOrDefault("IMAGE_JPEG_QUALITY", "0.8"), 0.8f);
        boolean optimizationEnabled = parseBoolean(values.getOrDefault("IMAGE_OPTIMIZATION_ENABLED", "true"), true);
        boolean fontCleanupEnabled = parseBoolean(values.getOrDefault("FONT_CLEANUP_ENABLED", "true"), true);
        boolean layoutNormalizeEnabled = parseBoolean(values.getOrDefault("LAYOUT_NORMALIZE_ENABLED", "true"), true);
        Path presentationKeys = Path.of(values.getOrDefault("PRESENTATION_KEYS_PATH", "presentation-keys.properties"));
        Path voiceStyle = Path.of(values.getOrDefault("VOICE_STYLE_PATH", "prompts/voice-styles/neutral.md"));
        Set<Integer> skipTextFitSlideIndices = parseSkipTextFitSlideIndices(
                values.getOrDefault("LAYOUT_SKIP_TEXT_FIT_SLIDE_INDICES", ""));

        validateGeminiCli(cliPath);
        validateInputFile(pptx, "TEMPLATE_PPTX_PATH");
        List<Path> sourceDocxPaths = resolveSourceDocxPaths(sourcesDir);
        validateInputFile(
                presentationKeys,
                "PRESENTATION_KEYS_PATH",
                "Copy presentation-keys.example.properties to presentation-keys.properties");

        validateInputFile(voiceStyle, "VOICE_STYLE_PATH");

        return new AppConfig(
                cliPath, pptx, sourceDocxPaths, output, model, retries, pexelsKey, imageCache,
                clampJpegQuality(jpegQuality), optimizationEnabled, fontCleanupEnabled, layoutNormalizeEnabled,
                presentationKeys, voiceStyle, skipTextFitSlideIndices);
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
                "GEMINI_CLI_PATH", "TEMPLATE_PPTX_PATH", "SOURCES_DIR",
                "OUTPUT_PPTX_PATH", "GEMINI_MODEL", "GEMINI_MAX_RETRIES",
                "PEXELS_API_KEY", "IMAGE_CACHE_DIR",
                "IMAGE_JPEG_QUALITY", "IMAGE_OPTIMIZATION_ENABLED", "FONT_CLEANUP_ENABLED",
                "LAYOUT_NORMALIZE_ENABLED", "PRESENTATION_KEYS_PATH",
                "VOICE_STYLE_PATH", "LAYOUT_SKIP_TEXT_FIT_SLIDE_INDICES")) {
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

    private static List<Path> resolveSourceDocxPaths(Path sourcesDir) {
        if (!Files.isDirectory(sourcesDir)) {
            throw new IllegalStateException(
                    "Sources directory not found: " + sourcesDir
                            + ". Create it, place one or more .docx files inside, and re-run ./gradlew run");
        }
        List<Path> docxPaths;
        try (Stream<Path> stream = Files.list(sourcesDir)) {
            docxPaths = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".docx"))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to read sources directory: " + sourcesDir
                            + ". Check directory permissions and re-run ./gradlew run",
                    e);
        }
        if (docxPaths.isEmpty()) {
            throw new IllegalStateException(
                    "No .docx files found in " + sourcesDir
                            + ". Place one or more .docx files in sources/ and re-run ./gradlew run");
        }
        for (Path path : docxPaths) {
            validateInputFile(
                    path,
                    "SOURCES_DIR",
                    "Ensure every .docx file in " + sourcesDir + " is readable and re-run ./gradlew run");
        }
        return docxPaths;
    }

    private static void validateInputFile(Path path, String envName) {
        validateInputFile(path, envName, "Set " + envName + " in .env to a valid path and re-run ./gradlew run");
    }

    private static void validateInputFile(Path path, String envName, String resolution) {
        if (!Files.exists(path) || !Files.isReadable(path)) {
            throw new IllegalStateException(
                    "Input file not found or unreadable: " + path + ". " + resolution);
        }
    }

    private static int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static float parseFloat(String value, float defaultValue) {
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static boolean parseBoolean(String value, boolean defaultValue) {
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        return defaultValue;
    }

    private static float clampJpegQuality(float quality) {
        return Math.max(0.0f, Math.min(1.0f, quality));
    }

    private static Set<Integer> parseSkipTextFitSlideIndices(String raw) {
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }
        Set<Integer> indices = new HashSet<>();
        for (String part : raw.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                indices.add(Integer.parseInt(trimmed));
            } catch (NumberFormatException e) {
                throw new IllegalStateException(
                        "Invalid LAYOUT_SKIP_TEXT_FIT_SLIDE_INDICES value: " + raw
                                + ". Use comma-separated 0-based slide indices and re-run ./gradlew run");
            }
        }
        return indices;
    }

    public String geminiCliPath() {
        return geminiCliPath;
    }

    public Path templatePptxPath() {
        return templatePptxPath;
    }

    public List<Path> sourceDocxPaths() {
        return sourceDocxPaths;
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

    public String pexelsApiKey() {
        return pexelsApiKey;
    }

    public Path imageCacheDir() {
        return imageCacheDir;
    }

    public float imageJpegQuality() {
        return imageJpegQuality;
    }

    public boolean imageOptimizationEnabled() {
        return imageOptimizationEnabled;
    }

    public boolean fontCleanupEnabled() {
        return fontCleanupEnabled;
    }

    public boolean layoutNormalizeEnabled() {
        return layoutNormalizeEnabled;
    }

    public Path presentationKeysPath() {
        return presentationKeysPath;
    }

    public Path voiceStylePath() {
        return voiceStylePath;
    }

    public Set<Integer> layoutSkipTextFitSlideIndices() {
        return layoutSkipTextFitSlideIndices;
    }
}
