package com.appfire.presentation.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HtmlDeckSlide(
        String layout,
        int milestone,
        String title,
        String kicker,
        String headline,
        String subline,
        List<HtmlDeckBullet> bullets,
        String imageQuery,
        List<String> notes,
        String ai) {

    public HtmlDeckSlide {
        if (bullets == null) {
            bullets = List.of();
        }
        if (notes == null) {
            notes = List.of();
        }
    }
}
