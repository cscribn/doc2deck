package com.appfire.presentation.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.appfire.presentation.model.ContentStyle;
import com.appfire.presentation.model.GenerationResponse;
import com.appfire.presentation.model.PresentationBlueprint;
import com.appfire.presentation.model.SlideAction;
import com.appfire.presentation.model.SlideBlueprint;
import com.appfire.presentation.model.SlideDirective;
import com.appfire.presentation.model.ThemeBlueprint;
import java.util.List;
import org.junit.jupiter.api.Test;

class MetaSlideFilterTest {

    private final MetaSlideFilter filter = new MetaSlideFilter();

    @Test
    void skipsPresentationTipsSlide() {
        SlideDirective metaSlide = new SlideDirective(
                0, SlideAction.REPLACE, "Presentation Tips",
                List.of("Use large fonts"), null, null, List.of(),
                null, null, null, ContentStyle.BULLETS, false, null,
                List.of(), List.of(), false, com.appfire.presentation.model.ImagePosition.NONE);
        GenerationResponse input = new GenerationResponse(List.of(metaSlide), List.of());
        PresentationBlueprint blueprint = new PresentationBlueprint(
                List.of(new SlideBlueprint(0, "Title and Content", List.of(), "")),
                new ThemeBlueprint(List.of(), List.of()),
                new com.appfire.presentation.model.LayoutCatalog(List.of()));

        GenerationResponse result = filter.filter(input, blueprint);
        assertEquals(SlideAction.SKIP, result.slides().get(0).action());
    }

    @Test
    void keepsLegitimateContentSlide() {
        SlideDirective contentSlide = new SlideDirective(
                0, SlideAction.REPLACE, "API Architecture Overview",
                List.of("REST endpoints"), null, null, List.of(0),
                null, null, null, ContentStyle.BULLETS, false, null,
                List.of(), List.of(), false, com.appfire.presentation.model.ImagePosition.NONE);
        GenerationResponse input = new GenerationResponse(List.of(contentSlide), List.of());
        PresentationBlueprint blueprint = new PresentationBlueprint(
                List.of(new SlideBlueprint(0, "Title and Content", List.of(), "")),
                new ThemeBlueprint(List.of(), List.of()),
                new com.appfire.presentation.model.LayoutCatalog(List.of()));

        GenerationResponse result = filter.filter(input, blueprint);
        assertEquals(SlideAction.REPLACE, result.slides().get(0).action());
    }
}
