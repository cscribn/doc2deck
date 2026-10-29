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

    private final AiBackend aiBackend;
    private final int llmMaxRetries;
    private final String googleApiKey;
    private final String googleGenaiModel;
    private final String cursorCliPath;
    private final String cursorModel;
    private final Path cursorWorkspace;
    private final Path templatePptxPath;
    private final List<Path> sourceDocxPaths;
    private final Path outputPptxPath;
    private final String pexelsApiKey;
    private final Path imageCacheDir;
    private final float imageJpegQuality;
    private final boolean imageOptimizationEnabled;
    private final boolean fontCleanupEnabled;
    private final boolean layoutNormalizeEnabled;
    private final DeckFormat deckFormat;
    private final Path outputHtmlPath;
    private final Path htmlDeckOutlinePath;
    private final Path presentationKeysPath;
    private final Path voiceStylePath;
    private final Set<Integer> layoutSkipTextFitSlideIndices;

    private AppConfig(
            AiBackend aiBackend,
            int llmMaxRetries,
            String googleApiKey,
            String googleGenaiModel,
            String cursorCliPath,
            String cursorModel,
            Path cursorWorkspace,
            Path templatePptxPath,
            List<Path> sourceDocxPaths,
            Path outputPptxPath,
            DeckFormat deckFormat,
            Path outputHtmlPath,
            Path htmlDeckOutlinePath,
            String pexelsApiKey,
            Path imageCacheDir,
            float imageJpegQuality,
            boolean imageOptimizationEnabled,
            boolean fontCleanupEnabled,
            boolean layoutNormalizeEnabled,
            Path presentationKeysPath,
            Path voiceStylePath,
            Set<Integer> layoutSkipTextFitSlideIndices) {
        this.aiBackend = aiBackend;
        this.llmMaxRetries = llmMaxRetries;
        this.googleApiKey = googleApiKey;
        this.googleGenaiModel = googleGenaiModel;
        this.cursorCliPath = cursorCliPath;
        this.cursorModel = cursorModel;
        this.cursorWorkspace = cursorWorkspace;
        this.templatePptxPath = templatePptxPath;
        this.sourceDocxPaths = List.copyOf(sourceDocxPaths);
        this.outputPptxPath = outputPptxPath;
        this.deckFormat = deckFormat;
        this.outputHtmlPath = outputHtmlPath;
        this.htmlDeckOutlinePath = htmlDeckOutlinePath;
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

        AiBackend aiBackend = AiBackend.parse(values.getOrDefault("AI_BACKEND", "cursor_cli"));
        int llmMaxRetries = parseInt(values.getOrDefault("LLM_MAX_RETRIES", "3"), 3);
        String googleApiKey = values.getOrDefault("GOOGLE_API_KEY", "");
        String googleGenaiModel = values.getOrDefault("GOOGLE_GENAI_MODEL", "gemini-3.1-flash-lite");
        String cursorCliPath = values.getOrDefault("CURSOR_CLI_PATH", "agent");
        String cursorModel = values.getOrDefault("CURSOR_MODEL", "");
        Path cursorWorkspace = Path.of(values.getOrDefault("CURSOR_WORKSPACE", System.getProperty("user.dir")));

        Path pptx = Path.of(values.getOrDefault("TEMPLATE_PPTX_PATH", "template.pptx"));
        Path sourcesDir = Path.of(values.getOrDefault("SOURCES_DIR", "sources"));
        Path output = Path.of(values.getOrDefault("OUTPUT_PPTX_PATH", "final_presentation.pptx"));
        DeckFormat deckFormat = DeckFormat.parse(values.getOrDefault("DECK_FORMAT", "pptx"));
        Path outputHtml = Path.of(values.getOrDefault("OUTPUT_HTML_PATH", "final_presentation.html"));
        Path htmlOutline = Path.of(values.getOrDefault("HTML_DECK_OUTLINE_PATH", "html-deck-outline.properties"));
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

        validateAiBackend(aiBackend, googleApiKey, cursorCliPath, cursorWorkspace);

        List<Path> sourceDocxPaths = resolveSourceDocxPaths(sourcesDir);
        validateInputFile(voiceStyle, "VOICE_STYLE_PATH");

        if (deckFormat == DeckFormat.PPTX) {
            validateInputFile(pptx, "TEMPLATE_PPTX_PATH");
            validateInputFile(
                    presentationKeys,
                    "PRESENTATION_KEYS_PATH",
                    "Copy presentation-keys.example.properties to presentation-keys.properties");
        } else {
            validateInputFile(
                    htmlOutline,
                    "HTML_DECK_OUTLINE_PATH",
                    "Copy html-deck-outline.example.properties to html-deck-outline.properties");
        }

        return new AppConfig(
                aiBackend,
                llmMaxRetries,
                googleApiKey,
                googleGenaiModel,
                cursorCliPath,
                cursorModel,
                cursorWorkspace,
                pptx,
                sourceDocxPaths,
                output,
                deckFormat,
                outputHtml,
                htmlOutline,
                pexelsKey,
                imageCache,
                clampJpegQuality(jpegQuality),
                optimizationEnabled,
                fontCleanupEnabled,
                layoutNormalizeEnabled,
                presentationKeys,
                voiceStyle,
                skipTextFitSlideIndices);
    }

    private static void validateAiBackend(
            AiBackend aiBackend, String googleApiKey, String cursorCliPath, Path cursorWorkspace) {
        switch (aiBackend) {
            case GOOGLE_GENAI -> validateGoogleGenai(googleApiKey);
            case CURSOR_CLI -> validateCursorCli(cursorCliPath, cursorWorkspace);
        }
    }

    private static void validateGoogleGenai(String googleApiKey) {
        if (googleApiKey == null || googleApiKey.isBlank()) {
            throw new IllegalStateException(
                    "GOOGLE_API_KEY is required when AI_BACKEND=google_genai. "
                            + "Set it in .env or the environment and re-run ./gradlew bootRun");
        }
    }

    private static void validateCursorCli(String cursorCliPath, Path cursorWorkspace) {
        if (!Files.isDirectory(cursorWorkspace)) {
            throw new IllegalStateException(
                    "CURSOR_WORKSPACE is not a directory: " + cursorWorkspace
                            + ". Set CURSOR_WORKSPACE to the project root and re-run ./gradlew bootRun");
        }
        try {
            Process process = new ProcessBuilder(cursorCliPath, "--version")
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException(
                        "Cursor CLI timed out: " + cursorCliPath
                                + ". Verify the agent command is installed and re-run ./gradlew bootRun");
            }
            if (process.exitValue() != 0) {
                throw new IllegalStateException(
                        "Cursor CLI not working: " + cursorCliPath
                                + ". Install from https://cursor.com/docs/cli/overview and re-run ./gradlew bootRun");
            }
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Cursor CLI not found: " + cursorCliPath
                            + ". Install Cursor CLI and ensure it is on PATH, then re-run ./gradlew bootRun",
                    e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while validating Cursor CLI", e);
        }
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
                    "Failed to read .env file. Check file permissions and re-run ./gradlew bootRun", e);
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
                "AI_BACKEND",
                "LLM_MAX_RETRIES",
                "GOOGLE_API_KEY",
                "GOOGLE_GENAI_MODEL",
                "CURSOR_CLI_PATH",
                "CURSOR_MODEL",
                "CURSOR_WORKSPACE",
                "TEMPLATE_PPTX_PATH",
                "SOURCES_DIR",
                "OUTPUT_PPTX_PATH",
                "DECK_FORMAT",
                "OUTPUT_HTML_PATH",
                "HTML_DECK_OUTLINE_PATH",
                "PEXELS_API_KEY",
                "IMAGE_CACHE_DIR",
                "IMAGE_JPEG_QUALITY",
                "IMAGE_OPTIMIZATION_ENABLED",
                "FONT_CLEANUP_ENABLED",
                "LAYOUT_NORMALIZE_ENABLED",
                "PRESENTATION_KEYS_PATH",
                "VOICE_STYLE_PATH",
                "LAYOUT_SKIP_TEXT_FIT_SLIDE_INDICES")) {
            String env = System.getenv(key);
            if (env != null && !env.isBlank()) {
                values.put(key, env);
            }
        }
    }

    private static List<Path> resolveSourceDocxPaths(Path sourcesDir) {
        if (!Files.isDirectory(sourcesDir)) {
            throw new IllegalStateException(
                    "Sources directory not found: " + sourcesDir
                            + ". Create it, place one or more .docx files inside, and re-run ./gradlew bootRun");
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
                            + ". Check directory permissions and re-run ./gradlew bootRun",
                    e);
        }
        if (docxPaths.isEmpty()) {
            throw new IllegalStateException(
                    "No .docx files found in " + sourcesDir
                            + ". Place one or more .docx files in sources/ and re-run ./gradlew bootRun");
        }
        for (Path path : docxPaths) {
            validateInputFile(
                    path,
                    "SOURCES_DIR",
                    "Ensure every .docx file in " + sourcesDir + " is readable and re-run ./gradlew bootRun");
        }
        return docxPaths;
    }

    private static void validateInputFile(Path path, String envName) {
        validateInputFile(path, envName, "Set " + envName + " in .env to a valid path and re-run ./gradlew bootRun");
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
                                + ". Use comma-separated 0-based slide indices and re-run ./gradlew bootRun");
            }
        }
        return indices;
    }

    public AiBackend aiBackend() {
        return aiBackend;
    }

    public int llmMaxRetries() {
        return llmMaxRetries;
    }

    public String googleApiKey() {
        return googleApiKey;
    }

    public String googleGenaiModel() {
        return googleGenaiModel;
    }

    public String cursorCliPath() {
        return cursorCliPath;
    }

    public String cursorModel() {
        return cursorModel;
    }

    public Path cursorWorkspace() {
        return cursorWorkspace;
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

    public DeckFormat deckFormat() {
        return deckFormat;
    }

    public Path outputHtmlPath() {
        return outputHtmlPath;
    }

    public Path htmlDeckOutlinePath() {
        return htmlDeckOutlinePath;
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
