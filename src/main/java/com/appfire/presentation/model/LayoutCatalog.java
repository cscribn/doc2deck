package com.appfire.presentation.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public record LayoutCatalog(List<LayoutBlueprint> layouts, List<MediaLayoutSpec> mediaLayouts) {

    public LayoutCatalog {
        if (layouts == null) {
            layouts = List.of();
        }
        if (mediaLayouts == null) {
            mediaLayouts = List.of();
        }
    }

    public LayoutCatalog(List<LayoutBlueprint> layouts) {
        this(layouts, List.of());
    }

    public Optional<LayoutBlueprint> findByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        String normalized = normalize(name);
        return layouts.stream()
                .filter(l -> normalize(l.name()).equals(normalized))
                .findFirst();
    }

    public List<MediaLayoutSpec> mediaLayouts() {
        return mediaLayouts;
    }

    public Optional<MediaLayoutSpec> findMediaLayout(ImagePosition position) {
        return mediaLayouts.stream()
                .filter(m -> m.imagePosition() == position)
                .findFirst();
    }

    public Optional<MediaLayoutSpec> findMediaLayoutByName(String layoutName) {
        if (layoutName == null || layoutName.isBlank()) {
            return Optional.empty();
        }
        String normalized = normalize(layoutName);
        return mediaLayouts.stream()
                .filter(m -> normalize(m.layoutName()).equals(normalized))
                .findFirst();
    }

    public MediaLayoutSpec mediaLayoutForSlide(int slideIndex, ImagePosition preferred) {
        if (mediaLayouts.isEmpty()) {
            return null;
        }
        List<MediaLayoutSpec> matching = mediaLayouts.stream()
                .filter(m -> m.imagePosition() == preferred)
                .toList();
        if (!matching.isEmpty()) {
            return matching.get(Math.floorMod(slideIndex, matching.size()));
        }
        return mediaLayouts.get(Math.floorMod(slideIndex, mediaLayouts.size()));
    }

    public List<ImagePosition> availableImagePositions() {
        return mediaLayouts.stream()
                .map(MediaLayoutSpec::imagePosition)
                .distinct()
                .toList();
    }

    @Deprecated
    public Optional<LayoutBlueprint> findPictureLayout() {
        return layouts.stream()
                .filter(LayoutBlueprint::hasPicture)
                .findFirst()
                .or(() -> mediaLayouts.stream()
                        .findFirst()
                        .flatMap(m -> findByName(m.layoutName())));
    }

    public Optional<LayoutBlueprint> findTwoColumnLayout() {
        return layouts.stream()
                .filter(LayoutBlueprint::hasTwoColumns)
                .findFirst();
    }

    public Optional<LayoutBlueprint> findStandardContentLayout() {
        return layouts.stream()
                .filter(l -> l.hasTitle() && l.hasBody() && !l.hasPicture() && !l.hasTwoColumns())
                .findFirst()
                .or(() -> layouts.stream()
                        .filter(l -> l.hasTitle() && l.hasBody())
                        .findFirst());
    }

    private static String normalize(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }
}
