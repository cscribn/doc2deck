package com.appfire.presentation.llm;

import com.appfire.presentation.model.GenerationResponse;
import com.appfire.presentation.model.SlideAction;
import com.appfire.presentation.model.SlideDirective;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ResponseValidator {

    private static final Logger LOG = LoggerFactory.getLogger(ResponseValidator.class);
    private static final int MAX_BULLETS = 4;
    private static final double MIN_TITLE_PT = 32.0;
    private static final double MIN_BODY_PT = 18.0;

    public ValidationResult validate(GenerationResponse response, int templateSlideCount) {
        List<String> critical = new ArrayList<>();
        List<String> advisory = new ArrayList<>();

        if (response.slides() == null || response.slides().isEmpty()) {
            critical.add("Missing or empty slides array");
            return new ValidationResult(false, critical, advisory);
        }

        for (SlideDirective slide : response.slides()) {
            validateSlide(slide, templateSlideCount, critical, advisory);
        }
        checkNarrativeFlow(response.slides(), templateSlideCount, advisory);

        boolean passed = critical.isEmpty();
        if (!passed) {
            critical.forEach(msg -> LOG.error("Validation critical: {}. Resolution: fix Gemini output or re-run.",
                    msg));
        }
        advisory.forEach(msg -> LOG.warn("Validation advisory: {}", msg));

        return new ValidationResult(passed, critical, advisory);
    }

    private void validateSlide(
            SlideDirective slide,
            int templateSlideCount,
            List<String> critical,
            List<String> advisory) {
        if (slide.action() == SlideAction.SKIP) {
            validateSkipSlide(slide, templateSlideCount, critical);
            return;
        }
        validateActiveSlide(slide, templateSlideCount, critical, advisory);
    }

    private void validateSkipSlide(SlideDirective slide, int templateSlideCount, List<String> critical) {
        if (slide.slideIndex() < 0 || slide.slideIndex() >= templateSlideCount) {
            critical.add("skip slideIndex " + slide.slideIndex() + " out of range [0, "
                    + (templateSlideCount - 1) + "]");
        }
    }

    private void validateActiveSlide(
            SlideDirective slide,
            int templateSlideCount,
            List<String> critical,
            List<String> advisory) {
        validateSlideIndex(slide, templateSlideCount, critical);
        validateBullets(slide, critical);
        validateTitle(slide, critical);
        validateFontSizes(slide, advisory);
        validateSourceRefs(slide, advisory);
    }

    private void validateSlideIndex(SlideDirective slide, int templateSlideCount, List<String> critical) {
        if (slide.action() == SlideAction.APPEND) {
            if (slide.slideIndex() < templateSlideCount) {
                critical.add("append slideIndex " + slide.slideIndex()
                        + " must be >= template slide count " + templateSlideCount);
            }
            return;
        }
        if (slide.slideIndex() < 0 || slide.slideIndex() >= templateSlideCount) {
            critical.add("replace slideIndex " + slide.slideIndex() + " out of range [0, "
                    + (templateSlideCount - 1) + "]");
        }
    }

    private void validateBullets(SlideDirective slide, List<String> critical) {
        if (slide.bullets() != null && slide.bullets().size() > MAX_BULLETS) {
            critical.add("slide " + slide.slideIndex() + " has " + slide.bullets().size()
                    + " bullets (max " + MAX_BULLETS + ")");
        }
    }

    private void validateTitle(SlideDirective slide, List<String> critical) {
        if (slide.title() == null || slide.title().isBlank()) {
            critical.add("slide " + slide.slideIndex() + " has empty title");
        }
    }

    private void validateFontSizes(SlideDirective slide, List<String> advisory) {
        if (slide.titleFontSizePt() != null && slide.titleFontSizePt() < MIN_TITLE_PT) {
            advisory.add("slide " + slide.slideIndex() + " title font " + slide.titleFontSizePt()
                    + "pt below minimum " + MIN_TITLE_PT + "pt");
        }
        if (slide.bodyFontSizePt() != null && slide.bodyFontSizePt() < MIN_BODY_PT) {
            advisory.add("slide " + slide.slideIndex() + " body font " + slide.bodyFontSizePt()
                    + "pt below minimum " + MIN_BODY_PT + "pt");
        }
    }

    private void validateSourceRefs(SlideDirective slide, List<String> advisory) {
        if (slide.sourceRefs() == null || slide.sourceRefs().isEmpty()) {
            advisory.add("slide " + slide.slideIndex() + " has empty sourceRefs");
        }
    }

    private void checkNarrativeFlow(List<SlideDirective> slides, int templateSlideCount, List<String> advisory) {
        if (templateSlideCount < 4) {
            return;
        }
        long activeSlides = slides.stream().filter(s -> s.action() != SlideAction.SKIP).count();
        if (activeSlides < 4) {
            advisory.add("Fewer than 4 active slides; narrative flow sections may be incomplete");
        }
    }

    public record ValidationResult(boolean passed, List<String> criticalFailures, List<String> advisories) {
    }
}
