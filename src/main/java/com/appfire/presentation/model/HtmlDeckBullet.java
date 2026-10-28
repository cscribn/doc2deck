package com.appfire.presentation.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HtmlDeckBullet(String icon, String text) {
}
