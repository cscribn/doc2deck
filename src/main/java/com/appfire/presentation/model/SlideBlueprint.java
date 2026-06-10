package com.appfire.presentation.model;

import java.util.List;

public record SlideBlueprint(
        int slideIndex,
        String layoutName,
        List<ShapeBlueprint> shapes,
        String notes) {
}
