package com.appfire.presentation.images;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

class ImageAcquisitionServiceNetworkTest {

    @Test
    @EnabledIfEnvironmentVariable(named = "PEXELS_API_KEY", matches = ".+")
    void fetchesImageFromPexels() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.pexels.com/v1/search?query=architecture&per_page=1"))
                .header("Authorization", System.getenv("PEXELS_API_KEY"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        org.junit.jupiter.api.Assertions.assertEquals(200, response.statusCode());
    }
}
