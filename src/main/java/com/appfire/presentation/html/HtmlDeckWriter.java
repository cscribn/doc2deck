package com.appfire.presentation.html;

import com.appfire.presentation.config.HtmlDeckOutlineConfig;
import com.appfire.presentation.model.HtmlDeckContentResponse;
import com.appfire.presentation.model.HtmlDeckSlide;
import com.appfire.presentation.model.ImageKeyPlan;
import com.appfire.presentation.model.ImageKeyPlan.ResolvedImageKey;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.apache.poi.sl.usermodel.PictureData;
import org.springframework.stereotype.Component;

@Component
public final class HtmlDeckWriter {

    private static final String SHELL_PATH = "/html-deck/shell.html";
    private static final String BASE_CSS_PATH = "/html-deck/base.css";
    private static final String LAYOUTS_CSS_PATH = "/html-deck/layouts.css";
    private static final String ICONS_PATH = "/html-deck/icons.svg";
    private static final String RUNTIME_PATH = "/html-deck/runtime.js";

    private final HtmlSlideRenderer slideRenderer;

    public HtmlDeckWriter(HtmlSlideRenderer slideRenderer) {
        this.slideRenderer = slideRenderer;
    }

    public void write(
            Path outputPath,
            HtmlDeckContentResponse response,
            HtmlDeckOutlineConfig outlineConfig,
            ImageKeyPlan imagePlan) throws IOException {
        Map<String, String> imageUris = buildImageDataUris(imagePlan);
        StringBuilder track = new StringBuilder();
        for (int i = 0; i < response.slides().size(); i++) {
            HtmlDeckSlide slide = response.slides().get(i);
            String uri = null;
            if ("split".equalsIgnoreCase(slide.layout())) {
                uri = imageUris.get(imageKeyForSlide(i));
            }
            track.append(slideRenderer.renderSlide(slide, i, uri));
        }

        String html = loadResource(SHELL_PATH)
                .replace("{{DECK_TITLE}}", HtmlEscaper.text(response.deckTitle()))
                .replace("{{BASE_CSS}}", loadResource(BASE_CSS_PATH))
                .replace("{{LAYOUTS_CSS}}", loadResource(LAYOUTS_CSS_PATH))
                .replace("{{MILESTONES_JSON}}", outlineConfig.milestonesJsonForRuntime())
                .replace("{{ICONS_SVG}}", loadResource(ICONS_PATH).trim())
                .replace("{{TRACK}}", track.toString())
                .replace("{{RUNTIME_JS}}", loadResource(RUNTIME_PATH));

        Path parent = outputPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(outputPath, html, StandardCharsets.UTF_8);
    }

    public static String imageKeyForSlide(int index) {
        return "html-slide-" + index;
    }

    public static Map<String, String> queriesBySlideIndex(HtmlDeckContentResponse response) {
        Map<String, String> queries = new HashMap<>();
        for (int i = 0; i < response.slides().size(); i++) {
            HtmlDeckSlide slide = response.slides().get(i);
            if ("split".equalsIgnoreCase(slide.layout())
                    && slide.imageQuery() != null
                    && !slide.imageQuery().isBlank()) {
                queries.put(imageKeyForSlide(i), slide.imageQuery());
            }
        }
        return queries;
    }

    private Map<String, String> buildImageDataUris(ImageKeyPlan plan) {
        Map<String, String> uris = new HashMap<>();
        if (plan == null) {
            return uris;
        }
        for (Map.Entry<String, ResolvedImageKey> entry : plan.images().entrySet()) {
            ResolvedImageKey image = entry.getValue();
            String mime = image.pictureType() == PictureData.PictureType.PNG ? "image/png" : "image/jpeg";
            String encoded = Base64.getEncoder().encodeToString(image.data());
            uris.put(entry.getKey(), "data:" + mime + ";base64," + encoded);
        }
        return uris;
    }

    private static String loadResource(String path) throws IOException {
        try (InputStream stream = HtmlDeckWriter.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IOException("Missing classpath resource: " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
