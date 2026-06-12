package com.appfire.presentation.template;

import com.appfire.presentation.model.PresentationContentResponse;
import com.appfire.presentation.model.PresentationKeys;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.PresentationMLPackage;
import org.docx4j.openpackaging.parts.PresentationML.MainPresentationPart;
import org.docx4j.openpackaging.parts.PresentationML.SlidePart;
import org.pptx4j.Pptx4jException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.xml.bind.JAXBException;

public final class PptxTemplateReplacer {

    private static final Logger LOG = LoggerFactory.getLogger(PptxTemplateReplacer.class);

    public void replace(Path templatePath, PresentationContentResponse response, Path outputPath)
            throws IOException {
        Path writePath = outputPath;
        Path tempOutput = null;
        if (isSamePath(templatePath, outputPath)) {
            tempOutput = Files.createTempFile("pptx-replace-", ".pptx");
            writePath = tempOutput;
        }
        try {
            PresentationMLPackage pptPackage = PresentationMLPackage.load(new File(templatePath.toString()));
            MainPresentationPart mainPart = pptPackage.getMainPresentationPart();
            Map<String, String> replacements = buildTextReplacements(response);

            List<SlidePart> slideParts = mainPart.getSlideParts();
            for (SlidePart slidePart : slideParts) {
                slidePart.variableReplace(replacements);
            }

            pptPackage.save(new File(writePath.toString()));
            LOG.info("Text replacement complete: {} slide(s) processed", slideParts.size());
        } catch (Docx4JException | Pptx4jException | JAXBException e) {
            if (tempOutput != null) {
                Files.deleteIfExists(tempOutput);
            }
            throw new IOException(
                    "Failed to replace template placeholders. Ensure each ${key} is in a single text run "
                            + "and re-run ./gradlew run",
                    e);
        }
        if (tempOutput != null) {
            Files.move(tempOutput, outputPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static boolean isSamePath(Path left, Path right) {
        return left.toAbsolutePath().normalize().equals(right.toAbsolutePath().normalize());
    }

    private Map<String, String> buildTextReplacements(PresentationContentResponse response) {
        Map<String, String> replacements = new HashMap<>();
        for (String key : PresentationKeys.textKeys()) {
            if (PresentationKeys.isImageKey(key)) {
                continue;
            }
            String value = response.keys().get(key);
            if (!PresentationKeys.isPopulated(key, value)) {
                continue;
            }
            String sanitized = TextSanitizer.sanitize(value);
            if (sanitized.isBlank()) {
                continue;
            }
            replacements.put(key, ContentLengthEnforcer.enforce(key, sanitized));
        }
        return replacements;
    }
}
