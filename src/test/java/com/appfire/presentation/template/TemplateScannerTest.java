package com.appfire.presentation.template;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.appfire.presentation.TestFixtureGenerator;
import com.appfire.presentation.TestFixtures;
import com.appfire.presentation.model.PresentationKeys;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TemplateScannerTest {

    @BeforeAll
    static void ensureFixtures() throws Exception {
        TestFixtureGenerator.ensureFixtures();
    }

    @Test
    void findsPlaceholderKeysInTemplate() throws Exception {
        assumeTrue(TestFixtures.hasTemplatePptx(), "template.pptx not available");
        TemplateScanner scanner = new TemplateScanner();
        var result = scanner.scan(TestFixtures.resolveTemplatePptx());

        assertTrue(result.foundKeys().contains(PresentationKeys.SHORT_PROJECT_DESCRIPTION)
                || result.foundKeys().contains(PresentationKeys.PROBLEM_1)
                || !result.foundKeys().isEmpty());
    }
}
