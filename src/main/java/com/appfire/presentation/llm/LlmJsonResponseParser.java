package com.appfire.presentation.llm;

import com.appfire.presentation.model.HtmlDeckContentResponse;
import com.appfire.presentation.model.PresentationContentResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.springframework.stereotype.Component;

@Component
public final class LlmJsonResponseParser {

    private final ObjectMapper objectMapper;

    public LlmJsonResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public PresentationContentResponse parsePresentation(String modelText) throws IOException {
        return objectMapper.readValue(stripMarkdownFences(modelText), PresentationContentResponse.class);
    }

    public HtmlDeckContentResponse parseHtmlDeck(String modelText) throws IOException {
        return objectMapper.readValue(stripMarkdownFences(modelText), HtmlDeckContentResponse.class);
    }

    static String stripMarkdownFences(String text) {
        if (text == null || text.isBlank()) {
            return "{}";
        }
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                return trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return trimmed;
    }
}
