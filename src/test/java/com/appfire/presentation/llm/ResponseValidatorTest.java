package com.appfire.presentation.llm;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.appfire.presentation.model.GenerationResponse;
import com.appfire.presentation.model.LayoutCatalog;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ResponseValidatorTest {

    private ResponseValidator validator;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        validator = new ResponseValidator(new LayoutCatalog(java.util.List.of()));
        objectMapper = new ObjectMapper();
    }

    @Test
    void acceptsValidResponse() throws IOException {
        GenerationResponse response = loadFixture("valid-response.json");
        ResponseValidator.ValidationResult result = validator.validate(response, 1);
        assertTrue(result.passed());
    }

    @Test
    void rejectsTooManyBullets() throws IOException {
        GenerationResponse response = loadFixture("invalid-too-many-bullets.json");
        ResponseValidator.ValidationResult result = validator.validate(response, 1);
        assertFalse(result.passed());
    }

    private GenerationResponse loadFixture(String name) throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(name)) {
            return objectMapper.readValue(in, GenerationResponse.class);
        }
    }
}
