package com.appfire.presentation.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ImagePosition {
    NONE,
    LEFT,
    RIGHT,
    TOP,
    FULL;

    @JsonCreator
    public static ImagePosition fromString(String value) {
        if (value == null || value.isBlank()) {
            return NONE;
        }
        return switch (value.trim().toLowerCase()) {
            case "left" -> LEFT;
            case "right" -> RIGHT;
            case "top" -> TOP;
            case "full" -> FULL;
            default -> NONE;
        };
    }
}
