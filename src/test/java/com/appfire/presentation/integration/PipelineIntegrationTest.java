package com.appfire.presentation.integration;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.appfire.presentation.Application;
import com.appfire.presentation.TestFixtures;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

class PipelineIntegrationTest {

    @Test
    @EnabledIf("geminiCliAvailable")
    void runsFullPipeline() throws Exception {
        assumeTrue(TestFixtures.hasSourcePptx(), "source.pptx not available");
        assumeTrue(TestFixtures.hasSourceDocx(), "source.docx not available");
        Application.run();
    }

    static boolean geminiCliAvailable() {
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
