package com.appfire.presentation.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.appfire.presentation.TestFixtures;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class AppConfigTest {

    @Test
    void loadsWhenPrerequisitesExist() {
        assumeTrue(geminiCliAvailable(), "gemini CLI not available");
        assumeTrue(Files.exists(TestFixtures.resolveSourcePptx()), "source PPTX not available");
        assumeTrue(Files.exists(TestFixtures.resolveSourceDocx()), "source DOCX not available");

        AppConfig config = AppConfig.load();

        assertEquals("gemini", config.geminiCliPath());
        assertEquals("gemini-3.1-flash-lite", config.geminiModel());
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
