package com.appfire.presentation.template;

import java.util.regex.Pattern;

public final class TextSanitizer {

    private static final Pattern LEADING_BULLET_PREFIX = Pattern.compile(
            "^\\s*(?:[•\\-*◦·▪▸►]\\s*|\\d+[.)]\\s*)+");

    private TextSanitizer() {
    }

    public static String sanitize(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String trimmed = text.trim();
        String withoutPrefix = LEADING_BULLET_PREFIX.matcher(trimmed).replaceFirst("").trim();
        return withoutPrefix.isBlank() ? "" : withoutPrefix;
    }
}
