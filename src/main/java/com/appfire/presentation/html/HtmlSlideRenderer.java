package com.appfire.presentation.html;

import com.appfire.presentation.model.HtmlDeckBullet;
import com.appfire.presentation.model.HtmlDeckSlide;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public final class HtmlSlideRenderer {

    private static final String[] MILESTONE_COLORS = {"0", "1", "2", "3", "4", "5", "6"};
    private static final int[] BAR_WIDTHS = {45, 62, 78, 55, 88, 70};

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
            case "journey" -> renderJourney(slide, sb);
            case "doc" -> renderDoc(slide, sb);
            case "cards" -> renderCards(slide, sb);
            case "list" -> renderList(slide, sb);
            default -> renderStandard(slide, sb);
        }

        sb.append(renderNotes(slide.notes()));
        sb.append("</section>\n");
        return sb.toString();
    }

    private void renderHero(HtmlDeckSlide slide, StringBuilder sb) {
        sb.append("<div class=\"slide-hero\">\n<div class=\"s1-left\">\n");
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
        sb.append("</div>\n");
    }

    private void renderStandard(HtmlDeckSlide slide, StringBuilder sb) {
        openContentSlide(slide, sb, "layout-standard");
        sb.append("<div class=\"slide-body\"><div class=\"gen-chips chip-wrap\">\n");
        appendChips(slide.bullets(), sb, 0.2);
        sb.append("</div></div>\n");
        closeContentSlide(sb);
    }

    private void renderSplit(HtmlDeckSlide slide, StringBuilder sb, String imageDataUri) {
        String mediaClass = "left".equalsIgnoreCase(slide.imageSide()) ? "split-media-left" : "split-media-right";
        sb.append("<div class=\"slide-safe layout-split ").append(mediaClass).append("\">\n");
        sb.append("<div class=\"split-cols\">\n<div class=\"split-copy\">\n");
        if (slide.kicker() != null && !slide.kicker().isBlank()) {
            sb.append("<div class=\"kicker fu\" style=\"--d:.05s\">")
                    .append(HtmlEscaper.text(slide.kicker())).append("</div>\n");
        }
        sb.append("<h2 class=\"title fu\" style=\"--d:.12s\">").append(HtmlEscaper.text(slide.title()))
                .append("</h2>\n<div class=\"split-bullets\">\n");
        appendSplitBullets(slide.bullets(), sb, 0.18);
        sb.append("</div></div>\n<div class=\"split-media gen-img-panel fu\" style=\"--d:.35s\">");
        if (imageDataUri != null && !imageDataUri.isBlank()) {
            sb.append("<img src=\"").append(imageDataUri).append("\" alt=\"\">");
        } else {
            sb.append("<div class=\"gen-img-ph\">Image unavailable</div>");
        }
        sb.append("</div>\n");
        if (hasAi(slide)) {
            appendAiOrb(slide, sb, "75%", "35%", 0.55);
        }
        sb.append("</div></div>\n");
    }

    private void renderClosing(HtmlDeckSlide slide, StringBuilder sb) {
        sb.append("<div class=\"slide-safe\">\n<div class=\"gen-closing\">\n");
        sb.append("<h2 class=\"title fu\" style=\"--d:.1s\">").append(HtmlEscaper.text(slide.title()))
                .append("</h2>\n<div class=\"gen-chips chip-wrap\">\n");
        appendChips(slide.bullets(), sb, 0.25);
        sb.append("</div>\n");
        if (slide.subline() != null && !slide.subline().isBlank()) {
            sb.append("<p class=\"gen-cta fu\" style=\"--d:.8s\">")
                    .append(formatSubline(slide.subline())).append("</p>\n");
        }
        sb.append("</div></div>\n");
    }

    private void renderJourney(HtmlDeckSlide slide, StringBuilder sb) {
        openContentSlide(slide, sb, "layout-journey");
        List<HtmlDeckBullet> bullets = slide.bullets();
        int n = bullets.size();
        sb.append("<div class=\"slide-body journey-stage\">\n");
        sb.append("<div class=\"journey-road gx flow fu\" style=\"--d:.2s\"></div>\n");
        sb.append("<div class=\"journey-doc\"><svg class=\"ic\"><use href=\"#i-doc\"/></svg></div>\n");
        double start = 80;
        double span = n >= 5 ? 1200 : 1140;
        if (n < 5) {
            start = 110;
        }
        for (int i = 0; i < n; i++) {
            HtmlDeckBullet bullet = bullets.get(i);
            int left = n == 1 ? (int) (start + span / 2) : (int) (start + i * span / (n - 1));
            String color = MILESTONE_COLORS[Math.min(i + 1, MILESTONE_COLORS.length - 1)];
            double hitDelay = 0.18 * i;
            sb.append("<div class=\"journey-node pop\" style=\"left:").append(left).append("px;--ac:var(--m")
                    .append(color).append(");--d:").append(String.format("%.2f", 0.35 + i * 0.12))
                    .append("s;--hd:").append(String.format("%.2f", hitDelay)).append("s\">")
                    .append("<div class=\"av\">").append(iconSvg(bullet.icon())).append("</div>")
                    .append("<span class=\"num\">").append(String.format("%02d", i + 1)).append("</span>")
                    .append("<span class=\"lbl\">").append(HtmlEscaper.text(bullet.text())).append("</span>")
                    .append("</div>\n");
        }
        if (hasAi(slide)) {
            appendAiOrb(slide, sb, "50%", "38%", 1.5);
        }
        sb.append("</div>\n");
        closeContentSlide(sb);
    }

    private void renderDoc(HtmlDeckSlide slide, StringBuilder sb) {
        openContentSlide(slide, sb, "layout-doc");
        List<HtmlDeckBullet> bullets = slide.bullets();
        sb.append("<div class=\"slide-body\">\n<div class=\"doc-panel fu\" style=\"--d:.25s\">\n");
        sb.append("<div class=\"hd\">").append(iconSvg(bullets.isEmpty() ? "doc" : bullets.get(0).icon()))
                .append(HtmlEscaper.text(slide.title())).append("</div>\n");
        for (int i = 0; i < bullets.size(); i++) {
            HtmlDeckBullet bullet = bullets.get(i);
            int bar = BAR_WIDTHS[i % BAR_WIDTHS.length];
            sb.append("<div class=\"doc-row fu\" style=\"--d:").append(String.format("%.2f", 0.35 + i * 0.1))
                    .append("s\"><div class=\"ri\">").append(iconSvg(bullet.icon())).append("</div>")
                    .append("<span class=\"rl\">").append(HtmlEscaper.text(bullet.text())).append("</span>")
                    .append("<div class=\"bar\"><i class=\"gx\" style=\"--pw:").append(bar)
                    .append("%;--d:").append(String.format("%.2f", 0.5 + i * 0.12)).append("s\"></i></div>")
                    .append("</div>\n");
        }
        sb.append("</div>\n");
        sb.append("</div>\n");
        closeContentSlide(sb);
    }

    private void appendSplitBullets(List<HtmlDeckBullet> bullets, StringBuilder sb, double baseDelay) {
        for (int i = 0; i < bullets.size(); i++) {
            HtmlDeckBullet bullet = bullets.get(i);
            sb.append("<div class=\"split-bullet fu\" style=\"--d:")
                    .append(String.format("%.2f", baseDelay + i * 0.1)).append("s\">")
                    .append("<div class=\"ri\">").append(iconSvg(bullet.icon())).append("</div>")
                    .append("<span class=\"text\">").append(HtmlEscaper.text(bullet.text())).append("</span>")
                    .append("</div>\n");
        }
    }

    private void renderCards(HtmlDeckSlide slide, StringBuilder sb) {
        openContentSlide(slide, sb, "");
        List<HtmlDeckBullet> bullets = slide.bullets();
        int cols = bullets.size() == 3 ? 3 : 2;
        sb.append("<div class=\"slide-body cards-grid\" style=\"--cols:").append(cols).append("\">\n");
        for (int i = 0; i < bullets.size(); i++) {
            HtmlDeckBullet bullet = bullets.get(i);
            String color = MILESTONE_COLORS[Math.min(i + 1, MILESTONE_COLORS.length - 1)];
            sb.append("<div class=\"s17-card pop\" style=\"--kc:var(--m").append(color).append(");--d:")
                    .append(String.format("%.2f", 0.2 + i * 0.1)).append("s\">")
                    .append(iconSvg(bullet.icon())).append("<b>")
                    .append(HtmlEscaper.text(bullet.text())).append("</b></div>\n");
        }
        sb.append("</div>\n");
        closeContentSlide(sb);
    }

    private void renderList(HtmlDeckSlide slide, StringBuilder sb) {
        openContentSlide(slide, sb, "");
        List<HtmlDeckBullet> bullets = slide.bullets();
        sb.append("<div class=\"slide-body list-rows\">\n");
        for (int i = 0; i < bullets.size(); i++) {
            HtmlDeckBullet bullet = bullets.get(i);
            int bar = BAR_WIDTHS[(i + 2) % BAR_WIDTHS.length];
            sb.append("<div class=\"s6-row fu\" style=\"--d:").append(String.format("%.2f", 0.18 + i * 0.1))
                    .append("s\"><div class=\"ri\">").append(iconSvg(bullet.icon())).append("</div>")
                    .append("<span class=\"rl\">").append(HtmlEscaper.text(bullet.text())).append("</span>")
                    .append("<div class=\"bar\"><i style=\"--pw:").append(bar).append("%\"></i></div>")
                    .append("</div>\n");
        }
        sb.append("</div>\n");
        closeContentSlide(sb);
    }

    private void openContentSlide(HtmlDeckSlide slide, StringBuilder sb, String extraClass) {
        sb.append("<div class=\"slide-safe");
        if (extraClass != null && !extraClass.isBlank()) {
            sb.append(' ').append(extraClass);
        }
        sb.append("\">\n");
        if (slide.kicker() != null && !slide.kicker().isBlank()) {
            sb.append("<div class=\"kicker fu\" style=\"--d:.05s\">")
                    .append(HtmlEscaper.text(slide.kicker())).append("</div>\n");
        }
        sb.append("<h2 class=\"title fu\" style=\"--d:.12s\">").append(HtmlEscaper.text(slide.title()))
                .append("</h2>\n");
    }

    private void closeContentSlide(StringBuilder sb) {
        sb.append("</div>\n");
    }

    private void appendAiOrb(HtmlDeckSlide slide, StringBuilder sb, String left, String top, double delay) {
        sb.append("<div class=\"journey-ai ctr pop abs\" style=\"left:").append(left).append(";top:")
                .append(top).append(";--d:").append(String.format("%.2f", delay)).append("s\">")
                .append("<div class=\"stem\"></div><div class=\"orb\"><div class=\"ring\"></div><div class=\"core\">")
                .append("<svg class=\"ic\"><use href=\"#i-sparkles\"/></svg></div><div class=\"name\">")
                .append(HtmlEscaper.text(slide.ai())).append("</div></div></div>\n");
    }

    private static boolean hasAi(HtmlDeckSlide slide) {
        return slide.ai() != null && !slide.ai().isBlank();
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
