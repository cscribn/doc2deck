package com.appfire.presentation.template;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.appfire.presentation.model.PresentationKeys;
import org.junit.jupiter.api.Test;

class KeyContentLimitsTest {

    @Test
    void returnsConfiguredMaxWords() {
        assertEquals(10, KeyContentLimits.maxWordsFor(PresentationKeys.SHORT_PROJECT_DESCRIPTION));
        assertEquals(15, KeyContentLimits.maxWordsFor(PresentationKeys.PROBLEM_1));
        assertEquals(18, KeyContentLimits.maxWordsFor(PresentationKeys.SCALE));
        assertEquals(25, KeyContentLimits.maxWordsFor(PresentationKeys.ARCHITECTURE_APPROACH));
    }

    @Test
    void countsWordsIgnoringExtraWhitespace() {
        assertEquals(3, KeyContentLimits.countWords("  one   two three  "));
        assertEquals(0, KeyContentLimits.countWords("   "));
    }
}
