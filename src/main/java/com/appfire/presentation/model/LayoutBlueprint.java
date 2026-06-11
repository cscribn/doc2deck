package com.appfire.presentation.model;

public record LayoutBlueprint(
        String name,
        boolean hasTitle,
        boolean hasBody,
        boolean hasPicture,
        boolean hasTwoColumns,
        int bodyPlaceholderCount) {
}
