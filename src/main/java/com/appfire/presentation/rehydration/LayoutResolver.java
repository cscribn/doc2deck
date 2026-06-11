package com.appfire.presentation.rehydration;

import com.appfire.presentation.model.ContentStyle;
import com.appfire.presentation.model.ImagePosition;
import com.appfire.presentation.model.LayoutBlueprint;
import com.appfire.presentation.model.LayoutCatalog;
import com.appfire.presentation.model.MediaLayoutSpec;
import com.appfire.presentation.model.SlideDirective;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LayoutResolver {

    private static final Logger LOG = LoggerFactory.getLogger(LayoutResolver.class);
    private static final int LONG_BODY_THRESHOLD = 120;

    private final LayoutCatalog catalog;

    public LayoutResolver(LayoutCatalog catalog) {
        this.catalog = catalog != null ? catalog : new LayoutCatalog(java.util.List.of());
    }

    public ResolvedLayout resolve(SlideDirective directive) {
        if (Boolean.TRUE.equals(directive.includeImage())) {
            Optional<MediaLayoutSpec> mediaSpec = resolveMediaSpec(directive);
            if (mediaSpec.isPresent()) {
                MediaLayoutSpec spec = mediaSpec.get();
                Optional<LayoutBlueprint> layout = catalog.findByName(spec.layoutName());
                return new ResolvedLayout(layout.orElse(null), spec);
            }
        }

        Optional<LayoutBlueprint> byName = catalog.findByName(directive.layoutName());
        if (byName.isPresent()) {
            return new ResolvedLayout(byName.get(), null);
        }
        if (directive.layoutName() != null && !directive.layoutName().isBlank()) {
            LOG.warn("Layout '{}' not in catalog for slide {}. Using fallback.",
                    directive.layoutName(), directive.slideIndex());
        }
        return new ResolvedLayout(fallback(directive).orElse(null), null);
    }

    private Optional<MediaLayoutSpec> resolveMediaSpec(SlideDirective directive) {
        if (directive.imagePosition() != null && directive.imagePosition() != ImagePosition.NONE) {
            MediaLayoutSpec spec = catalog.mediaLayoutForSlide(
                    directive.slideIndex(), directive.imagePosition());
            if (spec != null) {
                return Optional.of(spec);
            }
        }
        if (directive.layoutName() != null && !directive.layoutName().isBlank()) {
            Optional<MediaLayoutSpec> byLayout = catalog.mediaLayouts().stream()
                    .filter(m -> m.layoutName().equalsIgnoreCase(directive.layoutName()))
                    .findFirst();
            if (byLayout.isPresent()) {
                return byLayout;
            }
        }
        MediaLayoutSpec rotated = catalog.mediaLayoutForSlide(
                directive.slideIndex(), ImagePosition.RIGHT);
        return rotated != null ? Optional.of(rotated) : Optional.empty();
    }

    private Optional<LayoutBlueprint> fallback(SlideDirective directive) {
        if (directive.contentStyle() == ContentStyle.TWO_COLUMN) {
            Optional<LayoutBlueprint> twoCol = catalog.findTwoColumnLayout();
            if (twoCol.isPresent()) {
                return twoCol;
            }
        }
        return catalog.findStandardContentLayout();
    }

    public LayoutCatalog catalog() {
        return catalog;
    }

    public record ResolvedLayout(LayoutBlueprint layout, MediaLayoutSpec mediaSpec) {
    }
}
