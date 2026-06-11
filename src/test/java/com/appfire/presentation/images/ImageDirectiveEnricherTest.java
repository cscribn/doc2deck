package com.appfire.presentation.images;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.appfire.presentation.model.ContentStyle;
import com.appfire.presentation.model.GenerationResponse;
import com.appfire.presentation.model.ImagePosition;
import com.appfire.presentation.model.LayoutCatalog;
import com.appfire.presentation.model.MediaLayoutSpec;
import com.appfire.presentation.model.PresentationBlueprint;
import com.appfire.presentation.model.SlideAction;
import com.appfire.presentation.model.SlideDirective;
import com.appfire.presentation.model.ThemeBlueprint;
import java.util.List;
import org.junit.jupiter.api.Test;

class ImageDirectiveEnricherTest {

    private final LayoutCatalog catalog = new LayoutCatalog(List.of(), List.of(
            new MediaLayoutSpec("MAIN_POINT_1", ImagePosition.RIGHT,
                    360, 40, 320, 320, 72, 40, 280, 320, true),
            new MediaLayoutSpec("MAIN_POINT_1", ImagePosition.LEFT,
                    72, 40, 280, 320, 360, 40, 320, 320, true)));

    @Test
    void enrichesMostSlidesWhenGeminiOmitsImages() {
        ImageDirectiveEnricher enricher = new ImageDirectiveEnricher(catalog);
        List<SlideDirective> slides = List.of(
                slide(0, "Hook"),
                slide(1, "Architecture Overview"),
                slide(2, "API Design"),
                slide(3, "Next Steps"),
                slide(4, "Timeline"));
        GenerationResponse input = new GenerationResponse(slides, List.of());
        PresentationBlueprint blueprint = new PresentationBlueprint(
                List.of(), new ThemeBlueprint(List.of(), List.of()), catalog);

        GenerationResponse result = enricher.enrich(input, blueprint);
        long withImages = result.slides().stream()
                .filter(s -> Boolean.TRUE.equals(s.includeImage()))
                .count();
        assertTrue(withImages >= 3);
    }

    @Test
    void rotatesImagePositions() {
        ImageDirectiveEnricher enricher = new ImageDirectiveEnricher(catalog);
        List<SlideDirective> slides = List.of(
                slide(0, "A"), slide(1, "B"), slide(2, "C"), slide(3, "D"));
        GenerationResponse result = enricher.enrich(
                new GenerationResponse(slides, List.of()),
                new PresentationBlueprint(List.of(), new ThemeBlueprint(List.of(), List.of()), catalog));
        long positions = result.slides().stream()
                .filter(s -> Boolean.TRUE.equals(s.includeImage()))
                .map(SlideDirective::imagePosition)
                .distinct()
                .count();
        assertTrue(positions >= 2);
    }

    @Test
    void leavesResponseUnchangedWhenCoverageMet() {
        ImageDirectiveEnricher enricher = new ImageDirectiveEnricher(catalog);
        SlideDirective withImage = new SlideDirective(
                0, SlideAction.REPLACE, "A", List.of(), null, null, List.of(),
                null, null, null, ContentStyle.BULLETS, true, "tech abstract",
                List.of(), List.of(), false, ImagePosition.RIGHT);
        SlideDirective without = slide(1, "B");
        GenerationResponse input = new GenerationResponse(List.of(withImage, without), List.of());
        PresentationBlueprint blueprint = new PresentationBlueprint(
                List.of(), new ThemeBlueprint(List.of(), List.of()), catalog);

        GenerationResponse result = enricher.enrich(input, blueprint);
        assertEquals(1, result.slides().stream()
                .filter(s -> Boolean.TRUE.equals(s.includeImage())).count());
    }

    private SlideDirective slide(int index, String title) {
        return new SlideDirective(
                index, SlideAction.REPLACE, title, List.of("point"), null, null, List.of(0),
                null, null, null, ContentStyle.BULLETS, false, null,
                List.of(), List.of(), false, ImagePosition.NONE);
    }
}
