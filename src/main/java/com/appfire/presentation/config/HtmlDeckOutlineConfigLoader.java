package com.appfire.presentation.config;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public final class HtmlDeckOutlineConfigLoader {

    private HtmlDeckOutlineConfigLoader() {
    }

    public static HtmlDeckOutlineConfig load(Path path) {
        if (!Files.exists(path) || !Files.isReadable(path)) {
            throw new IllegalStateException(
                    "HTML deck outline not found or unreadable: " + path.toAbsolutePath()
                            + ". Copy html-deck-outline.example.properties to html-deck-outline.properties and re-run ./gradlew bootRun");
        }
        Properties properties = new Properties();
        try (Reader reader = new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to read HTML deck outline: " + path.toAbsolutePath()
                            + ". Check file permissions and re-run ./gradlew bootRun",
                    e);
        }
        return parse(properties);
    }

    public static HtmlDeckOutlineConfig parse(Properties properties) {
        String header = properties.getProperty(HtmlDeckOutlineConfig.HEADER_KEY, "").trim();
        if (header.isBlank()) {
            throw new IllegalStateException(
                    "HTML deck outline missing required property: "
                            + HtmlDeckOutlineConfig.HEADER_KEY
                            + ". Set it in html-deck-outline.properties and re-run ./gradlew bootRun");
        }

        List<String> milestones = parseMilestones(properties.getProperty("milestones", ""));
        int slidesMin = parsePositiveInt("slides.min", properties.getProperty("slides.min", "8"));
        int slidesMax = parsePositiveInt("slides.max", properties.getProperty("slides.max", "14"));
        if (slidesMin > slidesMax) {
            throw new IllegalStateException(
                    "HTML deck outline slides.min (" + slidesMin + ") exceeds slides.max (" + slidesMax
                            + "). Fix html-deck-outline.properties and re-run ./gradlew bootRun");
        }

        Map<String, Integer> maxWords = new LinkedHashMap<>();
        Map<String, String> layouts = new LinkedHashMap<>();
        for (String name : properties.stringPropertyNames()) {
            if (HtmlDeckOutlineConfig.HEADER_KEY.equals(name)
                    || "milestones".equals(name)
                    || name.startsWith("slides.")) {
                continue;
            }
            if (name.startsWith("layout.")) {
                String layout = name.substring("layout.".length());
                if (!HtmlDeckOutlineConfig.ALLOWED_LAYOUTS.contains(layout)) {
                    throw new IllegalStateException(
                            "Unknown HTML deck layout in outline: " + layout
                                    + ". Use one of: " + HtmlDeckOutlineConfig.ALLOWED_LAYOUTS);
                }
                layouts.put(layout, properties.getProperty(name, "").trim());
                continue;
            }
            if (name.endsWith(".maxWords")) {
                maxWords.put(name.substring(0, name.length() - ".maxWords".length()),
                        parsePositiveInt(name, properties.getProperty(name)));
            }
        }

        for (String layout : HtmlDeckOutlineConfig.ALLOWED_LAYOUTS) {
            if (!layouts.containsKey(layout) || layouts.get(layout).isBlank()) {
                throw new IllegalStateException(
                        "HTML deck outline missing layout." + layout
                                + " description. Add it to html-deck-outline.properties");
            }
        }

        return new HtmlDeckOutlineConfig(header, milestones, slidesMin, slidesMax, maxWords, layouts);
    }

    private static List<String> parseMilestones(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalStateException(
                    "HTML deck outline missing milestones property. "
                            + "Use milestones=0:Intro,1:Problem,... in html-deck-outline.properties");
        }
        List<String> labels = new ArrayList<>();
        Map<Integer, String> byIndex = new LinkedHashMap<>();
        for (String part : raw.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int colon = trimmed.indexOf(':');
            if (colon <= 0) {
                throw new IllegalStateException(
                        "Invalid milestones entry: " + trimmed
                                + ". Use index:Label pairs separated by commas");
            }
            int index = Integer.parseInt(trimmed.substring(0, colon).trim());
            String label = trimmed.substring(colon + 1).trim();
            if (label.isBlank()) {
                throw new IllegalStateException("Milestone label blank for index " + index);
            }
            byIndex.put(index, label);
        }
        if (byIndex.isEmpty()) {
            throw new IllegalStateException("HTML deck outline milestones list is empty");
        }
        int max = byIndex.keySet().stream().max(Integer::compareTo).orElse(0);
        for (int i = 0; i <= max; i++) {
            labels.add(byIndex.getOrDefault(i, "Section " + i));
        }
        return labels;
    }

    private static int parsePositiveInt(String name, String value) {
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed <= 0) {
                throw new NumberFormatException("non-positive");
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalStateException(
                    "Invalid " + name + " value: " + value
                            + ". Set a positive integer in html-deck-outline.properties");
        }
    }
}
