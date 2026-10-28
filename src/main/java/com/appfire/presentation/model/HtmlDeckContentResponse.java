package com.appfire.presentation.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HtmlDeckContentResponse(
        String deckTitle,
        List<HtmlDeckSlide> slides,
        Map<String, List<Integer>> sourceRefs,
        List<String> warnings) {

    public HtmlDeckContentResponse {
        if (slides == null) {
            slides = List.of();
        }
        if (sourceRefs == null) {
            sourceRefs = Map.of();
        }
        if (warnings == null) {
            warnings = List.of();
        }
    }
}
