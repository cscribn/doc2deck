package com.appfire.presentation.config;

import java.util.LinkedHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HtmlDeckOutlineConfig {

    public static final String HEADER_KEY = "htmlDeck.header";
    public static final int DEFAULT_MAX_WORDS = 15;
    public static final int MIN_IMAGE_QUERY_WORDS = 2;
    public static final int MAX_IMAGE_QUERY_WORDS = 5;
    public static final List<String> ALLOWED_LAYOUTS = List.of("hero", "standard", "split", "closing");

    private final String header;
    private final List<String> milestoneLabels;
    private final int slidesMin;
    private final int slidesMax;
    private final Map<String, Integer> maxWordsByField;
    private final Map<String, String> layoutDescriptions;

    public HtmlDeckOutlineConfig(
            String header,
            List<String> milestoneLabels,
            int slidesMin,
            int slidesMax,
            Map<String, Integer> maxWordsByField,
            Map<String, String> layoutDescriptions) {
        this.header = header;
        this.milestoneLabels = List.copyOf(milestoneLabels);
        this.slidesMin = slidesMin;
        this.slidesMax = slidesMax;
        this.maxWordsByField = Map.copyOf(maxWordsByField);
        this.layoutDescriptions = Map.copyOf(layoutDescriptions);
    }

    public static HtmlDeckOutlineConfig unusedPlaceholder() {
        Map<String, String> layouts = new LinkedHashMap<>();
        for (String layout : ALLOWED_LAYOUTS) {
            layouts.put(layout, "(unused in pptx mode)");
        }
        return new HtmlDeckOutlineConfig(
                "(unused)", List.of("Start"), 1, 99, Map.of(), layouts);
    }

    public String header() {
        return header;
    }

    public List<String> milestoneLabels() {
        return milestoneLabels;
    }

    public int slidesMin() {
        return slidesMin;
    }

    public int slidesMax() {
        return slidesMax;
    }

    public int maxWordsFor(String fieldKey) {
        return maxWordsByField.getOrDefault(fieldKey, DEFAULT_MAX_WORDS);
    }

    public Map<String, String> layoutDescriptions() {
        return layoutDescriptions;
    }

    public String formatForPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append(header).append("\n\n");
        sb.append("Milestones (use milestone index 0-").append(milestoneLabels.size() - 1).append("):\n");
        for (int i = 0; i < milestoneLabels.size(); i++) {
            sb.append("  ").append(i).append(": ").append(milestoneLabels.get(i)).append("\n");
        }
        sb.append("\nSlide count: ").append(slidesMin).append("-").append(slidesMax).append(" inclusive.\n\n");
        sb.append("Field word limits (hard; output rejected if exceeded):\n");
        maxWordsByField.forEach((key, limit) -> sb.append("  ").append(key).append(": ").append(limit).append(" words\n"));
        sb.append("\nLayouts:\n");
        layoutDescriptions.forEach((layout, desc) -> sb.append("  ").append(layout).append(": ").append(desc).append("\n"));
        return sb.toString();
    }

    public String milestonesJsonForRuntime() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < milestoneLabels.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append('"').append(escapeJson(milestoneLabels.get(i))).append('"');
        }
        sb.append(']');
        return sb.toString();
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
