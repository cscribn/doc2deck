package com.appfire.presentation.images;

import com.appfire.presentation.model.ContentStyle;
import com.appfire.presentation.model.GenerationResponse;
import com.appfire.presentation.model.ImagePosition;
import com.appfire.presentation.model.LayoutCatalog;
import com.appfire.presentation.model.MediaLayoutSpec;
import com.appfire.presentation.model.PresentationBlueprint;
import com.appfire.presentation.model.SlideAction;
import com.appfire.presentation.model.SlideDirective;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ImageDirectiveEnricher {

    private static final Logger LOG = LoggerFactory.getLogger(ImageDirectiveEnricher.class);
    private static final double MIN_COVERAGE = 0.75;
    private static final double TARGET_COVERAGE = 0.85;
    private static final int LONG_BODY_THRESHOLD = 120;
    private static final ImagePosition[] POSITION_ROTATION = {
            ImagePosition.RIGHT, ImagePosition.LEFT, ImagePosition.RIGHT, ImagePosition.TOP
    };

    private final LayoutCatalog layoutCatalog;

    public ImageDirectiveEnricher(LayoutCatalog layoutCatalog) {
        this.layoutCatalog = layoutCatalog != null ? layoutCatalog : new LayoutCatalog(List.of());
    }

    public GenerationResponse enrich(GenerationResponse response, PresentationBlueprint blueprint) {
        List<SlideDirective> slides = response.slides();
        List<SlideDirective> active = slides.stream()
                .filter(s -> s.action() != SlideAction.SKIP)
                .toList();
        if (active.isEmpty()) {
            return response;
        }

        if (layoutCatalog.mediaLayouts().isEmpty()) {
            LOG.warn("No media layouts detected in template. Images may overlap text.");
        }

        long withImages = active.stream().filter(s -> Boolean.TRUE.equals(s.includeImage())).count();
        double coverage = (double) withImages / active.size();
        List<Integer> targets = coverage >= MIN_COVERAGE
                ? List.of()
                : selectTargetIndexes(active, (int) Math.round(active.size() * TARGET_COVERAGE) - (int) withImages);

        if (targets.isEmpty() && coverage >= MIN_COVERAGE) {
            LOG.info("Image directives present on {}/{} active slides ({}%)",
                    withImages, active.size(), Math.round(coverage * 100));
            return enrichExistingPositions(response);
        }

        String themePrefix = deriveThemePrefix(active);
        List<SlideDirective> enriched = new ArrayList<>();
        int rotationIndex = 0;
        for (SlideDirective slide : slides) {
            if (shouldAssignImage(slide, targets)) {
                ImagePosition position = POSITION_ROTATION[rotationIndex % POSITION_ROTATION.length];
                rotationIndex++;
                MediaLayoutSpec mediaLayout = layoutCatalog.mediaLayoutForSlide(slide.slideIndex(), position);
                String layoutName = mediaLayout != null ? mediaLayout.layoutName() : slide.layoutName();
                ImagePosition resolvedPosition = mediaLayout != null ? mediaLayout.imagePosition() : position;
                String query = buildImageQuery(slide, themePrefix);
                enriched.add(slide.withImage(query, layoutName, resolvedPosition));
                LOG.info("Auto-assigned {} image to slide {} ('{}'): layout='{}' query='{}'",
                        resolvedPosition, slide.slideIndex(), slide.title(), layoutName, query);
            } else {
                enriched.add(slide);
            }
        }

        if (!targets.isEmpty()) {
            LOG.warn("Gemini returned {}% image coverage; auto-enriched {} slides to reach ~{}%",
                    Math.round(coverage * 100), targets.size(), Math.round(TARGET_COVERAGE * 100));
        }
        return new GenerationResponse(enriched, response.warnings());
    }

    private GenerationResponse enrichExistingPositions(GenerationResponse response) {
        List<SlideDirective> enriched = new ArrayList<>();
        int rotationIndex = 0;
        for (SlideDirective slide : response.slides()) {
            if (Boolean.TRUE.equals(slide.includeImage())
                    && slide.imagePosition() == ImagePosition.NONE) {
                ImagePosition position = POSITION_ROTATION[rotationIndex++ % POSITION_ROTATION.length];
                MediaLayoutSpec spec = layoutCatalog.mediaLayoutForSlide(slide.slideIndex(), position);
                enriched.add(slide.withImage(
                        slide.imageQuery(),
                        spec != null ? spec.layoutName() : slide.layoutName(),
                        spec != null ? spec.imagePosition() : position));
            } else {
                enriched.add(slide);
            }
        }
        return new GenerationResponse(enriched, response.warnings());
    }

    private boolean shouldAssignImage(SlideDirective slide, List<Integer> targets) {
        if (slide.action() == SlideAction.SKIP) {
            return false;
        }
        if (Boolean.TRUE.equals(slide.includeImage())) {
            return false;
        }
        return targets.contains(slide.slideIndex());
    }

    private List<Integer> selectTargetIndexes(List<SlideDirective> active, int needed) {
        List<Integer> eligible = active.stream()
                .filter(this::isEligibleForImage)
                .map(SlideDirective::slideIndex)
                .toList();
        if (eligible.isEmpty() || needed <= 0) {
            return List.of();
        }
        int targetCount = Math.min(needed, eligible.size());
        List<Integer> selected = new ArrayList<>();
        if (targetCount >= eligible.size()) {
            return new ArrayList<>(eligible);
        }
        double step = (double) eligible.size() / targetCount;
        for (int i = 0; i < targetCount; i++) {
            selected.add(eligible.get((int) Math.floor(i * step)));
        }
        return selected;
    }

    private boolean isEligibleForImage(SlideDirective slide) {
        if (slide.bodyText() != null && slide.bodyText().length() > LONG_BODY_THRESHOLD) {
            return false;
        }
        return slide.contentStyle() != ContentStyle.TWO_COLUMN;
    }

    private String deriveThemePrefix(List<SlideDirective> active) {
        return active.stream()
                .map(SlideDirective::title)
                .filter(t -> t != null && !t.isBlank())
                .findFirst()
                .map(t -> t.split("\\s+")[0])
                .orElse("professional");
    }

    private String buildImageQuery(SlideDirective slide, String themePrefix) {
        if (slide.imageQuery() != null && !slide.imageQuery().isBlank()) {
            return slide.imageQuery();
        }
        String title = slide.title() != null ? slide.title() : "presentation";
        String[] words = title.replaceAll("[^a-zA-Z0-9\\s]", " ").trim().split("\\s+");
        int limit = Math.min(words.length, 4);
        StringBuilder query = new StringBuilder(themePrefix);
        for (int i = 0; i < limit; i++) {
            if (!words[i].isBlank()) {
                query.append(' ').append(words[i]);
            }
        }
        return query.toString().trim();
    }
}
