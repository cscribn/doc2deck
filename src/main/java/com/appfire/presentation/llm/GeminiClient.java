package com.appfire.presentation.llm;

import com.appfire.presentation.config.AppConfig;
import com.appfire.presentation.model.PresentationContentResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GeminiClient {

    private static final Logger LOG = LoggerFactory.getLogger(GeminiClient.class);
    private static final long BASE_BACKOFF_MS = 1_000L;
    private static final long MAX_BACKOFF_MS = 30_000L;
    private static final long CLI_TIMEOUT_MINUTES = 10L;

    private final AppConfig config;
    private final ObjectMapper objectMapper;

    public GeminiClient(AppConfig config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
    }

    public PresentationContentResponse generate(String prompt) throws IOException {
        int attempts = 0;
        long backoff = BASE_BACKOFF_MS;
        while (true) {
            try {
                String responseText = invokeGeminiCli(prompt);
                return parseResponse(responseText);
            } catch (IOException e) {
                attempts++;
                if (attempts > config.geminiMaxRetries() || !isRetryable(e)) {
                    throw new IOException(
                            "Gemini CLI failed after " + attempts + " attempts. "
                                    + "Verify the gemini CLI is installed and authenticated, then re-run ./gradlew run",
                            e);
                }
                LOG.warn("Gemini CLI call failed (attempt {}). Retrying in {}ms. Resolution: check CLI auth and rate limits.",
                        attempts, backoff);
                sleep(backoff);
                backoff = Math.min(backoff * 2, MAX_BACKOFF_MS);
            }
        }
    }

    private String invokeGeminiCli(String prompt) throws IOException {
        List<String> command = List.of(
                config.geminiCliPath(),
                "-p", "Process the prompt provided on stdin.",
                "-m", config.geminiModel(),
                "--output-format", "json",
                "--approval-mode", "plan",
                "--skip-trust");

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectError(ProcessBuilder.Redirect.DISCARD);
        Process process = builder.start();

        writePrompt(process, prompt);
        String output = readProcessOutput(process);
        int exitCode = waitForProcess(process);

        if (exitCode != 0) {
            throw new IOException("Gemini CLI exited with code " + exitCode + ": " + truncate(output));
        }

        return extractResponseText(output);
    }

    private String readStream(InputStream inputStream) throws IOException {
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }

    private void writePrompt(Process process, String prompt) throws IOException {
        try (OutputStream outputStream = process.getOutputStream()) {
            outputStream.write(prompt.getBytes(StandardCharsets.UTF_8));
        }
    }

    private String readProcessOutput(Process process) throws IOException {
        return readStream(process.getInputStream());
    }

    private int waitForProcess(Process process) throws IOException {
        try {
            boolean finished = process.waitFor(CLI_TIMEOUT_MINUTES, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                throw new IOException("Gemini CLI timed out after " + CLI_TIMEOUT_MINUTES + " minutes");
            }
            return process.exitValue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new IOException("Interrupted while waiting for Gemini CLI", e);
        }
    }

    private String extractResponseText(String cliOutput) throws IOException {
        JsonNode root = objectMapper.readTree(cliOutput.trim());
        JsonNode error = root.get("error");
        if (error != null && !error.isNull()) {
            throw new IOException("Gemini CLI returned an error: " + error);
        }
        JsonNode response = root.get("response");
        if (response == null || response.isNull()) {
            throw new IOException("Gemini CLI JSON output missing 'response' field: " + truncate(cliOutput));
        }
        return response.asText();
    }

    private PresentationContentResponse parseResponse(String text) throws IOException {
        String json = stripMarkdownFences(text);
        return objectMapper.readValue(json, PresentationContentResponse.class);
    }

    private String stripMarkdownFences(String text) {
        if (text == null) {
            return "{}";
        }
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                return trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return trimmed;
    }

    private boolean isRetryable(IOException e) {
        String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        return message.contains("rate") || message.contains("429")
                || message.contains("unavailable") || message.contains("timeout")
                || message.contains("timed out");
    }

    private String truncate(String text) {
        if (text == null) {
            return "";
        }
        return text.length() <= 500 ? text : text.substring(0, 500) + "...";
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted during Gemini retry backoff", e);
        }
    }
}
