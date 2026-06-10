package com.appfire.presentation.rehydration;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.appfire.presentation.TestFixtures;
import com.appfire.presentation.extraction.PptxExtractor;
import com.appfire.presentation.model.ExtractedPresentation;
import com.appfire.presentation.model.GenerationResponse;
import com.appfire.presentation.model.SlideAction;
import com.appfire.presentation.model.SlideDirective;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import com.appfire.presentation.TestFixtureExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

@ExtendWith(TestFixtureExtension.class)
class PptxRehydratorTest {

    @Test
    void writesOutputFromValidResponse(@TempDir Path tempDir) throws Exception {
        assumeTrue(TestFixtures.hasSourcePptx(), "source.pptx not available");

        PptxExtractor extractor = new PptxExtractor();
        ExtractedPresentation extracted = extractor.extract(TestFixtures.resolveSourcePptx());

        ObjectMapper mapper = new ObjectMapper();
        GenerationResponse response;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("valid-response.json")) {
            response = mapper.readValue(in, GenerationResponse.class);
        }

        SlideDirective directive = new SlideDirective(
                0, SlideAction.REPLACE, "Rehydrated Title",
                java.util.List.of("Bullet A"), null, null, java.util.List.of(0), null, null);
        response = new GenerationResponse(java.util.List.of(directive), java.util.List.of());

        Path output = tempDir.resolve("out.pptx");
        new PptxRehydrator().rehydrate(extracted, response, output);
        assertTrue(Files.exists(output));
        assertTrue(Files.size(output) > 0);
    }
}
