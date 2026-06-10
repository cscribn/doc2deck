package com.appfire.presentation.integration;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.appfire.presentation.Application;
import com.appfire.presentation.TestFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

class PipelineIntegrationTest {

    @Test
    @EnabledIfEnvironmentVariable(named = "GEMINI_API_KEY", matches = ".+")
    void runsFullPipeline() throws Exception {
        assumeTrue(TestFixtures.hasSourcePptx(), "source.pptx not available");
        assumeTrue(TestFixtures.hasSourceDocx(), "source.docx not available");
        Application.run();
    }
}
