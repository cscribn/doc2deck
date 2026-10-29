package com.appfire.presentation.integration;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.appfire.presentation.Application;
import com.appfire.presentation.TestFixtures;
import com.appfire.presentation.config.AiBackend;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

class PipelineIntegrationTest {

    @Test
    @EnabledIf("aiBackendAvailable")
    void runsFullPipeline() throws Exception {
        assumeTrue(TestFixtures.hasTemplatePptx(), "template.pptx not available");
        assumeTrue(TestFixtures.hasSourceDocx(), "source.docx not available");
        Application.run();
    }

    static boolean aiBackendAvailable() {
        String backend = System.getenv("AI_BACKEND");
        if (backend == null || backend.isBlank()) {
            return cursorCliAvailable();
        }
        AiBackend parsed = AiBackend.parse(backend);
        return switch (parsed) {
            case CURSOR_CLI -> cursorCliAvailable();
            case GOOGLE_GENAI -> googleGenaiAvailable();
        };
    }

    static boolean cursorCliAvailable() {
        try {
            Process process = new ProcessBuilder("agent", "--version")
                    .redirectErrorStream(true)
                    .start();
            return process.waitFor(30, TimeUnit.SECONDS) && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    static boolean googleGenaiAvailable() {
        String key = System.getenv("GOOGLE_API_KEY");
        return key != null && !key.isBlank();
    }
}
