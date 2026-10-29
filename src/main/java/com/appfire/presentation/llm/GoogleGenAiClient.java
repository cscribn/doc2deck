package com.appfire.presentation.llm;

import com.appfire.presentation.config.AppConfig;
import com.appfire.presentation.model.HtmlDeckContentResponse;
import com.appfire.presentation.model.PresentationContentResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GoogleGenAiClient implements PresentationContentGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(GoogleGenAiClient.class);
    private static final int PROMPT_LOG_SNIPPET_LEN = 500;

    private final AppConfig config;
    private final LlmJsonResponseParser responseParser;
    private final com.google.genai.Client genaiClient;

    public GoogleGenAiClient(AppConfig config, LlmJsonResponseParser responseParser) {
        this.config = config;
        this.responseParser = responseParser;
        this.genaiClient = com.google.genai.Client.builder().apiKey(config.googleApiKey()).build();
    }

    @Override
    public PresentationContentResponse generate(String prompt) throws IOException {
        return invokeWithRetries(prompt, responseParser::parsePresentation);
    }

    @Override
    public HtmlDeckContentResponse generateHtmlDeck(String prompt) throws IOException {
        return invokeWithRetries(prompt, responseParser::parseHtmlDeck);
    }

    private <T> T invokeWithRetries(String prompt, ResponseParser<T> parser) throws IOException {
        int attempts = 0;
        long backoff = LlmRetrySupport.BASE_BACKOFF_MS;
        IOException lastError = null;
        while (true) {
            attempts++;
            try {
                String text = invokeOnce(prompt);
                return parser.parse(text);
            } catch (IOException e) {
                lastError = e;
                GoogleGenAiErrorClassifier.Classification classification = classifyFailure(e);
                if (attempts > config.llmMaxRetries() || !isRetryable(classification)) {
                    throw new IOException(buildFailureMessage(attempts, lastError), lastError);
                }
                logRetry(attempts, delayFor(classification, backoff), classification, e);
                LlmRetrySupport.sleep(delayFor(classification, backoff));
                if (classification.kind() == GoogleGenAiErrorClassifier.FailureKind.RETRY_EXP_BACKOFF) {
                    backoff = LlmRetrySupport.nextExponentialBackoff(backoff);
                }
            }
        }
    }

    private String invokeOnce(String prompt) throws IOException {
        com.google.genai.types.GenerateContentResponse response;
        try {
            response = genaiClient.models.generateContent(config.googleGenaiModel(), prompt, null);
        } catch (RuntimeException e) {
            throw mapRuntimeFailure(prompt, e);
        }
        String text = extractResponseText(response);
        if (text.isBlank()) {
            throw new IOException("Google GenAI returned empty text for model " + config.googleGenaiModel());
        }
        return text;
    }

    static String extractResponseText(com.google.genai.types.GenerateContentResponse response) {
        if (response == null) {
            return "";
        }
        String quickText = response.text();
        if (quickText != null && !quickText.isBlank()) {
            return quickText.trim();
        }
        var parts = new StringBuilder();
        if (response.candidates().isPresent()) {
            for (var candidate : response.candidates().get()) {
                if (candidate.content().isEmpty()) {
                    continue;
                }
                var content = candidate.content().get();
                if (content.parts().isEmpty()) {
                    continue;
                }
                for (var part : content.parts().get()) {
                    if (part.text().isPresent() && !part.text().get().isBlank()) {
                        parts.append(part.text().get());
                    }
                }
            }
        }
        return parts.toString().trim();
    }

    private IOException mapRuntimeFailure(String prompt, RuntimeException e) {
        GoogleGenAiErrorClassifier.Classification classification = GoogleGenAiErrorClassifier.classify(e);
        if (classification.kind() == GoogleGenAiErrorClassifier.FailureKind.FAIL_AUTH) {
            return new IOException(
                    "Google GenAI authentication failed. Verify GOOGLE_API_KEY and re-run ./gradlew bootRun", e);
        }
        if (classification.kind() == GoogleGenAiErrorClassifier.FailureKind.FAIL_NO_RETRY) {
            LOG.error(
                    "Google GenAI request rejected (model={}, promptSnippet={})",
                    config.googleGenaiModel(),
                    LlmRetrySupport.truncate(prompt, PROMPT_LOG_SNIPPET_LEN));
            return new IOException(
                    "Google GenAI request rejected for model " + config.googleGenaiModel() + ": " + e.getMessage(),
                    e);
        }
        return new IOException(e.getMessage(), e);
    }

    private GoogleGenAiErrorClassifier.Classification classifyFailure(IOException e) {
        if (e.getCause() instanceof JsonProcessingException) {
            return GoogleGenAiErrorClassifier.classifyMalformedJson();
        }
        if (e.getMessage() != null && e.getMessage().contains("empty text")) {
            return GoogleGenAiErrorClassifier.classifyEmptyResponse();
        }
        return GoogleGenAiErrorClassifier.classify(e);
    }

    private boolean isRetryable(GoogleGenAiErrorClassifier.Classification classification) {
        return classification.kind() == GoogleGenAiErrorClassifier.FailureKind.RETRY_EXP_BACKOFF
                || classification.kind() == GoogleGenAiErrorClassifier.FailureKind.RETRY_JITTER;
    }

    private long delayFor(GoogleGenAiErrorClassifier.Classification classification, long backoff) {
        if (classification.kind() == GoogleGenAiErrorClassifier.FailureKind.RETRY_EXP_BACKOFF) {
            return backoff;
        }
        return LlmRetrySupport.jitteredDelay(backoff);
    }

    private void logRetry(
            int attempt,
            long delayMs,
            GoogleGenAiErrorClassifier.Classification classification,
            IOException error) {
        if (!isJerbsVerbose()) {
            return;
        }
        System.err.println(
                "Google GenAI retry attempt " + attempt + " in " + delayMs + "ms (status="
                        + classification.httpStatus() + "): " + error.getMessage());
    }

    private static boolean isJerbsVerbose() {
        return "true".equalsIgnoreCase(System.getenv("JERBS_VERBOSE"));
    }

    private String buildFailureMessage(int attempts, IOException last) {
        return "Google GenAI failed after " + attempts + " attempts: " + last.getMessage()
                + ". Verify GOOGLE_API_KEY, model " + config.googleGenaiModel()
                + ", and re-run ./gradlew bootRun";
    }

    @FunctionalInterface
    private interface ResponseParser<T> {
        T parse(String text) throws IOException;
    }
}
