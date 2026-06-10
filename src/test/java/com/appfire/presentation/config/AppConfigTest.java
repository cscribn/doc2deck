package com.appfire.presentation.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AppConfigTest {

    @Test
    void failsWhenApiKeyMissing() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, AppConfig::load);
        assertEquals(true, ex.getMessage().contains("GEMINI_API_KEY"));
    }
}
