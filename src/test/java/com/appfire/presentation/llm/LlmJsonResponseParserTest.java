package com.appfire.presentation.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LlmJsonResponseParserTest {

    @Test
    void stripMarkdownFencesRemovesJsonFence() {
        String input = "```json\n{\"keys\":{}}\n```";
        assertEquals("{\"keys\":{}}", LlmJsonResponseParser.stripMarkdownFences(input));
    }

    @Test
    void stripMarkdownFencesReturnsEmptyObjectForBlank() {
        assertEquals("{}", LlmJsonResponseParser.stripMarkdownFences("   "));
    }

    @Test
    void stripMarkdownFencesLeavesPlainJson() {
        assertEquals("{\"a\":1}", LlmJsonResponseParser.stripMarkdownFences("{\"a\":1}"));
    }
}
