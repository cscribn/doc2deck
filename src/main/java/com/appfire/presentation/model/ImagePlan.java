package com.appfire.presentation.model;

import java.util.Map;

public record ImagePlan(Map<Integer, ResolvedImage> imagesBySlideIndex) {

    public ImagePlan {
        if (imagesBySlideIndex == null) {
            imagesBySlideIndex = Map.of();
        }
    }

    public ResolvedImage forSlide(int slideIndex) {
        return imagesBySlideIndex.get(slideIndex);
    }
}
