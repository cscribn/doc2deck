package com.appfire.presentation.model;

import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Set;

public record TemplateScanResult(
        Set<String> foundKeys,
        List<String> splitRunWarnings,
        List<ImageKeyAnchor> imageAnchors) {

    public TemplateScanResult {
        if (foundKeys == null) {
            foundKeys = Set.of();
        }
        if (splitRunWarnings == null) {
            splitRunWarnings = List.of();
        }
        if (imageAnchors == null) {
            imageAnchors = List.of();
        }
    }

    public record ImageKeyAnchor(
            String key,
            int slideIndex,
            int shapeId,
            Rectangle2D anchor) {
    }
}
