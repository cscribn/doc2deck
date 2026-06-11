package com.appfire.presentation;

import com.appfire.presentation.config.AppConfig;
import com.appfire.presentation.extraction.DocxExtractor;
import com.appfire.presentation.images.ImageAcquisitionService;
import com.appfire.presentation.images.PresentationImageOptimizer;
import com.appfire.presentation.llm.GeminiClient;
import com.appfire.presentation.llm.PromptBuilder;
import com.appfire.presentation.llm.ResponseValidator;
import com.appfire.presentation.model.DocumentContent;
import com.appfire.presentation.model.PresentationContentResponse;
import com.appfire.presentation.model.TemplateScanResult;
import com.appfire.presentation.template.EmbeddedFontCleaner;
import com.appfire.presentation.template.ImageInserter;
import com.appfire.presentation.template.OptionalPlaceholderCleaner;
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
        ConsoleProgress.step("Loading configuration...");
        AppConfig config = AppConfig.load();

        ObjectMapper objectMapper = new ObjectMapper();
        DocxExtractor docxExtractor = new DocxExtractor();
        TemplateScanner templateScanner = new TemplateScanner();
        PromptBuilder promptBuilder = new PromptBuilder();
        GeminiClient geminiClient = new GeminiClient(config, objectMapper);
        ResponseValidator validator = new ResponseValidator();
        PptxTemplateReplacer templateReplacer = new PptxTemplateReplacer();
        ImageAcquisitionService imageService = new ImageAcquisitionService(
                config.pexelsApiKey(), config.imageCacheDir(), objectMapper);
        OptionalPlaceholderCleaner optionalCleaner = new OptionalPlaceholderCleaner();
        PresentationImageOptimizer imageOptimizer = new PresentationImageOptimizer(
                config.imageOptimizationEnabled(), config.imageJpegQuality());
        EmbeddedFontCleaner fontCleaner = new EmbeddedFontCleaner(config.fontCleanupEnabled());
        ImageInserter imageInserter = new ImageInserter(imageOptimizer, fontCleaner);

        ConsoleProgress.step("Extracting content from source document...");
        DocumentContent document = docxExtractor.extract(config.sourceDocxPath());

        ConsoleProgress.step("Scanning template for placeholders...");
        TemplateScanResult scan = templateScanner.scan(config.templatePptxPath());

        ConsoleProgress.step("Generating presentation content with Gemini...");
        String prompt = promptBuilder.build(document, scan);
        PresentationContentResponse response = geminiClient.generate(prompt);

        ConsoleProgress.step("Validating generated content...");
        ResponseValidator.ValidationResult validation = validator.validate(response, scan);
        if (!validation.passed()) {
            throw new IllegalStateException(
                    "Gemini response failed validation: " + validation.criticalFailures());
        }

        ConsoleProgress.step("Replacing text placeholders...");
        Path tempPptx = Files.createTempFile("presentation-text-", ".pptx");
        try {
            templateReplacer.replace(config.templatePptxPath(), response, tempPptx);

            ConsoleProgress.step("Cleaning empty optional sections...");
            optionalCleaner.clean(tempPptx, response);

            if (config.pexelsApiKey().isBlank()) {
                ConsoleProgress.step("Skipping image acquisition (PEXELS_API_KEY not set)...");
            } else {
                ConsoleProgress.step("Acquiring slide images...");
            }
            var imagePlan = imageService.acquire(response.imageQueries());

            ConsoleProgress.step("Inserting images, optimizing, writing presentation, and cleaning fonts...");
            imageInserter.insert(tempPptx, imagePlan, scan, config.outputPptxPath());
        } finally {
            Files.deleteIfExists(tempPptx);
        }

        ConsoleProgress.complete(config.outputPptxPath());
    }

    private static String resolutionFor(Exception e) {
        if (e instanceof IllegalStateException) {
            return "Fix configuration or input files and re-run ./gradlew run";
        }
        return "Review the error above, verify gemini CLI and inputs, then re-run ./gradlew run";
    }
}
