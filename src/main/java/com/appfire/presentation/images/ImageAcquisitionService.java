package com.appfire.presentation.images;

import com.appfire.presentation.model.GenerationResponse;
import com.appfire.presentation.model.ImagePlan;
import com.appfire.presentation.model.ResolvedImage;
import com.appfire.presentation.model.SlideAction;
import com.appfire.presentation.model.SlideDirective;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.poi.sl.usermodel.PictureData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ImageAcquisitionService {

    private static final Logger LOG = LoggerFactory.getLogger(ImageAcquisitionService.class);
    private static final String PEXELS_SEARCH_URL = "https://api.pexels.com/v1/search";
    private static final int SEARCH_PER_PAGE = 5;

    private final String pexelsApiKey;
    private final Path cacheDir;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ImageAcquisitionService(String pexelsApiKey, Path cacheDir, ObjectMapper objectMapper) {
        this.pexelsApiKey = pexelsApiKey;
        this.cacheDir = cacheDir;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    public ImagePlan acquire(GenerationResponse response) {
        Map<Integer, ResolvedImage> images = new HashMap<>();
        if (pexelsApiKey == null || pexelsApiKey.isBlank()) {
            LOG.warn("PEXELS_API_KEY not set. Skipping image acquisition.");
            return new ImagePlan(images);
        }

        long requested = response.slides().stream()
                .filter(s -> s.action() != SlideAction.SKIP && Boolean.TRUE.equals(s.includeImage()))
                .count();
        if (requested == 0) {
            LOG.warn("No slides requested images (includeImage=true). Check Gemini output or ImageDirectiveEnricher.");
            return new ImagePlan(images);
        }
        LOG.info("Acquiring images for {} slide(s) via Pexels", requested);

        for (SlideDirective slide : response.slides()) {
            if (slide.action() == SlideAction.SKIP || !Boolean.TRUE.equals(slide.includeImage())) {
                continue;
            }
            if (slide.imageQuery() == null || slide.imageQuery().isBlank()) {
                LOG.warn("Slide {} has includeImage but no imageQuery. Skipping image.", slide.slideIndex());
                continue;
            }
            try {
                ResolvedImage image = fetchImage(slide.slideIndex(), slide.imageQuery());
                if (image != null) {
                    images.put(slide.slideIndex(), image);
                }
            } catch (Exception e) {
                LOG.warn("Failed to acquire image for slide {}: {}. Continuing without image.",
                        slide.slideIndex(), e.getMessage());
            }
        }
        LOG.info("Acquired {} of {} requested image(s)", images.size(), requested);
        return new ImagePlan(images);
    }

    private ResolvedImage fetchImage(int slideIndex, String query) throws IOException, InterruptedException {
        Path cached = cachePath(slideIndex, query);
        if (Files.exists(cached)) {
            byte[] data = Files.readAllBytes(cached);
            return new ResolvedImage(slideIndex, data, detectType(data));
        }

        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = PEXELS_SEARCH_URL + "?query=" + encodedQuery
                + "&per_page=" + SEARCH_PER_PAGE + "&orientation=landscape";
        String searchBody = httpGet(url, Map.of("Authorization", pexelsApiKey));

        JsonNode root = objectMapper.readTree(searchBody);
        JsonNode photos = root.path("photos");
        if (!photos.isArray() || photos.isEmpty()) {
            LOG.warn("No Pexels results for query '{}'", query);
            return null;
        }

        int pickIndex = Math.floorMod(slideIndex + query.hashCode(), photos.size());
        JsonNode photo = photos.get(pickIndex);
        String imageUrl = photo.path("src").path("large").asText(null);
        if (imageUrl == null || imageUrl.isBlank()) {
            imageUrl = photo.path("src").path("medium").asText(null);
        }
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }

        byte[] imageBytes = httpGetBytes(imageUrl, Map.of());
        Files.createDirectories(cacheDir);
        Files.write(cached, imageBytes);
        return new ResolvedImage(slideIndex, imageBytes, detectType(imageBytes));
    }

    private String httpGet(String url, Map<String, String> headers) throws IOException, InterruptedException {
        try {
            return httpClientGet(url, headers);
        } catch (IOException e) {
            if (isSslError(e)) {
                LOG.warn("Java HTTP SSL failed for {}. Falling back to curl.", url);
                return curlGet(url, headers);
            }
            throw e;
        }
    }

    private byte[] httpGetBytes(String url, Map<String, String> headers) throws IOException, InterruptedException {
        try {
            return httpClientGetBytes(url, headers);
        } catch (IOException e) {
            if (isSslError(e)) {
                LOG.warn("Java HTTP SSL failed for image download. Falling back to curl.");
                return curlGetBytes(url, headers);
            }
            throw e;
        }
    }

    private String httpClientGet(String url, Map<String, String> headers)
            throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .GET();
        headers.forEach(builder::header);
        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("HTTP GET failed with status " + response.statusCode() + " for " + url);
        }
        return response.body();
    }

    private byte[] httpClientGetBytes(String url, Map<String, String> headers)
            throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(60))
                .GET();
        headers.forEach(builder::header);
        HttpResponse<byte[]> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) {
            throw new IOException("HTTP GET failed with status " + response.statusCode() + " for " + url);
        }
        return response.body();
    }

    private String curlGet(String url, Map<String, String> headers) throws IOException {
        return new String(curlGetBytes(url, headers), StandardCharsets.UTF_8);
    }

    private byte[] curlGetBytes(String url, Map<String, String> headers) throws IOException {
        List<String> command = new ArrayList<>();
        command.add("curl");
        command.add("-sS");
        command.add("-L");
        command.add("--fail");
        for (Map.Entry<String, String> header : headers.entrySet()) {
            command.add("-H");
            command.add(header.getKey() + ": " + header.getValue());
        }
        command.add(url);

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        byte[] body = readAllBytes(process.getInputStream());
        int exitCode = waitForProcess(process);
        if (exitCode != 0) {
            throw new IOException("curl failed with exit code " + exitCode + " for " + url);
        }
        return body;
    }

    private byte[] readAllBytes(InputStream input) throws IOException {
        try (input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            input.transferTo(output);
            return output.toByteArray();
        }
    }

    private int waitForProcess(Process process) throws IOException {
        try {
            boolean finished = process.waitFor(60, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IOException("curl timed out");
            }
            return process.exitValue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new IOException("Interrupted waiting for curl", e);
        }
    }

    private boolean isSslError(IOException e) {
        String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        Throwable cause = e.getCause();
        String causeMessage = cause != null && cause.getMessage() != null
                ? cause.getMessage().toLowerCase()
                : "";
        return message.contains("certificate")
                || message.contains("pkix")
                || message.contains("ssl")
                || causeMessage.contains("certificate")
                || causeMessage.contains("pkix")
                || causeMessage.contains("ssl");
    }

    private Path cachePath(int slideIndex, String query) {
        String safeQuery = query.replaceAll("[^a-zA-Z0-9_-]", "_");
        return cacheDir.resolve("slide-" + slideIndex + "-" + safeQuery + ".jpg");
    }

    private PictureData.PictureType detectType(byte[] data) {
        if (data.length >= 8
                && data[0] == (byte) 0x89
                && data[1] == 0x50
                && data[2] == 0x4E
                && data[3] == 0x47) {
            return PictureData.PictureType.PNG;
        }
        return PictureData.PictureType.JPEG;
    }
}
