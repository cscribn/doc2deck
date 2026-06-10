package com.appfire.presentation;

import java.nio.file.Files;
import java.nio.file.Path;

public final class TestFixtures {

    private TestFixtures() {
    }

    public static Path resolveSourcePptx() {
        return resolveFirstExisting(Path.of("source.pptx"), Path.of("src/test/resources/source.pptx"));
    }

    public static Path resolveSourceDocx() {
        return resolveFirstExisting(Path.of("source.docx"), Path.of("src/test/resources/source.docx"));
    }

    public static boolean hasSourcePptx() {
        Path path = resolveSourcePptx();
        return path != null && Files.exists(path);
    }

    public static boolean hasSourceDocx() {
        Path path = resolveSourceDocx();
        return path != null && Files.exists(path);
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
