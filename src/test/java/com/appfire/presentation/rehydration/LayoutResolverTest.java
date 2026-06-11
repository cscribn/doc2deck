package com.appfire.presentation.rehydration;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.appfire.presentation.model.ContentStyle;
import com.appfire.presentation.model.ImagePosition;
import com.appfire.presentation.model.LayoutBlueprint;
import com.appfire.presentation.model.LayoutCatalog;
import com.appfire.presentation.model.MediaLayoutSpec;
import com.appfire.presentation.model.SlideAction;
import com.appfire.presentation.model.SlideDirective;
import com.appfire.presentation.rehydration.LayoutResolver.ResolvedLayout;
import java.util.List;
import org.junit.jupiter.api.Test;

class LayoutResolverTest {

    private final LayoutCatalog catalog = new LayoutCatalog(
            List.of(new LayoutBlueprint("Two Content", true, true, false, true, 2)),
            List.of(new MediaLayoutSpec(
                    "Picture Right", ImagePosition.RIGHT,
                    360, 40, 320, 320, 72, 40, 280, 320, true)));

    @Test
    void resolvesMediaLayoutForImageSlides() {
        LayoutResolver resolver = new LayoutResolver(catalog);
        SlideDirective directive = new SlideDirective(
                0, SlideAction.REPLACE, "Title", List.of(), null, null, List.of(),
                null, null, "Picture Right", ContentStyle.BULLETS, true, "tech abstract",
                List.of(), List.of(), false, ImagePosition.RIGHT);
        ResolvedLayout result = resolver.resolve(directive);
        assertTrue(result.mediaSpec() != null);
        assertTrue(result.mediaSpec().imagePosition() == ImagePosition.RIGHT);
    }

    @Test
    void resolvesTwoColumnByName() {
        LayoutResolver resolver = new LayoutResolver(catalog);
        SlideDirective directive = new SlideDirective(
                0, SlideAction.REPLACE, "Title", List.of(), null, null, List.of(),
                null, null, "Two Content", ContentStyle.TWO_COLUMN, false, null,
                List.of("A"), List.of("B"), false, ImagePosition.NONE);
        ResolvedLayout result = resolver.resolve(directive);
        assertTrue(result.layout() != null);
        assertTrue(result.layout().hasTwoColumns());
    }
}
