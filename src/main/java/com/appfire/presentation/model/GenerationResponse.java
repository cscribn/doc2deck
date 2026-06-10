package com.appfire.presentation.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GenerationResponse(
        List<SlideDirective> slides,
        List<String> warnings) {

    public GenerationResponse {
        if (warnings == null) {
            warnings = List.of();
        }
    }
}
