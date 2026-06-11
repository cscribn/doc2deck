package com.appfire.presentation.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ContentStyle {
    BULLETS,
    BODY,
    TITLE_ONLY,
    TWO_COLUMN;

    @JsonCreator
    public static ContentStyle fromString(String value) {
        if (value == null || value.isBlank()) {
            return BULLETS;
        }
        return switch (value.trim().toLowerCase()) {
            case "bullets" -> BULLETS;
            case "body" -> BODY;
            case "titleonly", "title_only", "title-only" -> TITLE_ONLY;
            case "twocolumn", "two_column", "two-column" -> TWO_COLUMN;
            default -> BULLETS;
        };
    }
}
