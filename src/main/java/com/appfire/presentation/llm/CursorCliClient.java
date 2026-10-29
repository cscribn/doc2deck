package com.appfire.presentation.llm;

import com.appfire.presentation.config.AppConfig;
import com.appfire.presentation.model.HtmlDeckContentResponse;
import com.appfire.presentation.model.PresentationContentResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CursorCliClient implements PresentationContentGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(CursorCliClient.class);
    private static final long CLI_TIMEOUT_MINUTES = 10L;
    private static final Path PROMPT_CACHE_DIR = Path.of(".cache", "llm");

    private final AppConfig config;
    private final LlmJsonResponseParser responseParser;

    public CursorCliClient(AppConfig config, LlmJsonResponseParser responseParser) {
        this.config = config;
        this.responseParser = responseParser;
    }

    @Override
    public PresentationContentResponse generate(String prompt) throws IOException {
        return invokeWithRetries(prompt, responseParser::parsePresentation);
    }

    @Override
    public HtmlDeckContentResponse generateHtmlDeck(String prompt) throws IOException {
        return invokeWithRetries(prompt, responseParser::parseHtmlDeck);
    }

    private <T> T invokeWithRetries(String prompt, ResponseParser<T> parser) throws IOException {
        int attempts = 0;
        long backoff = LlmRetrySupport.BASE_BACKOFF_MS;
        while (true) {
            attempts++;
            try {
                String text = invokeOnce(prompt);
                return parser.parse(text);
            } catch (IOException e) {
                if (attempts > config.llmMaxRetries() || !isRetryable(e)) {
                    throw new IOException(
                            "Cursor CLI failed after " + attempts + " attempts. "
                                    + "Verify agent is installed and authenticated (agent login or CURSOR_API_KEY), "
                                    + "then re-run ./gradlew bootRun",
                            e);
                }
                LOG.warn(
                        "Cursor CLI call failed (attempt {}). Retrying in {}ms. Resolution: check CLI auth and rate limits.",
                        attempts,
                        backoff);
                LlmRetrySupport.sleep(backoff);
                backoff = LlmRetrySupport.nextExponentialBackoff(backoff);
            }
        }
    }

    private String invokeOnce(String prompt) throws IOException {
        Path promptFile = writePromptFile(prompt);
        try {
            List<String> command = buildCommand(promptFile);
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.directory(config.cursorWorkspace().toFile());
            builder.redirectErrorStream(true);
            Process process = builder.start();
            String output = readStream(process.getInputStream());
            int exitCode = waitForProcess(process);
            if (exitCode != 0) {
                throw new IOException(
                        "Cursor CLI exited with code " + exitCode + ": " + LlmRetrySupport.truncate(output, 500));
            }
            if (output.isBlank()) {
                throw new IOException("Cursor CLI returned empty output");
            }
            return output.trim();
        } finally {
            Files.deleteIfExists(promptFile);
        }
    }

    static List<String> buildCommand(AppConfig config, Path promptFile) {
        List<String> command = new ArrayList<>();
        command.add(config.cursorCliPath());
        command.add("-p");
        command.add(buildShortInstruction(promptFile));
        command.add("--trust");
        command.add("--mode=ask");
        command.add("--workspace");
        command.add(config.cursorWorkspace().toAbsolutePath().toString());
        if (config.cursorModel() != null && !config.cursorModel().isBlank()) {
            command.add("--model");
            command.add(config.cursorModel());
        }
        return List.copyOf(command);
    }

    private List<String> buildCommand(Path promptFile) {
        return buildCommand(config, promptFile);
    }

    static String buildShortInstruction(Path promptFile) {
        return "Read the file at "
                + promptFile.toAbsolutePath()
                + ". Follow it exactly. Return only the JSON specified there. Do not edit any files.";
    }

    private Path writePromptFile(String prompt) throws IOException {
        Files.createDirectories(PROMPT_CACHE_DIR);
        Path promptFile = PROMPT_CACHE_DIR.resolve("prompt-" + UUID.randomUUID() + ".md");
        Files.writeString(promptFile, prompt, StandardCharsets.UTF_8);
        return promptFile;
    }

    private static String readStream(java.io.InputStream inputStream) throws IOException {
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }

    private static int waitForProcess(Process process) throws IOException {
        try {
            boolean finished = process.waitFor(CLI_TIMEOUT_MINUTES, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                throw new IOException("Cursor CLI timed out after " + CLI_TIMEOUT_MINUTES + " minutes");
            }
            return process.exitValue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new IOException("Interrupted while waiting for Cursor CLI", e);
        }
    }

    private boolean isRetryable(IOException e) {
        if (e.getCause() instanceof JsonProcessingException) {
            return true;
        }
        String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        return message.contains("rate")
                || message.contains("429")
                || message.contains("unavailable")
                || message.contains("timeout")
                || message.contains("timed out")
                || message.contains("empty output")
                || message.contains("malformed");
    }

    @FunctionalInterface
    private interface ResponseParser<T> {
        T parse(String text) throws IOException;
    }
}
