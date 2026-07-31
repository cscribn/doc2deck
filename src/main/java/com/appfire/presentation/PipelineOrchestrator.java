package com.appfire.presentation;

import com.appfire.presentation.config.AppConfig;
import com.appfire.presentation.config.PresentationKeysConfig;
import com.appfire.presentation.extraction.DocxExtractor;
import com.appfire.presentation.images.ImageAcquisitionService;
import com.appfire.presentation.llm.GeminiClient;
import com.appfire.presentation.llm.PromptBuilder;
import com.appfire.presentation.llm.ResponseValidator;
import com.appfire.presentation.model.DocumentContent;
import com.appfire.presentation.model.PresentationContentResponse;
import com.appfire.presentation.model.TemplateScanResult;
import com.appfire.presentation.template.ImageInserter;
import com.appfire.presentation.template.OptionalPlaceholderCleaner;
import com.appfire.presentation.template.PptxLayoutNormalizer;
import com.appfire.presentation.template.PptxTemplateReplacer;
import com.appfire.presentation.template.TemplateScanner;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PipelineOrchestrator {

    private static final Logger LOG = LoggerFactory.getLogger(PipelineOrchestrator.class);

    private final AppConfig config;
    private final PresentationKeysConfig keysConfig;
    private final DocxExtractor docxExtractor;
    private final TemplateScanner templateScanner;
    private final PromptBuilder promptBuilder;
    private final GeminiClient geminiClient;
    private final ResponseValidator validator;
    private final PptxTemplateReplacer templateReplacer;
    private final ImageAcquisitionService imageService;
    private final OptionalPlaceholderCleaner optionalCleaner;
    private final PptxLayoutNormalizer layoutNormalizer;
    private final ImageInserter imageInserter;

    public PipelineOrchestrator(
            AppConfig config,
            PresentationKeysConfig keysConfig,
            DocxExtractor docxExtractor,
            TemplateScanner templateScanner,
            PromptBuilder promptBuilder,
            GeminiClient geminiClient,
            ResponseValidator validator,
            PptxTemplateReplacer templateReplacer,
            ImageAcquisitionService imageService,
            OptionalPlaceholderCleaner optionalCleaner,
            PptxLayoutNormalizer layoutNormalizer,
            ImageInserter imageInserter) {
        this.config = config;
        this.keysConfig = keysConfig;
        this.docxExtractor = docxExtractor;
        this.templateScanner = templateScanner;
        this.promptBuilder = promptBuilder;
        this.geminiClient = geminiClient;
        this.validator = validator;
        this.templateReplacer = templateReplacer;
        this.imageService = imageService;
        this.optionalCleaner = optionalCleaner;
        this.layoutNormalizer = layoutNormalizer;
        this.imageInserter = imageInserter;
    }

    public void runPipeline() throws Exception {
        ConsoleProgress.step("Loading configuration...");

        ConsoleProgress.step("Extracting content from source documents...");
        DocumentContent document = docxExtractor.extractAll(config.sourceDocxPaths());

        ConsoleProgress.step("Scanning template for placeholders...");
        TemplateScanResult scan = templateScanner.scan(config.templatePptxPath(), keysConfig);
        keysConfig.validateAgainstTemplate(scan).forEach(msg -> LOG.warn("Presentation keys advisory: {}", msg));

        ConsoleProgress.step("Generating presentation content with Gemini...");
        String prompt = promptBuilder.build(document, scan);
        PresentationContentResponse response = geminiClient.generate(prompt);

        ConsoleProgress.step("Validating generated content...");
        ResponseValidator.ValidationResult validation = validator.validate(response, scan, keysConfig);
        if (!validation.passed()) {
            throw new IllegalStateException(
                    "Gemini response failed validation: " + validation.criticalFailures());
        }

        ConsoleProgress.step("Preparing working copy and replacing text placeholders...");
        Path workingPptx = Files.createTempFile("presentation-working-", ".pptx");
        try {
            Files.copy(config.templatePptxPath(), workingPptx, StandardCopyOption.REPLACE_EXISTING);
            if (config.layoutNormalizeEnabled()) {
                ConsoleProgress.step("Hardening layout for cross-viewer compatibility...");
                layoutNormalizer.hardenStructure(workingPptx);
            }
            templateReplacer.replace(workingPptx, response, keysConfig, workingPptx);

            ConsoleProgress.step("Cleaning empty optional sections...");
            optionalCleaner.clean(workingPptx, response, keysConfig);

            if (config.layoutNormalizeEnabled()) {
                ConsoleProgress.step("Fitting text to slide layout...");
                layoutNormalizer.fitText(workingPptx);
            }

            if (config.pexelsApiKey().isBlank()) {
                ConsoleProgress.step("Skipping image acquisition (PEXELS_API_KEY not set)...");
            } else {
                ConsoleProgress.step("Acquiring slide images...");
            }
            var imagePlan = imageService.acquire(response.imageQueries(keysConfig.imageKeyNames()));

            ConsoleProgress.step("Inserting images, optimizing, writing presentation, and cleaning fonts...");
            imageInserter.insert(workingPptx, imagePlan, scan, config.outputPptxPath());
        } finally {
            Files.deleteIfExists(workingPptx);
        }

        ConsoleProgress.complete(config.outputPptxPath());
    }
}
