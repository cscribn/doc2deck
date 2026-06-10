package com.appfire.presentation.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SlideDirective(
        int slideIndex,
        SlideAction action,
        String title,
        List<String> bullets,
        String bodyText,
        String notes,
        List<Integer> sourceRefs,
        Double titleFontSizePt,
        Double bodyFontSizePt) {

    public SlideDirective {
        if (bullets == null) {
            bullets = List.of();
        }
        if (sourceRefs == null) {
            sourceRefs = List.of();
        }
    }
}
