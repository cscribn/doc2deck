package com.appfire.presentation.llm;

import com.appfire.presentation.config.HtmlDeckOutlineConfig;
import com.appfire.presentation.model.HtmlDeckBullet;
import com.appfire.presentation.model.HtmlDeckContentResponse;
import com.appfire.presentation.model.HtmlDeckSlide;
import com.appfire.presentation.template.WordCount;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public final class HtmlDeckResponseValidator {

    private static final Logger LOG = LoggerFactory.getLogger(HtmlDeckResponseValidator.class);
    private static final Set<String> ALLOWED_ICONS = Set.of(
            "pen", "brief", "users", "chart", "target", "check", "sparkles", "flag", "trend", "doc", "shield", "star",
            "zap", "gauge", "building", "link", "search", "crown", "sliders", "cpu", "eye", "lock", "book", "layers",
            "sprout", "msg", "folder", "clip", "ticket", "grid", "alert", "scale", "rank", "cut", "loop", "x", "plus",
            "minus", "up", "down", "right", "tie", "user", "branch", "frame", "pr", "checkc", "q");

    private final HtmlDeckOutlineConfig outlineConfig;

    public HtmlDeckResponseValidator(HtmlDeckOutlineConfig outlineConfig) {
        this.outlineConfig = outlineConfig;
    }

    public ValidationResult validate(HtmlDeckContentResponse response) {
        List<String> critical = new ArrayList<>();
        List<String> advisory = new ArrayList<>();

        if (response.deckTitle() == null || response.deckTitle().isBlank()) {
            critical.add("Missing or blank deckTitle");
        } else {
            checkWords("deckTitle", response.deckTitle(), critical);
        }

        List<HtmlDeckSlide> slides = response.slides();
        if (slides == null || slides.isEmpty()) {
            critical.add("Missing or empty slides array");
            return new ValidationResult(false, critical, advisory);
        }

        if (slides.size() < outlineConfig.slidesMin() || slides.size() > outlineConfig.slidesMax()) {
            critical.add("Slide count " + slides.size() + " outside allowed range "
                    + outlineConfig.slidesMin() + "-" + outlineConfig.slidesMax());
        }

        int maxMilestone = outlineConfig.milestoneLabels().size() - 1;
        for (int i = 0; i < slides.size(); i++) {
            validateSlide(slides.get(i), i, maxMilestone, critical);
        }

        if (!slides.isEmpty()) {
            String firstLayout = slides.get(0).layout();
            String lastLayout = slides.get(slides.size() - 1).layout();
            if (!"hero".equalsIgnoreCase(firstLayout)) {
                critical.add("First slide must use layout hero");
            }
            if (!"closing".equalsIgnoreCase(lastLayout)) {
                critical.add("Last slide must use layout closing");
            }
            checkLayoutVariety(slides, advisory);
        }

        if (response.sourceRefs() == null || response.sourceRefs().isEmpty()) {
            advisory.add("Empty sourceRefs");
        }

        boolean passed = critical.isEmpty();
        if (!passed) {
            critical.forEach(msg -> LOG.error("HTML deck validation critical: {}", msg));
        }
        advisory.forEach(msg -> LOG.warn("HTML deck validation advisory: {}", msg));
        return new ValidationResult(passed, critical, advisory);
    }

    private void validateSlide(HtmlDeckSlide slide, int index, int maxMilestone, List<String> critical) {
        String prefix = "slides." + index;
        String layout = slide.layout();
        if (layout == null || layout.isBlank()) {
            critical.add(prefix + " missing layout");
            return;
        }
        if (!HtmlDeckOutlineConfig.ALLOWED_LAYOUTS.contains(layout.toLowerCase())) {
            critical.add(prefix + " invalid layout: " + layout);
            return;
        }
        layout = layout.toLowerCase();

        if (slide.milestone() < 0 || slide.milestone() > maxMilestone) {
            critical.add(prefix + " milestone " + slide.milestone() + " outside 0-" + maxMilestone);
        }

        if (slide.title() == null || slide.title().isBlank()) {
            critical.add(prefix + " missing title");
        } else {
            checkWords("slide.title", slide.title(), critical);
        }

        if (slide.kicker() != null && !slide.kicker().isBlank()) {
            checkWords("slide.kicker", slide.kicker(), critical);
        }

        switch (layout) {
            case "hero" -> validateHero(slide, prefix, critical);
            case "standard" -> validateStandard(slide, prefix, critical);
            case "split" -> validateSplit(slide, prefix, critical);
            case "journey" -> validateJourney(slide, prefix, critical);
            case "doc" -> validateDoc(slide, prefix, critical);
            case "cards" -> validateCards(slide, prefix, critical);
            case "list" -> validateList(slide, prefix, critical);
            case "closing" -> validateClosing(slide, prefix, critical);
            default -> critical.add(prefix + " unknown layout");
        }

        validateNotes(slide, prefix, critical);
    }

    private void validateHero(HtmlDeckSlide slide, String prefix, List<String> critical) {
        if (slide.headline() == null || slide.headline().isBlank()) {
            critical.add(prefix + " hero requires headline");
        } else {
            checkWords("slide.headline", slide.headline(), critical);
        }
        if (slide.subline() != null && !slide.subline().isBlank()) {
            checkWords("slide.subline", slide.subline(), critical);
        }
        validateBullets(slide.bullets(), prefix, 0, 5, critical);
    }

    private void validateStandard(HtmlDeckSlide slide, String prefix, List<String> critical) {
        validateBullets(slide.bullets(), prefix, 2, 4, critical);
    }

    private void validateSplit(HtmlDeckSlide slide, String prefix, List<String> critical) {
        validateBullets(slide.bullets(), prefix, 2, 3, critical);
        validateImageQuery(slide.imageQuery(), prefix, critical);
    }

    private void validateJourney(HtmlDeckSlide slide, String prefix, List<String> critical) {
        validateBullets(slide.bullets(), prefix, 3, 5, critical);
    }

    private void validateDoc(HtmlDeckSlide slide, String prefix, List<String> critical) {
        validateBullets(slide.bullets(), prefix, 2, 4, critical);
    }

    private void validateCards(HtmlDeckSlide slide, String prefix, List<String> critical) {
        validateBullets(slide.bullets(), prefix, 2, 4, critical);
    }

    private void validateList(HtmlDeckSlide slide, String prefix, List<String> critical) {
        validateBullets(slide.bullets(), prefix, 2, 4, critical);
    }

    private void validateClosing(HtmlDeckSlide slide, String prefix, List<String> critical) {
        validateBullets(slide.bullets(), prefix, 2, 3, critical);
        if (slide.subline() != null && !slide.subline().isBlank()) {
            checkWords("slide.subline", slide.subline(), critical);
        }
    }

    private void validateBullets(
            List<HtmlDeckBullet> bullets, String prefix, int min, int max, List<String> critical) {
        if (bullets == null || bullets.size() < min || bullets.size() > max) {
            critical.add(prefix + " requires " + min + "-" + max + " bullets");
            return;
        }
        for (int i = 0; i < bullets.size(); i++) {
            HtmlDeckBullet bullet = bullets.get(i);
            if (bullet.text() == null || bullet.text().isBlank()) {
                critical.add(prefix + ".bullets." + i + " text is blank");
            } else {
                checkWords("slide.bullet", bullet.text(), critical);
            }
            String icon = bullet.icon() == null ? "" : bullet.icon().trim().toLowerCase();
            if (icon.isBlank() || !ALLOWED_ICONS.contains(icon)) {
                critical.add(prefix + ".bullets." + i + " invalid icon: " + bullet.icon());
            }
        }
    }

    private void validateImageQuery(String query, String prefix, List<String> critical) {
        if (query == null || query.isBlank()) {
            critical.add(prefix + " split layout requires imageQuery");
            return;
        }
        int words = WordCount.count(query);
        if (words < HtmlDeckOutlineConfig.MIN_IMAGE_QUERY_WORDS
                || words > HtmlDeckOutlineConfig.MAX_IMAGE_QUERY_WORDS) {
            critical.add(prefix + " imageQuery must be "
                    + HtmlDeckOutlineConfig.MIN_IMAGE_QUERY_WORDS + "-"
                    + HtmlDeckOutlineConfig.MAX_IMAGE_QUERY_WORDS + " words");
        }
        checkWords("slide.imageQuery", query, critical);
    }

    private void validateNotes(HtmlDeckSlide slide, String prefix, List<String> critical) {
        if (slide.notes() == null || slide.notes().isEmpty()) {
            critical.add(prefix + " requires at least one speaker note");
            return;
        }
        for (int i = 0; i < slide.notes().size(); i++) {
            String note = slide.notes().get(i);
            if (note == null || note.isBlank()) {
                critical.add(prefix + ".notes." + i + " is blank");
            } else {
                checkWords("slide.notes", note, critical);
            }
        }
    }

    private void checkWords(String fieldKey, String value, List<String> critical) {
        int limit = outlineConfig.maxWordsFor(fieldKey);
        int words = WordCount.count(value);
        if (words > limit) {
            critical.add(fieldKey + " exceeds " + limit + " words (" + words + "): " + truncate(value));
        }
    }

    private static String truncate(String value) {
        return value.length() <= 60 ? value : value.substring(0, 60) + "...";
    }

    private static void checkLayoutVariety(List<HtmlDeckSlide> slides, List<String> advisory) {
        if (slides.size() < 3) {
            return;
        }
        int run = 1;
        String prev = slides.get(1).layout().toLowerCase();
        for (int i = 2; i < slides.size() - 1; i++) {
            String layout = slides.get(i).layout().toLowerCase();
            if (layout.equals(prev)) {
                run++;
                if (run >= 3) {
                    advisory.add("Layout " + layout + " used " + run + "+ times consecutively (middle slides)");
                    break;
                }
            } else {
                run = 1;
                prev = layout;
            }
        }
        Set<String> distinct = new LinkedHashSet<>();
        for (int i = 1; i < slides.size() - 1; i++) {
            distinct.add(slides.get(i).layout().toLowerCase());
        }
        if (slides.size() - 2 >= 4 && distinct.size() < 4) {
            advisory.add("Middle slides use fewer than 4 distinct layouts; vary journey, doc, cards, list, split, standard");
        }
    }

    public record ValidationResult(boolean passed, List<String> criticalFailures, List<String> advisories) {
    }
}
