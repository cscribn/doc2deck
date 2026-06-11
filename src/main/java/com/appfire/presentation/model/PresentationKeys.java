package com.appfire.presentation.model;

import java.util.List;
import java.util.Set;

public final class PresentationKeys {

    public static final String SHORT_PROJECT_DESCRIPTION = "shortProjectDescription";
    public static final String PROBLEM_1 = "problem1";
    public static final String PROBLEM_2 = "problem2";
    public static final String PERSONA = "persona";
    public static final String CURRENT_SOLUTION_1 = "currentSolution1";
    public static final String CURRENT_SOLUTION_2 = "currentSolution2";
    public static final String SCALE = "scale";
    public static final String WHY_NOW = "whyNow";
    public static final String IF_UNSOLVED = "ifUnsolved";
    public static final String ARCHITECTURE_APPROACH = "architectureApproach";
    public static final String APPROACH_REASONS = "approachReasons";
    public static final String PERSONA_BETTERMENT = "personaBetterment";
    public static final String APPFIRE_BETTERMENT = "appfireBetterment";
    public static final String ESTIMATED_IMPACT = "estimatedImpact";
    public static final String CURRENT_STATE = "currentState";
    public static final String SPRINTS_TO_DELIVER = "sprintsToDeliver";
    public static final String NON_DEV_COSTS = "nonDevCosts";

    public static final String PROBLEM_SOLVING_IMG = "problemSolvingImg";
    public static final String ARCHITECTURE_APPROACH_IMG = "architectureApproachImg";
    public static final String VALUE_IMPACT_IMG = "valueImpactImg";

    private static final List<String> TEXT_KEYS = List.of(
            SHORT_PROJECT_DESCRIPTION,
            PROBLEM_1,
            PROBLEM_2,
            PERSONA,
            CURRENT_SOLUTION_1,
            CURRENT_SOLUTION_2,
            SCALE,
            WHY_NOW,
            IF_UNSOLVED,
            ARCHITECTURE_APPROACH,
            APPROACH_REASONS,
            PERSONA_BETTERMENT,
            APPFIRE_BETTERMENT,
            ESTIMATED_IMPACT,
            CURRENT_STATE,
            SPRINTS_TO_DELIVER,
            NON_DEV_COSTS);

    private static final List<String> IMAGE_KEYS = List.of(
            PROBLEM_SOLVING_IMG,
            ARCHITECTURE_APPROACH_IMG,
            VALUE_IMPACT_IMG);

    private static final List<String> REQUIRED_TEXT_KEYS = List.of(
            SHORT_PROJECT_DESCRIPTION,
            PROBLEM_1,
            PROBLEM_2,
            PERSONA,
            CURRENT_SOLUTION_1,
            CURRENT_SOLUTION_2,
            SCALE,
            WHY_NOW,
            IF_UNSOLVED,
            ARCHITECTURE_APPROACH,
            APPROACH_REASONS,
            PERSONA_BETTERMENT,
            APPFIRE_BETTERMENT,
            ESTIMATED_IMPACT,
            CURRENT_STATE,
            SPRINTS_TO_DELIVER);

    private PresentationKeys() {
    }

    public static List<String> textKeys() {
        return TEXT_KEYS;
    }

    public static List<String> imageKeys() {
        return IMAGE_KEYS;
    }

    public static List<String> requiredTextKeys() {
        return REQUIRED_TEXT_KEYS;
    }

    public static List<String> requiredKeys() {
        return List.of(
                SHORT_PROJECT_DESCRIPTION,
                PROBLEM_1,
                PROBLEM_2,
                PERSONA,
                CURRENT_SOLUTION_1,
                CURRENT_SOLUTION_2,
                SCALE,
                WHY_NOW,
                IF_UNSOLVED,
                ARCHITECTURE_APPROACH,
                APPROACH_REASONS,
                PERSONA_BETTERMENT,
                APPFIRE_BETTERMENT,
                ESTIMATED_IMPACT,
                CURRENT_STATE,
                SPRINTS_TO_DELIVER,
                PROBLEM_SOLVING_IMG,
                ARCHITECTURE_APPROACH_IMG,
                VALUE_IMPACT_IMG);
    }

    public static Set<String> allKeyNames() {
        return Set.copyOf(List.of(
                SHORT_PROJECT_DESCRIPTION,
                PROBLEM_1,
                PROBLEM_2,
                PERSONA,
                CURRENT_SOLUTION_1,
                CURRENT_SOLUTION_2,
                SCALE,
                WHY_NOW,
                IF_UNSOLVED,
                ARCHITECTURE_APPROACH,
                APPROACH_REASONS,
                PERSONA_BETTERMENT,
                APPFIRE_BETTERMENT,
                ESTIMATED_IMPACT,
                CURRENT_STATE,
                SPRINTS_TO_DELIVER,
                NON_DEV_COSTS,
                PROBLEM_SOLVING_IMG,
                ARCHITECTURE_APPROACH_IMG,
                VALUE_IMPACT_IMG));
    }

    public static boolean isImageKey(String key) {
        return IMAGE_KEYS.contains(key);
    }

    public static List<String> optionalTextKeys() {
        return List.of(NON_DEV_COSTS);
    }

    public static boolean isPopulated(String key, String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String sanitized = value.trim();
        if (sanitized.equalsIgnoreCase(key)) {
            return false;
        }
        if (sanitized.equals("${" + key + "}")) {
            return false;
        }
        return true;
    }
}
