package com.appfire.presentation.html;

import com.appfire.presentation.model.HtmlDeckBullet;
import com.appfire.presentation.model.HtmlDeckSlide;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public final class HtmlSlideRenderer {

    private static final String[] MILESTONE_COLORS = {"0", "1", "2", "3", "4", "5", "6"};

    public String renderSlide(HtmlDeckSlide slide, int index, String imageDataUri) {
        String layout = slide.layout().toLowerCase();
        StringBuilder sb = new StringBuilder();
        sb.append("<section class=\"slide\" id=\"s").append(index + 1).append("\" data-m=\"")
                .append(slide.milestone()).append("\" data-title=\"")
                .append(HtmlEscaper.attr(slide.title())).append('"');
        if (slide.ai() != null && !slide.ai().isBlank()) {
            sb.append(" data-ai=\"").append(HtmlEscaper.attr(slide.ai())).append('"');
        }
        sb.append(">\n");

        switch (layout) {
            case "hero" -> renderHero(slide, sb);
            case "standard" -> renderStandard(slide, sb);
            case "split" -> renderSplit(slide, sb, imageDataUri);
            case "closing" -> renderClosing(slide, sb);
            default -> renderStandard(slide, sb);
        }

        sb.append(renderNotes(slide.notes()));
        sb.append("</section>\n");
        return sb.toString();
    }

    private void renderHero(HtmlDeckSlide slide, StringBuilder sb) {
        sb.append("<div class=\"s1-left\">\n");
        if (slide.kicker() != null && !slide.kicker().isBlank()) {
            sb.append("  <span class=\"tag fu\" style=\"--d:.05s;--tc:var(--ai2)\">")
                    .append(HtmlEscaper.text(slide.kicker())).append("</span>\n");
        }
        sb.append("  <h1 class=\"s1-h fu\" style=\"--d:.15s\">")
                .append(HtmlEscaper.text(slide.headline())).append("</h1>\n");
        if (slide.subline() != null && !slide.subline().isBlank()) {
            sb.append("  <p class=\"s1-sub fu\" style=\"--d:.3s\">")
                    .append(formatSubline(slide.subline())).append("</p>\n");
        }
        List<HtmlDeckBullet> bullets = slide.bullets();
        if (!bullets.isEmpty()) {
            sb.append("  <div class=\"s1-ms\">\n");
            for (int i = 0; i < bullets.size(); i++) {
                String color = MILESTONE_COLORS[Math.min(i + 1, MILESTONE_COLORS.length - 1)];
                sb.append("    <div class=\"av pop\" style=\"--ac:var(--m").append(color)
                        .append(");--d:").append(0.6 + i * 0.1).append("s\">")
                        .append(iconSvg(bullets.get(i).icon())).append("</div>\n");
            }
            sb.append("  </div>\n");
        }
        sb.append("</div>\n");
        sb.append("<div class=\"s1-keys fi\" style=\"--d:2.6s\"><span><kbd>→</kbd>next</span>")
                .append("<span><kbd>N</kbd>notes</span><span><kbd>O</kbd>overview</span>")
                .append("<span><kbd>F</kbd>fullscreen</span></div>\n");
    }

    private void renderStandard(HtmlDeckSlide slide, StringBuilder sb) {
        appendKickerTitle(slide, sb);
        sb.append("<div class=\"gen-body\">\n<div class=\"gen-chips\">\n");
        appendChips(slide.bullets(), sb, 0.2);
        sb.append("</div></div>\n");
    }

    private void renderSplit(HtmlDeckSlide slide, StringBuilder sb, String imageDataUri) {
        appendKickerTitle(slide, sb);
        sb.append("<div class=\"gen-body gen-split\">\n<div class=\"gen-chips\">\n");
        appendChips(slide.bullets(), sb, 0.15);
        sb.append("</div>\n<div class=\"gen-img-panel fu\" style=\"--d:.4s\">");
        if (imageDataUri != null && !imageDataUri.isBlank()) {
            sb.append("<img src=\"").append(imageDataUri).append("\" alt=\"\">");
        } else {
            sb.append("<div class=\"gen-img-ph\">Image unavailable</div>");
        }
        sb.append("</div></div>\n");
    }

    private void renderClosing(HtmlDeckSlide slide, StringBuilder sb) {
        sb.append("<div class=\"gen-closing\">\n");
        sb.append("<h2 class=\"title fu\" style=\"--d:.1s\">").append(HtmlEscaper.text(slide.title()))
                .append("</h2>\n<div class=\"gen-chips\">\n");
        appendChips(slide.bullets(), sb, 0.25);
        sb.append("</div>\n");
        if (slide.subline() != null && !slide.subline().isBlank()) {
            sb.append("<p class=\"gen-cta fu\" style=\"--d:.8s\">")
                    .append(formatSubline(slide.subline())).append("</p>\n");
        }
        sb.append("</div>\n");
    }

    private void appendKickerTitle(HtmlDeckSlide slide, StringBuilder sb) {
        if (slide.kicker() != null && !slide.kicker().isBlank()) {
            sb.append("<div class=\"kicker\">").append(HtmlEscaper.text(slide.kicker())).append("</div>\n");
        }
        sb.append("<h2 class=\"title\">").append(HtmlEscaper.text(slide.title())).append("</h2>\n");
    }

    private void appendChips(List<HtmlDeckBullet> bullets, StringBuilder sb, double baseDelay) {
        for (int i = 0; i < bullets.size(); i++) {
            HtmlDeckBullet bullet = bullets.get(i);
            sb.append("<span class=\"chip fu\" style=\"--d:")
                    .append(String.format("%.2f", baseDelay + i * 0.12))
                    .append("s\">").append(iconSvg(bullet.icon()))
                    .append(HtmlEscaper.text(bullet.text())).append("</span>\n");
        }
    }

    private String formatSubline(String subline) {
        String[] parts = subline.split(";");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                sb.append(" · ");
            }
            sb.append(HtmlEscaper.text(parts[i].trim()));
        }
        return sb.toString();
    }

    private String iconSvg(String icon) {
        String id = icon == null ? "check" : icon.trim().toLowerCase();
        return "<svg class=\"ic\"><use href=\"#i-" + HtmlEscaper.attr(id) + "\"/></svg>";
    }

    private String renderNotes(List<String> notes) {
        if (notes == null || notes.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("<aside class=\"notes\"><ul>\n");
        for (String note : notes) {
            sb.append("<li>").append(HtmlEscaper.text(note)).append("</li>\n");
        }
        sb.append("</ul></aside>\n");
        return sb.toString();
    }
}
