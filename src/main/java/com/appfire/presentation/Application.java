package com.appfire.presentation;

import com.appfire.presentation.config.AppConfig;
import com.appfire.presentation.extraction.DocxExtractor;
import com.appfire.presentation.extraction.PptxExtractor;
import com.appfire.presentation.llm.GeminiClient;
import com.appfire.presentation.llm.PromptBuilder;
import com.appfire.presentation.llm.ResponseValidator;
import com.appfire.presentation.model.DocumentContent;
import com.appfire.presentation.model.ExtractedPresentation;
import com.appfire.presentation.model.GenerationResponse;
import com.appfire.presentation.rehydration.PptxRehydrator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Application {

    private static final Logger LOG = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        int exitCode = 0;
        try {
            run();
        } catch (Exception e) {
            LOG.error("Pipeline failed: {}. Resolution: {}", e.getMessage(), resolutionFor(e), e);
            exitCode = 1;
        }
        System.exit(exitCode);
    }

    public static void run() throws Exception {
        AppConfig config = AppConfig.load();
        LOG.info("Source PPTX: {}", config.sourcePptxPath());
        LOG.info("Source DOCX: {}", config.sourceDocxPath());
        LOG.info("Output PPTX: {}", config.outputPptxPath());
        LOG.info("Gemini model: {}", config.geminiModel());

        ObjectMapper objectMapper = new ObjectMapper();
        PptxExtractor pptxExtractor = new PptxExtractor();
        DocxExtractor docxExtractor = new DocxExtractor();
        PromptBuilder promptBuilder = new PromptBuilder();
        GeminiClient geminiClient = new GeminiClient(config, objectMapper);
        ResponseValidator validator = new ResponseValidator();
        PptxRehydrator rehydrator = new PptxRehydrator();

        ExtractedPresentation extracted = pptxExtractor.extract(config.sourcePptxPath());
        DocumentContent document = docxExtractor.extract(config.sourceDocxPath());

        String prompt = promptBuilder.build(extracted.blueprint(), document);
        GenerationResponse response = geminiClient.generate(prompt);

        ResponseValidator.ValidationResult validation = validator.validate(
                response, extracted.blueprint().slides().size());
        if (!validation.passed()) {
            throw new IllegalStateException(
                    "Gemini response failed validation: " + validation.criticalFailures());
        }

        rehydrator.rehydrate(extracted, response, config.outputPptxPath());
        LOG.info("Presentation generation complete.");
    }

    private static String resolutionFor(Exception e) {
        if (e instanceof IllegalStateException) {
            return "Fix configuration or input files and re-run ./gradlew run";
        }
        return "Check logs, verify API key and inputs, then re-run ./gradlew run";
    }
}
