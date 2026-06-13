package com.appfire.presentation.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.appfire.presentation.TestFixtures;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class AppConfigTest {

    @Test
    void loadsWhenPrerequisitesExist() {
        assumeTrue(geminiCliAvailable(), "gemini CLI not available");
        assumeTrue(Files.exists(TestFixtures.resolveTemplatePptx()), "template PPTX not available");
        assumeTrue(Files.exists(TestFixtures.resolveSourceDocx()), "source DOCX not available");
        assumeTrue(Files.exists(Path.of("presentation-keys.properties")), "presentation-keys.properties not available");

        AppConfig config = AppConfig.load();

        assertEquals("gemini", config.geminiCliPath());
        assertEquals("gemini-3.1-flash-lite", config.geminiModel());
        assertEquals(Path.of("presentation-keys.properties"), config.presentationKeysPath());
        assertFalse(config.sourceDocxPaths().isEmpty());
    }

    private static boolean geminiCliAvailable() {
        try {
            Process process = new ProcessBuilder("gemini", "--version")
                    .redirectErrorStream(true)
                    .start();
            return process.waitFor(30, TimeUnit.SECONDS) && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
