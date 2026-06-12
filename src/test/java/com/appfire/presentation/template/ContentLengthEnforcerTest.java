package com.appfire.presentation.template;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.appfire.presentation.model.PresentationKeys;
import org.junit.jupiter.api.Test;

class ContentLengthEnforcerTest {

    @Test
    void leavesShortValuesUnchanged() {
        String value = "Short hook fragment";
        assertEquals(value, ContentLengthEnforcer.enforce(PresentationKeys.SHORT_PROJECT_DESCRIPTION, value));
    }

    @Test
    void truncatesAtWordBoundary() {
        String value = "one two three four five six seven eight nine ten eleven twelve";
        assertEquals(
                "one two three four five six seven eight nine ten",
                ContentLengthEnforcer.truncateToWordLimit(value, 10));
    }

    @Test
    void enforceUsesKeySpecificLimit() {
        String value = "word ".repeat(20).trim();
        String enforced = ContentLengthEnforcer.enforce(PresentationKeys.PROBLEM_1, value);
        assertEquals(15, KeyContentLimits.countWords(enforced));
    }
}
