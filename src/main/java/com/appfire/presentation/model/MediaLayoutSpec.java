package com.appfire.presentation.model;

public record MediaLayoutSpec(
        String layoutName,
        ImagePosition imagePosition,
        double mediaX,
        double mediaY,
        double mediaWidth,
        double mediaHeight,
        double textX,
        double textY,
        double textWidth,
        double textHeight,
        boolean useTitleForText) {

    public boolean hasMediaRegion() {
        return mediaWidth > 0 && mediaHeight > 0;
    }
}
