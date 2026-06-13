package com.appfire.presentation.llm;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.appfire.presentation.config.PresentationKeysConfig;
import com.appfire.presentation.config.PresentationKeysConfigLoader;
import com.appfire.presentation.model.PresentationContentResponse;
import com.appfire.presentation.model.TemplateScanResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ResponseValidatorTest {

    private ResponseValidator validator;
    private ObjectMapper objectMapper;
    private PresentationKeysConfig keysConfig;

    @BeforeEach
    void setUp() {
        validator = new ResponseValidator();
        objectMapper = new ObjectMapper();
        keysConfig = PresentationKeysConfigLoader.load(Path.of("presentation-keys.example.properties"));
    }

    @Test
    void acceptsValidResponse() throws IOException {
        PresentationContentResponse response = loadFixture("valid-response.json");
        ResponseValidator.ValidationResult result = validator.validate(
                response, fullTemplateScan(), keysConfig, "");
        assertTrue(result.passed());
    }

    @Test
    void rejectsMissingRequiredKeys() throws IOException {
        PresentationContentResponse response = loadFixture("invalid-missing-keys.json");
        ResponseValidator.ValidationResult result = validator.validate(
                response, fullTemplateScan(), keysConfig, "");
        assertFalse(result.passed());
    }

    @Test
    void rejectsSingleWordImageQuery() throws IOException {
        PresentationContentResponse response = loadFixture("invalid-image-query.json");
        ResponseValidator.ValidationResult result = validator.validate(
                response, fullTemplateScan(), keysConfig, "");
        assertFalse(result.passed());
    }

    @Test
    void rejectsOverLimitTextKey() throws IOException {
        PresentationContentResponse response = loadFixture("valid-response.json");
        response.keys().put(
                "problem1",
                "one two three four five six seven eight nine ten eleven twelve thirteen fourteen fifteen sixteen");
        ResponseValidator.ValidationResult result = validator.validate(
                response, fullTemplateScan(), keysConfig, "");
        assertFalse(result.passed());
        assertTrue(result.criticalFailures().stream().anyMatch(msg -> msg.contains("problem1")));
    }

    @Test
    void warnsWhenShortDescriptionRepeatsTemplateTitle() throws IOException {
        PresentationContentResponse response = loadFixture("valid-response.json");
        response.keys().put(
                "shortProjectDescription",
                "Flow API Monorepo: unified API layer for services");
        ResponseValidator.ValidationResult result = validator.validate(
                response, fullTemplateScan(), keysConfig, "Flow API Monorepo");
        assertTrue(result.passed());
        assertTrue(result.advisories().stream()
                .anyMatch(msg -> msg.contains("Flow API Monorepo")));
    }

    private TemplateScanResult fullTemplateScan() {
        return new TemplateScanResult(keysConfig.configuredKeyNames(), java.util.List.of(), java.util.List.of());
    }

    private PresentationContentResponse loadFixture(String name) throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(name)) {
            return objectMapper.readValue(in, PresentationContentResponse.class);
        }
    }
}
