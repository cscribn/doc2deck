package com.appfire.presentation.model;

public record ShapeBlueprint(
        String shapeId,
        String placeholderType,
        String currentText,
        String fontFamily,
        Double fontSizePt,
        String colorHex) {
}
