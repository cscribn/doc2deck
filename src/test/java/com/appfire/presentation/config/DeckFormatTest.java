package com.appfire.presentation.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DeckFormatTest {

    @Test
    void parsesPptxDefaults() {
        assertEquals(DeckFormat.PPTX, DeckFormat.parse(null));
        assertEquals(DeckFormat.PPTX, DeckFormat.parse("pptx"));
        assertEquals(DeckFormat.HTML, DeckFormat.parse("html"));
    }
}
