package com.appfire.presentation.llm;

import java.util.concurrent.ThreadLocalRandom;

final class LlmRetrySupport {

    static final long BASE_BACKOFF_MS = 1_000L;
    static final long MAX_BACKOFF_MS = 30_000L;

    private LlmRetrySupport() {}

    static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted during LLM retry backoff", e);
        }
    }

    static long nextExponentialBackoff(long currentBackoff) {
        return Math.min(currentBackoff * 2, MAX_BACKOFF_MS);
    }

    static long jitteredDelay(long baseDelayMs) {
        long jitter = ThreadLocalRandom.current().nextLong(0, Math.max(1, baseDelayMs / 4 + 1));
        return Math.min(baseDelayMs + jitter, MAX_BACKOFF_MS);
    }

    static String truncate(String text, int maxLen) {
        if (text == null) {
            return "";
        }
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}
