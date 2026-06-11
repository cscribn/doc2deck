package com.appfire.presentation.llm;

import com.appfire.presentation.model.GenerationResponse;
import com.appfire.presentation.model.PresentationBlueprint;
import com.appfire.presentation.model.SlideAction;
import com.appfire.presentation.model.SlideBlueprint;
import com.appfire.presentation.model.SlideDirective;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MetaSlideFilter {

    private static final Logger LOG = LoggerFactory.getLogger(MetaSlideFilter.class);

    private static final Pattern META_TITLE_PATTERN = Pattern.compile(
            "(?i)(presentation|slide|deck|template).*(tip|guide|how\\s*to|instruction|overview)"
                    + "|(tip|guide|instruction).*(presentation|slide|template)");

    private static final Set<String> META_KEYWORDS = Set.of(
            "tips", "how to present", "layout", "formatting", "template usage", "slide guide");

    public GenerationResponse filter(GenerationResponse response, PresentationBlueprint blueprint) {
        List<SlideDirective> filtered = new ArrayList<>();
        for (SlideDirective slide : response.slides()) {
            if (shouldSkipAsMeta(slide, blueprint)) {
                LOG.warn("MetaSlideFilter: converting slide {} ('{}') to skip",
                        slide.slideIndex(), slide.title());
                filtered.add(slide.withAction(SlideAction.SKIP));
            } else {
                filtered.add(slide);
            }
        }
        return new GenerationResponse(filtered, response.warnings());
    }

    private boolean shouldSkipAsMeta(SlideDirective slide, PresentationBlueprint blueprint) {
        if (slide.action() == SlideAction.SKIP) {
            return false;
        }
        if (Boolean.TRUE.equals(slide.isMetaSlide())) {
            return true;
        }
        int signals = 0;
        if (matchesMetaTitle(slide.title())) {
            signals++;
        }
        if (hasNoDocxGrounding(slide) && matchesMetaKeywords(slide.title())) {
            signals++;
        }
        if (overlapsTemplateNotesOnly(slide, blueprint)) {
            signals++;
        }
        return signals >= 2;
    }

    private boolean matchesMetaTitle(String title) {
        return title != null && META_TITLE_PATTERN.matcher(title).find();
    }

    private boolean hasNoDocxGrounding(SlideDirective slide) {
        return slide.sourceRefs() == null || slide.sourceRefs().isEmpty();
    }

    private boolean matchesMetaKeywords(String title) {
        if (title == null) {
            return false;
        }
        String lower = title.toLowerCase(Locale.ROOT);
        return META_KEYWORDS.stream().anyMatch(lower::contains);
    }

    private boolean overlapsTemplateNotesOnly(SlideDirective slide, PresentationBlueprint blueprint) {
        if (!hasNoDocxGrounding(slide)) {
            return false;
        }
        SlideBlueprint templateSlide = blueprint.slides().stream()
                .filter(s -> s.slideIndex() == slide.slideIndex())
                .findFirst()
                .orElse(null);
        if (templateSlide == null || templateSlide.notes().isBlank()) {
            return false;
        }
        String slideText = collectSlideText(slide);
        return tokenOverlap(slideText, templateSlide.notes()) > 0.6;
    }

    private String collectSlideText(SlideDirective slide) {
        StringBuilder builder = new StringBuilder();
        if (slide.title() != null) {
            builder.append(slide.title()).append(' ');
        }
        if (slide.bullets() != null) {
            builder.append(String.join(" ", slide.bullets())).append(' ');
        }
        if (slide.bodyText() != null) {
            builder.append(slide.bodyText());
        }
        return builder.toString();
    }

    private double tokenOverlap(String a, String b) {
        Set<String> tokensA = tokenize(a);
        Set<String> tokensB = tokenize(b);
        if (tokensA.isEmpty() || tokensB.isEmpty()) {
            return 0.0;
        }
        long intersection = tokensA.stream().filter(tokensB::contains).count();
        long union = tokensA.size() + tokensB.size() - intersection;
        return union == 0 ? 0.0 : (double) intersection / union;
    }

    private Set<String> tokenize(String text) {
        return Pattern.compile("\\W+")
                .splitAsStream(text.toLowerCase(Locale.ROOT))
                .filter(t -> t.length() > 2)
                .collect(Collectors.toSet());
    }
}
