package com.appfire.presentation.template;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.appfire.presentation.config.PresentationKeysConfigLoader;
import com.appfire.presentation.TestFixtureGenerator;
import com.appfire.presentation.TestFixtures;
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
        var keysConfig = PresentationKeysConfigLoader.load(Path.of("presentation-keys.example.properties"));
        var result = scanner.scan(TestFixtures.resolveTemplatePptx(), keysConfig);

        assertTrue(result.foundKeys().contains("shortProjectDescription")
                || result.foundKeys().contains("problem1")
                || !result.foundKeys().isEmpty());
    }
}
