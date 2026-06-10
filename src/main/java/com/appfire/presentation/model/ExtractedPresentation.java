package com.appfire.presentation.model;

import org.apache.poi.xslf.usermodel.XMLSlideShow;

public record ExtractedPresentation(
        PresentationBlueprint blueprint,
        XMLSlideShow slideShow) {
}
