package com.appfire.presentation.llm;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class PromptLoader {

    private final Path promptsDir;
    private final Map<String, String> cache = new HashMap<>();

    public PromptLoader() {
        this(Path.of("prompts"));
    }

    public PromptLoader(Path promptsDir) {
        this.promptsDir = promptsDir;
    }

    public String load(String filename) {
        return cache.computeIfAbsent(filename, this::readPromptFile);
    }

    public String apply(String template, Map<String, String> placeholders) {
        String result = template;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return result;
    }

    private String readPromptFile(String filename) {
        Path path = promptsDir.resolve(filename);
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException(
                    "Prompt file not found: " + path.toAbsolutePath()
                            + ". Ensure the prompts directory exists at the project root and re-run ./gradlew run");
        }
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to read prompt file: " + path.toAbsolutePath()
                            + ". Check file permissions and re-run ./gradlew run",
                    e);
        }
    }
}
