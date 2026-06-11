package com.appfire.presentation;

import java.nio.file.Files;
import java.nio.file.Path;

public final class TestFixtures {

    private TestFixtures() {
    }

    public static Path resolveTemplatePptx() {
        return resolveFirstExisting(Path.of("template.pptx"), Path.of("src/test/resources/template.pptx"));
    }

    public static Path resolveSourceDocx() {
        return resolveFirstExisting(Path.of("source.docx"), Path.of("src/test/resources/source.docx"));
    }

    public static boolean hasTemplatePptx() {
        Path path = resolveTemplatePptx();
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
