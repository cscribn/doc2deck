package com.appfire.presentation.images;

import com.appfire.presentation.model.ImageKeyPlan;
import com.appfire.presentation.model.ImageKeyPlan.ResolvedImageKey;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
    private final ObjectMapper objectMapper;

    public ImageAcquisitionService(String pexelsApiKey, Path cacheDir, ObjectMapper objectMapper) {
        this.pexelsApiKey = pexelsApiKey;
        this.cacheDir = cacheDir;
        this.objectMapper = objectMapper;
    }

    public ImageKeyPlan acquire(Map<String, String> imageQueries) {
        Map<String, ResolvedImageKey> images = new HashMap<>();
        if (pexelsApiKey == null || pexelsApiKey.isBlank()) {
            LOG.warn("PEXELS_API_KEY not set. Skipping image acquisition.");
            return new ImageKeyPlan(images);
        }
        if (imageQueries == null || imageQueries.isEmpty()) {
            LOG.warn("No image queries provided. Check Gemini output for image keys.");
            return new ImageKeyPlan(images);
        }

        LOG.info("Acquiring images for {} key(s) via Pexels", imageQueries.size());
        int index = 0;
        for (Map.Entry<String, String> entry : imageQueries.entrySet()) {
            String key = entry.getKey();
            String query = entry.getValue();
            if (query == null || query.isBlank()) {
                LOG.warn("Image key '{}' has no query. Skipping.", key);
                continue;
            }
            try {
                ResolvedImageKey image = fetchImage(key, query, index++);
                if (image != null) {
                    images.put(key, image);
                }
            } catch (Exception e) {
                LOG.warn("Failed to acquire image for key '{}': {}. Continuing without image.",
                        key, e.getMessage());
            }
        }
        LOG.info("Acquired {} of {} requested image(s)", images.size(), imageQueries.size());
        return new ImageKeyPlan(images);
    }

    private ResolvedImageKey fetchImage(String key, String query, int pickOffset) throws IOException {
        Path cached = cachePath(key, query);
        if (Files.exists(cached)) {
            byte[] data = Files.readAllBytes(cached);
            return new ResolvedImageKey(key, data, detectType(data));
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

        int pickIndex = Math.floorMod(pickOffset + query.hashCode(), photos.size());
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
        return new ResolvedImageKey(key, imageBytes, detectType(imageBytes));
    }

    private String httpGet(String url, Map<String, String> headers) throws IOException {
        return new String(httpGetBytes(url, headers), StandardCharsets.UTF_8);
    }

    private byte[] httpGetBytes(String url, Map<String, String> headers) throws IOException {
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

    private Path cachePath(String key, String query) {
        String safeQuery = query.replaceAll("[^a-zA-Z0-9_-]", "_");
        return cacheDir.resolve(key + "-" + safeQuery + ".jpg");
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
