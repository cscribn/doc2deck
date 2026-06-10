package com.appfire.presentation.llm;

import com.appfire.presentation.config.AppConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.appfire.presentation.model.GenerationResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GeminiClient {

    private static final Logger LOG = LoggerFactory.getLogger(GeminiClient.class);
    private static final long BASE_BACKOFF_MS = 1_000L;
    private static final long MAX_BACKOFF_MS = 30_000L;

    private final AppConfig config;
    private final ObjectMapper objectMapper;

    public GeminiClient(AppConfig config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
    }

    public GenerationResponse generate(String prompt) throws IOException {
        Client client = Client.builder().apiKey(config.geminiApiKey()).build();
        GenerateContentConfig genConfig = GenerateContentConfig.builder()
                .responseMimeType("application/json")
                .build();

        int attempts = 0;
        long backoff = BASE_BACKOFF_MS;
        while (true) {
            try {
                GenerateContentResponse response = client.models.generateContent(
                        config.geminiModel(), prompt, genConfig);
                return parseResponse(response.text());
            } catch (RuntimeException e) {
                attempts++;
                if (attempts > config.geminiMaxRetries() || !isRetryable(e)) {
                    throw new IOException(
                            "Gemini API call failed after " + attempts + " attempts. "
                                    + "Verify GEMINI_API_KEY and network connectivity, then re-run ./gradlew run",
                            e);
                }
                LOG.warn("Gemini call failed (attempt {}). Retrying in {}ms. Resolution: check rate limits.",
                        attempts, backoff);
                sleep(backoff);
                backoff = Math.min(backoff * 2, MAX_BACKOFF_MS);
            }
        }
    }

    private GenerationResponse parseResponse(String text) throws IOException {
        String json = stripMarkdownFences(text);
        return objectMapper.readValue(json, GenerationResponse.class);
    }

    private String stripMarkdownFences(String text) {
        if (text == null) {
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

    private boolean isRetryable(RuntimeException e) {
        String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        return message.contains("rate") || message.contains("429")
                || message.contains("unavailable") || message.contains("timeout");
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted during Gemini retry backoff", e);
        }
    }
}
