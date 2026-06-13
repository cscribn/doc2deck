package com.appfire.presentation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public final class TestFixtures {

    private TestFixtures() {
    }

    public static Path resolveTemplatePptx() {
        return resolveFirstExisting(Path.of("template.pptx"), Path.of("src/test/resources/template.pptx"));
    }

    public static Path resolveSourceDocx() {
        List<Path> sources = listSourceDocxPaths();
        if (!sources.isEmpty()) {
            return sources.get(0);
        }
        Path fallback = Path.of("src/test/resources/source.docx");
        return Files.exists(fallback) ? fallback : null;
    }

    public static Path resolveSourcesDir() {
        Path sourcesDir = Path.of("sources");
        if (Files.isDirectory(sourcesDir) && !listSourceDocxPaths(sourcesDir).isEmpty()) {
            return sourcesDir;
        }
        return null;
    }

    public static boolean hasTemplatePptx() {
        Path path = resolveTemplatePptx();
        return path != null && Files.exists(path);
    }

    public static boolean hasSourceDocx() {
        return resolveSourceDocx() != null;
    }

    public static List<Path> listSourceDocxPaths() {
        Path sourcesDir = Path.of("sources");
        if (!Files.isDirectory(sourcesDir)) {
            return List.of();
        }
        return listSourceDocxPaths(sourcesDir);
    }

    private static List<Path> listSourceDocxPaths(Path sourcesDir) {
        try (Stream<Path> stream = Files.list(sourcesDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".docx"))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    private static Path resolveFirstExisting(Path... paths) {
        for (Path path : paths) {
            if (Files.exists(path)) {
                return path;
            }
        }
        return null;
    }
}
