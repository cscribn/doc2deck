package com.appfire.presentation.template;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class WordCountTest {

    @Test
    void countsWordsIgnoringExtraWhitespace() {
        assertEquals(3, WordCount.count("  one   two three  "));
        assertEquals(0, WordCount.count("   "));
    }
}
