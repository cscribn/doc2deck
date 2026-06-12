package com.appfire.presentation.template;

import com.appfire.presentation.model.PresentationKeys;
import java.util.Map;

public final class KeyContentLimits {

    private static final int SHORT_FRAGMENT_WORDS = 10;
    private static final int SINGLE_FRAGMENT_WORDS = 15;
    private static final int TWO_FRAGMENT_WORDS = 18;
    private static final int THREE_FRAGMENT_WORDS = 25;

    private static final Map<String, Integer> MAX_WORDS_BY_KEY = Map.ofEntries(
            Map.entry(PresentationKeys.SHORT_PROJECT_DESCRIPTION, SHORT_FRAGMENT_WORDS),
            Map.entry(PresentationKeys.SPRINTS_TO_DELIVER, SHORT_FRAGMENT_WORDS),
            Map.entry(PresentationKeys.PROBLEM_1, SINGLE_FRAGMENT_WORDS),
            Map.entry(PresentationKeys.PROBLEM_2, SINGLE_FRAGMENT_WORDS),
            Map.entry(PresentationKeys.PERSONA, SINGLE_FRAGMENT_WORDS),
            Map.entry(PresentationKeys.CURRENT_SOLUTION_1, SINGLE_FRAGMENT_WORDS),
            Map.entry(PresentationKeys.CURRENT_SOLUTION_2, SINGLE_FRAGMENT_WORDS),
            Map.entry(PresentationKeys.SCALE, TWO_FRAGMENT_WORDS),
            Map.entry(PresentationKeys.WHY_NOW, TWO_FRAGMENT_WORDS),
            Map.entry(PresentationKeys.IF_UNSOLVED, TWO_FRAGMENT_WORDS),
            Map.entry(PresentationKeys.PERSONA_BETTERMENT, TWO_FRAGMENT_WORDS),
            Map.entry(PresentationKeys.APPFIRE_BETTERMENT, TWO_FRAGMENT_WORDS),
            Map.entry(PresentationKeys.CURRENT_STATE, TWO_FRAGMENT_WORDS),
            Map.entry(PresentationKeys.NON_DEV_COSTS, TWO_FRAGMENT_WORDS),
            Map.entry(PresentationKeys.ARCHITECTURE_APPROACH, THREE_FRAGMENT_WORDS),
            Map.entry(PresentationKeys.APPROACH_REASONS, THREE_FRAGMENT_WORDS),
            Map.entry(PresentationKeys.ESTIMATED_IMPACT, THREE_FRAGMENT_WORDS));

    private KeyContentLimits() {
    }

    public static int maxWordsFor(String key) {
        return MAX_WORDS_BY_KEY.getOrDefault(key, SINGLE_FRAGMENT_WORDS);
    }

    public static int countWords(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }
}
