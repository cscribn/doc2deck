package com.appfire.presentation.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class TextSanitizerTest {

    @Test
    void stripsBulletPrefix() {
        assertEquals("Teams duplicate integration logic", TextSanitizer.sanitize("• Teams duplicate integration logic"));
        assertEquals("Point one", TextSanitizer.sanitize("- Point one"));
        assertEquals("Point two", TextSanitizer.sanitize("* Point two"));
        assertEquals("Fourth sprint", TextSanitizer.sanitize("1. Fourth sprint"));
    }

    @Test
    void returnsEmptyForBlankInput() {
        assertEquals("", TextSanitizer.sanitize(null));
        assertEquals("", TextSanitizer.sanitize("   "));
    }

    @Test
    void leavesPlainTextUnchanged() {
        assertEquals("No prefix here", TextSanitizer.sanitize("No prefix here"));
    }
}
