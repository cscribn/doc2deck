package com.appfire.presentation.model;

import java.util.List;

public record PresentationBlueprint(
        List<SlideBlueprint> slides,
        ThemeBlueprint theme) {
}
