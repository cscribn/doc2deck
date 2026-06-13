package com.appfire.presentation.template;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ContentLengthEnforcerTest {

    @Test
    void leavesShortValuesUnchanged() {
        String value = "Short hook fragment";
        assertEquals(value, ContentLengthEnforcer.enforce("shortProjectDescription", value, 10));
    }

    @Test
    void truncatesAtWordBoundary() {
        String value = "one two three four five six seven eight nine ten eleven twelve";
        assertEquals(
                "one two three four five six seven eight nine ten",
                ContentLengthEnforcer.truncateToWordLimit(value, 10));
    }

    @Test
    void enforceUsesProvidedLimit() {
        String value = "word ".repeat(20).trim();
        String enforced = ContentLengthEnforcer.enforce("problem1", value, 15);
        assertEquals(15, WordCount.count(enforced));
    }
}
