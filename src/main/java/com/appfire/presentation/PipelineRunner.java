package com.appfire.presentation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!integration-test")
public class PipelineRunner implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(PipelineRunner.class);

    private final PipelineOrchestrator orchestrator;

    public PipelineRunner(PipelineOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @Override
    public void run(String... args) {
        try {
            orchestrator.runPipeline();
        } catch (Exception e) {
            LOG.error("Pipeline failed: {}. Resolution: {}", e.getMessage(), resolutionFor(e), e);
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private static String resolutionFor(Exception e) {
        if (e instanceof IllegalStateException) {
            return "Fix configuration or input files and re-run ./gradlew bootRun";
        }
        return "Review the error above, verify gemini CLI and inputs, then re-run ./gradlew bootRun";
    }
}
