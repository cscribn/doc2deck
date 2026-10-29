package com.appfire.presentation.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import java.io.IOException;

class GoogleGenAiErrorClassifierTest {

    @Test
    void classifies429AsExponentialRetry() {
        var classification = GoogleGenAiErrorClassifier.classify(new RuntimeException("HTTP 429 Too Many Requests"));
        assertEquals(GoogleGenAiErrorClassifier.FailureKind.RETRY_EXP_BACKOFF, classification.kind());
        assertEquals(429, classification.httpStatus());
    }

    @Test
    void classifies400AsNonRetryable() {
        var classification = GoogleGenAiErrorClassifier.classify(new RuntimeException("HTTP 400 Bad Request"));
        assertEquals(GoogleGenAiErrorClassifier.FailureKind.FAIL_NO_RETRY, classification.kind());
    }

    @Test
    void classifies403AsAuthFailure() {
        var classification = GoogleGenAiErrorClassifier.classify(new RuntimeException("HTTP 403 Forbidden"));
        assertEquals(GoogleGenAiErrorClassifier.FailureKind.FAIL_AUTH, classification.kind());
    }

    @Test
    void classifies503AsJitterRetry() {
        var classification = GoogleGenAiErrorClassifier.classify(new RuntimeException("HTTP 503 Service Unavailable"));
        assertEquals(GoogleGenAiErrorClassifier.FailureKind.RETRY_JITTER, classification.kind());
    }

    @Test
    void extractHttpStatusFindsCodeInMessage() {
        assertEquals(408, GoogleGenAiErrorClassifier.extractHttpStatus(new IOException("request failed with 408")));
    }
}
