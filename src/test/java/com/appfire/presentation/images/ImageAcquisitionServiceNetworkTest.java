package com.appfire.presentation.images;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.appfire.presentation.model.PresentationKeys;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

class ImageAcquisitionServiceNetworkTest {

    @Test
    @EnabledIfEnvironmentVariable(named = "PEXELS_API_KEY", matches = ".+")
    void fetchesImageFromPexels(@TempDir Path tempDir) throws Exception {
        ImageAcquisitionService service = new ImageAcquisitionService(
                System.getenv("PEXELS_API_KEY"),
                tempDir,
                new ObjectMapper());

        var plan = service.acquire(java.util.Map.of(
                PresentationKeys.PROBLEM_SOLVING_IMG, "architecture skyline"));

        assertFalse(plan.images().isEmpty(), "Expected at least one image from Pexels");
        assertEquals(1, plan.images().size());
    }
}
