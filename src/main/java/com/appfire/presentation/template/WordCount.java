package com.appfire.presentation.template;

public final class WordCount {

    private WordCount() {
    }

    public static int count(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }
}
