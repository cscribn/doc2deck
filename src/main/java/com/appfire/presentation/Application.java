package com.appfire.presentation;

import com.appfire.presentation.config.AppConfig;
import com.appfire.presentation.extraction.DocxExtractor;
import com.appfire.presentation.images.ImageAcquisitionService;
import com.appfire.presentation.llm.GeminiClient;
import com.appfire.presentation.llm.PromptBuilder;
import com.appfire.presentation.llm.ResponseValidator;
import com.appfire.presentation.model.DocumentContent;
import com.appfire.presentation.model.PresentationContentResponse;
import com.appfire.presentation.model.TemplateScanResult;
import com.appfire.presentation.template.ImageInserter;
import com.appfire.presentation.template.PptxTemplateReplacer;
import com.appfire.presentation.template.TemplateScanner;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
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
        LOG.info("Template PPTX: {}", config.templatePptxPath());
        LOG.info("Source DOCX: {}", config.sourceDocxPath());
        LOG.info("Output PPTX: {}", config.outputPptxPath());
        LOG.info("Gemini model: {}", config.geminiModel());

        ObjectMapper objectMapper = new ObjectMapper();
        DocxExtractor docxExtractor = new DocxExtractor();
        TemplateScanner templateScanner = new TemplateScanner();
        PromptBuilder promptBuilder = new PromptBuilder();
        GeminiClient geminiClient = new GeminiClient(config, objectMapper);
        ResponseValidator validator = new ResponseValidator();
        PptxTemplateReplacer templateReplacer = new PptxTemplateReplacer();
        ImageAcquisitionService imageService = new ImageAcquisitionService(
                config.pexelsApiKey(), config.imageCacheDir(), objectMapper);
        ImageInserter imageInserter = new ImageInserter();

        DocumentContent document = docxExtractor.extract(config.sourceDocxPath());
        TemplateScanResult scan = templateScanner.scan(config.templatePptxPath());

        String prompt = promptBuilder.build(document, scan);
        PresentationContentResponse response = geminiClient.generate(prompt);

        ResponseValidator.ValidationResult validation = validator.validate(response, scan);
        if (!validation.passed()) {
            throw new IllegalStateException(
                    "Gemini response failed validation: " + validation.criticalFailures());
        }

        Path tempPptx = Files.createTempFile("presentation-text-", ".pptx");
        try {
            templateReplacer.replace(config.templatePptxPath(), response, tempPptx);
            var imagePlan = imageService.acquire(response.imageQueries());
            imageInserter.insert(tempPptx, imagePlan, scan, config.outputPptxPath());
        } finally {
            Files.deleteIfExists(tempPptx);
        }

        LOG.info("Presentation generation complete.");
    }

    private static String resolutionFor(Exception e) {
        if (e instanceof IllegalStateException) {
            return "Fix configuration or input files and re-run ./gradlew run";
        }
        return "Check logs, verify gemini CLI and inputs, then re-run ./gradlew run";
    }
}
