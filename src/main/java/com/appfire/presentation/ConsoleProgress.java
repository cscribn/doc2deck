package com.appfire.presentation;

import java.nio.file.Path;

/** User-facing pipeline progress written to standard output. */
public final class ConsoleProgress {

    private ConsoleProgress() {
    }

    public static void step(String message) {
        System.out.println(message);
    }

    public static void complete(Path outputPath) {
        System.out.println("Done. Presentation saved to " + outputPath.toAbsolutePath());
    }
}
