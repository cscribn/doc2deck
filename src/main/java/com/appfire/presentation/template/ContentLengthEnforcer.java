package com.appfire.presentation.template;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ContentLengthEnforcer {

    private static final Logger LOG = LoggerFactory.getLogger(ContentLengthEnforcer.class);

    private ContentLengthEnforcer() {
    }

    public static String enforce(String key, String value, int maxWords) {
        if (value == null || value.isBlank()) {
            return value;
        }
        int wordCount = WordCount.count(value);
        if (wordCount <= maxWords) {
            return value;
        }
        String truncated = truncateToWordLimit(value, maxWords);
        LOG.warn(
                "Key '{}' exceeded {}-word limit (had {}). Truncated for layout safety.",
                key,
                maxWords,
                wordCount);
        return truncated;
    }

    static String truncateToWordLimit(String text, int maxWords) {
        String trimmed = text.trim();
        String[] words = trimmed.split("\\s+");
        if (words.length <= maxWords) {
            return trimmed;
        }
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < maxWords; index++) {
            if (index > 0) {
                builder.append(' ');
            }
            builder.append(words[index]);
        }
        return builder.toString();
    }
}
